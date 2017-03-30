package com.anxpp.tinyim.server.sdk;

import java.nio.ByteBuffer;

import com.anxpp.tinyim.server.sdk.processor.UserProcessor;
import com.anxpp.tinyim.server.sdk.protocal.CharsetHelper;
import com.anxpp.tinyim.server.sdk.protocal.ProtocalType;
import com.anxpp.tinyim.server.sdk.event.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.event.ClientMessageListener;
import com.anxpp.tinyim.server.sdk.protocal.Protocal;
import com.anxpp.tinyim.server.sdk.protocal.ProtocalFactory;
import com.anxpp.tinyim.server.sdk.protocal.c.LoginInfo;
import com.anxpp.tinyim.server.sdk.qos.QoS4ReceiveDaemonC2S;
import com.anxpp.tinyim.server.sdk.qos.QoS4SendDaemonS2C;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerCoreHandler extends IoHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ServerCoreHandler.class);

    // 服务端事件回调实现
    private ClientMessageListener clientMessageListener = null;

    // QoS机制下的S2C模式中，由服务端主动发起消息的QoS事件回调实现
    private ServerMessageListener serverMessageListener = null;

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.error("exceptionCaught:" + cause.getMessage(), cause);
//        session.close(true);
        session.closeNow();
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
        if ((message instanceof IoBuffer)) {
            IoBuffer buffer = (IoBuffer) message;
            Protocal pFromClient = fromIOBuffer(buffer);

            String remoteAddress = clientInfoToString(session);
            switch (pFromClient.getType()) {
                case ProtocalType.C.FROM_CLIENT_TYPE_OF_RECEIVED:
                case ProtocalType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA: {
                    logger.info(">> client normal message:" + remoteAddress);
                    // 开始回调
                    if (this.clientMessageListener != null) {
                        //用户未登陆
                        if (!UserProcessor.isLogined(session)) {
                            replyDataForUnlogined(session, pFromClient);
                            return;
                        }
                        // 客户端发给服务端的消息
                        if (pFromClient.getTo() == 0) {
                            if (pFromClient.getType() == ProtocalType.C.FROM_CLIENT_TYPE_OF_RECEIVED) {
                                String theFingerPrint = pFromClient.getDataContent();
                                logger.debug("client msg to server:" + pFromClient.getFrom() + "finger:" + theFingerPrint + "received");
                                if (this.serverMessageListener != null) {
                                    this.serverMessageListener.messagesBeReceived(theFingerPrint);
                                }
                                QoS4SendDaemonS2C.getInstance().remove(theFingerPrint);
                                break;
                            }

                            if (pFromClient.isQoS()) {
                                if (QoS4ReceiveDaemonC2S.getInstance().hasReceived(pFromClient.getFp())) {
                                    if (QoS4ReceiveDaemonC2S.DEBUG) {
                                        logger.debug("【IMCORE】【QoS机制】" + pFromClient.getFp() + "已经存在于发送列表中，这是重复包，通知业务处理层收到该包罗！");
                                    }
                                    QoS4ReceiveDaemonC2S.getInstance().addReceived(pFromClient);
                                    boolean receivedBackSendSucess = replyDelegateRecievedBack(session, pFromClient);
                                    if (receivedBackSendSucess) {
                                        logger.debug("【QoS_应答_C2S】向" + pFromClient.getFrom() + "发送" + pFromClient.getFp() + "的应答包成功了,from=" + pFromClient.getTo() + ".");
                                    }
                                    return;
                                }
                                QoS4ReceiveDaemonC2S.getInstance().addReceived(pFromClient);
                                boolean receivedBackSendSucess = replyDelegateRecievedBack(session, pFromClient);
                                if (receivedBackSendSucess) {
                                    logger.debug("【QoS_应答_C2S】向" + pFromClient.getFrom() + "发送" + pFromClient.getFp() + "的应答包成功了,from=" + pFromClient.getTo() + ".");
                                }
                            }
                            boolean receivedBackSendSucess = this.clientMessageListener.onClientToServer(pFromClient.getTo(), pFromClient.getFrom(), pFromClient.getDataContent(), pFromClient.getFp());
                            break;
                        }

                        // TODO DEBUG
                        UserProcessor.getInstance().__printOnline();

                        boolean sendOK = sendData(pFromClient);
                        if (sendOK) {
                            this.clientMessageListener.onClientToClient(
                                    pFromClient.getTo(), pFromClient.getFrom(), pFromClient.getDataContent());
                            break;
                        }

                        logger.info("[IM CORE]>> 客户端" + remoteAddress + "的通用数据尝试实时发送没有成功，将交给应用层进行离线存储哦...");

                        boolean offlineProcessedOK = this.clientMessageListener
                                .onClientToClientFailed(pFromClient.getTo(),
                                        pFromClient.getFrom(), pFromClient.getDataContent(), pFromClient.getFp());

                        if ((pFromClient.isQoS()) && (offlineProcessedOK)) {
                            boolean receivedBackSendSucess = replyDelegateRecievedBack(session, pFromClient);
                            if (!receivedBackSendSucess) break;
                            logger.debug("【QoS_伪应答_C2S】向" + pFromClient.getFrom() + "发送" + pFromClient.getFp() +
                                    "的伪应答包成功,from=" + pFromClient.getTo() + ".");
                            break;
                        }

                        logger.warn("[IM CORE]>> 客户端" + remoteAddress + "的通用数据传输消息尝试实时发送没有成功，但上层应用层没有成功(或者完全没有)进行离线存储，此消息将被服务端丢弃！");

                        break;
                    }

                    logger.warn("[IM CORE]>> 收到客户端" + remoteAddress + "的通用数据传输消息，但回调对象是null，回调无法继续.");
                    break;
                }
                //心跳
                case ProtocalType.C.FROM_CLIENT_TYPE_OF_KEEP$ALIVE: {
                    if (!UserProcessor.isLogined(session)) {
                        replyDataForUnlogined(session, pFromClient);
                        return;
                    }

                    sendData(ProtocalFactory.createPKeepAliveResponse(UserProcessor.getUserIdFromSession(session)));
                    break;
                }
                //登陆消息
                case ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGIN: {
                    LoginInfo loginInfo = ProtocalFactory.parsePLoginInfo(pFromClient.getDataContent());
                    logger.info("client login msg:" + remoteAddress + "?username=" + loginInfo.getUsername() + "&password=" + loginInfo.getPassword());

                    if (this.clientMessageListener != null) {
                        int t_userId = UserProcessor.getUserIdFromSession(session);
                        boolean hasLogin = t_userId != -1;
                        //已经登陆过了
                        if (hasLogin) {
                            logger.debug("client has already login:" + remoteAddress + "?username=" + loginInfo.getUsername());
                            boolean sendOK = sendData(session, ProtocalFactory.createPLoginInfoResponse(0, t_userId));
                            if (sendOK) {
                                // 将用户登陆成功后的id暂存到会话对象中备用
                                session.setAttribute(UserProcessor.USER_ID_IN_SESSION_ATTRIBUTE, t_userId);
                                // 将用户登陆成功后的登陆名暂存到会话对象中备用
                                session.setAttribute(UserProcessor.LOGIN_NAME_IN_SESSION_ATTRIBUTE, loginInfo.getUsername());
                                // 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
                                UserProcessor.getInstance().putUser(t_userId, session, loginInfo.getUsername());
                                this.clientMessageListener.onLoginSuccess(t_userId, loginInfo.getUsername(), session);
                                break;
                            }
                            logger.warn("login response failed for:" + remoteAddress);
                            break;
                        }
                        //未登陆，通知安排登陆信息校验
                        int code = this.clientMessageListener.onLogin(loginInfo.getUsername(), loginInfo.getPassword(), loginInfo.getExtra());
                        int user_id = -1;
                        //校验通过
                        if (code == 0) {
                             user_id = getNextUserId(loginInfo);
                            boolean sendOK = sendData(session, ProtocalFactory.createPLoginInfoResponse(code, user_id));
                            if (sendOK) {
                                // 将用户登陆成功后的id暂存到会话对象中备用
                                session.setAttribute(UserProcessor.USER_ID_IN_SESSION_ATTRIBUTE, user_id);
                                // 将用户登陆成功后的登陆名暂存到会话对象中备用
                                session.setAttribute(UserProcessor.LOGIN_NAME_IN_SESSION_ATTRIBUTE, loginInfo.getUsername());
                                // 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
                                UserProcessor.getInstance().putUser(user_id, session, loginInfo.getUsername());
                                this.clientMessageListener.onLoginSuccess(user_id, loginInfo.getUsername(), session);
                                break;
                            }
                            logger.warn("login response failed for:" + remoteAddress);
                            break;
                        }
                        sendData(session, ProtocalFactory.createPLoginInfoResponse(code, user_id));
                        break;
                    }

                    logger.warn("[IMCORE]>> 收到客户端" + remoteAddress + "登陆信息，但回调对象是null，没有进行回调.");
                    break;
                }
                //登出消息
                case ProtocalType.C.FROM_CLIENT_TYPE_OF_LOGOUT: {
                    logger.info("[IMCORE]>> 收到客户端" + remoteAddress + "的退出登陆请求.");

                    session.close(true);
                    break;
                }
                // FIXME: 以下代码建议仅用于Debug时，否则存在恶意DDoS攻击的可能！
                // 【收到客户端发过来的ECHO指令（目前回显指令仅用于C2S时开发人员的网络测试，别无他用】
//				case ProtocalType.C.FROM_CLIENT_TYPE_OF_ECHO:
//				{
//					pFromClient.setType(53);
//					sendData(session, pFromClient);
//					break;
//				}
                default:
                    logger.warn("[IMCORE]【注意】收到的客户端" + remoteAddress + "消息类型：" + pFromClient.getType() + "，但目前该类型服务端不支持解析和处理！");
                    break;
            }
        } else {
            logger.error("unknown message type:" + message.getClass() + ", IoBuffer?" + ", ByteBuffer?" + (message instanceof ByteBuffer));
        }
    }

    protected int getNextUserId(LoginInfo loginInfo) {
        return UserProcessor.nextUserId(loginInfo);
    }

    protected boolean replyDataForUnlogined(IoSession session, Protocal p) throws Exception {
        logger.warn("client have not login:" + clientInfoToString(session) + "-msg:" + p.getDataContent());

        return sendData(session, ProtocalFactory.createPErrorResponse(
                301, p.toGsonString(), -1));
    }

    protected boolean replyDelegateRecievedBack(IoSession session, Protocal pFromClient) throws Exception {
        if ((pFromClient.isQoS()) && (pFromClient.getFp() != null)) {
            Protocal receivedBackP = ProtocalFactory.createRecivedBack(
                    pFromClient.getTo(),
                    pFromClient.getFrom(),
                    pFromClient.getFp());

            return sendData(session, receivedBackP);
        }

        logger.warn("[IMCORE]收到" + pFromClient.getFrom() + "发过来需要QoS的包，但它的指纹码却为null！无法发伪应答包哦！");
        return false;
    }

    public void sessionClosed(IoSession session) throws Exception {
        int user_id = UserProcessor.getUserIdFromSession(session);
        String loginName = UserProcessor.getLoginNameFromSession(session);
        logger.info("[IMCORE]与" + clientInfoToString(session) + "的会话关闭(user_id=" + user_id + ",loginName=" + loginName + ")了...");
        if (user_id != -1) {
            UserProcessor.getInstance().removeUser(user_id);

            if (this.clientMessageListener != null) {
                this.clientMessageListener.onLogout(user_id, null);
            } else logger.debug("[IMCORE]>> 客户端" + clientInfoToString(session) + "的会话被系统close了，但回调对象是null，没有进行回调.");
        } else {
            logger.warn("[IMCORE]【注意】客户端" + clientInfoToString(session) + "的会话被系统close了，但它里面没有存放user_id，这个会话是何时建立的？");
        }
    }

    public void sessionCreated(IoSession session) throws Exception {
        logger.info("[IMCORE]与" + clientInfoToString(session) + "的会话建立(sessionCreated)了...");
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        logger.info("[IMCORE]Session idle...");
    }

    public void sessionOpened(IoSession session) throws Exception {
        logger.info("[IMCORE]与" + clientInfoToString(session) + "的会话(sessionOpened)打开了...");
    }

    ClientMessageListener getClientMessageListener() {
        return this.clientMessageListener;
    }

    void setClientMessageListener(ClientMessageListener clientMessageListener) {
        this.clientMessageListener = clientMessageListener;
    }

    ServerMessageListener getServerMessageListener() {
        return this.serverMessageListener;
    }

    void setServerMessageListener(ServerMessageListener serverMessageListener) {
        this.serverMessageListener = serverMessageListener;
    }

    static boolean sendData(int from_user_id, int to_user_id, String dataContent) throws Exception {
        return sendData(from_user_id, to_user_id, dataContent, false);
    }

    static boolean sendData(int from_user_id, int to_user_id, String dataContent, boolean QoS) throws Exception {
        return sendData(from_user_id, to_user_id, dataContent, QoS, null);
    }

    static boolean sendData(int from_user_id, int to_user_id, String dataContent, boolean QoS, String fingerPrint) throws Exception {
        return sendData(ProtocalFactory.createCommonData(dataContent, from_user_id, to_user_id, QoS, fingerPrint));
    }

    static boolean sendData(Protocal p) throws Exception {
        if (p != null) {
            if (p.getTo() != 0) {
                return sendData(UserProcessor.getInstance().getSession(p.getTo()), p);
            }

            logger.warn("[IMCORE]【注意】此Protocal对象中的接收方是服务器(user_id==0)，数据发送没有继续！" + p.toGsonString());
            return false;
        }

        return false;
    }

    //发送消息
    static boolean sendData(IoSession session, Protocal p) throws Exception {
        if (session == null) {
            logger.info("message from " + p.getFrom() + " the received user " + p.getTo() + " is not online:" + p.getDataContent());
        } else if (session.isConnected()) {
            if (p != null) {
                byte[] res = p.toBytes();
                IoBuffer buf = IoBuffer.wrap(res);
                WriteFuture future = session.write(buf);
                future.awaitUninterruptibly(100L);
                // The message has been written successfully
                if (future.isWritten()) {
                    if (p.getFrom() == 0) {
                        if ((p.isQoS()) && (!QoS4SendDaemonS2C.getInstance().exist(p.getFp()))) {
                            QoS4SendDaemonS2C.getInstance().put(p);
                        }
                    }
                    return true;
                }

                logger.warn("[IMCORE]给客户端：" + clientInfoToString(session) + "的数据->" + p.toGsonString() + ",发送失败！[" + res.length + "](此消息可考虑作离线处理哦).");
            }
        } else {
            logger.warn("[IMCORE]toSession!=null但会话已经关闭 >> 客户端id=" + p.getFrom() + "要发给客户端" + p.getTo() +
                    "的实时消息：str=" + p.getDataContent() + "没有继续(此消息可考虑作离线处理哦).");
        }

        return false;
    }

    public static String clientInfoToString(IoSession session) {
        return "{userName:" + UserProcessor.getLoginNameFromSession(session) + ",userId:" + UserProcessor.getUserIdFromSession(session) + "}" + session.getRemoteAddress().toString();
    }

    private static String bufferToJson(IoBuffer buffer) throws Exception {
        return buffer.getString(CharsetHelper.decoder);
    }

    private static Protocal fromIOBuffer(IoBuffer buffer) throws Exception {
        return ProtocalFactory.parse(bufferToJson(buffer), Protocal.class);
    }
}