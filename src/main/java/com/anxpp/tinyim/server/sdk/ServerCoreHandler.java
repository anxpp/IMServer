package com.anxpp.tinyim.server.sdk;

import com.anxpp.tinyim.server.sdk.event.ClientMessageListener;
import com.anxpp.tinyim.server.sdk.event.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.processor.UserProcessor;
import com.anxpp.tinyim.server.sdk.message.CharsetHelper;
import com.anxpp.tinyim.server.sdk.message.Message;
import com.anxpp.tinyim.server.sdk.message.MessageFactory;
import com.anxpp.tinyim.server.sdk.message.MessageType;
import com.anxpp.tinyim.server.sdk.message.client.LoginInfo;
import com.anxpp.tinyim.server.sdk.qos.QoS4ReceiveDaemonC2S;
import com.anxpp.tinyim.server.sdk.qos.QoS4SendDaemonS2C;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.nio.ByteBuffer;

/**
 * 服务端核心Server
 */
public class ServerCoreHandler extends IoHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(ServerCoreHandler.class);

    // 服务端事件回调实现
    private ClientMessageListener clientMessageListener = null;

    // QoS机制下的S2C模式中，由服务端主动发起消息的QoS事件回调实现
    private ServerMessageListener serverMessageListener = null;

    static boolean sendMessage(int fromUserId, int toUserId, String dataContent) throws Exception {
        return sendMessage(fromUserId, toUserId, dataContent, false);
    }

    static boolean sendMessage(int fromUserId, int toUserId, String dataContent, boolean QoS) throws Exception {
        return sendMessage(fromUserId, toUserId, dataContent, QoS, null);
    }

    static boolean sendMessage(int fromUserId, int toUserId, String dataContent, boolean QoS, String fingerPrint) throws Exception {
        return sendMessage(MessageFactory.createCommonData(dataContent, fromUserId, toUserId, QoS, fingerPrint));
    }

    /**
     * 发送消息
     *
     * @param message 消息体
     * @return 发送状态
     * @throws Exception 异常
     */
    static boolean sendMessage(Message message) throws Exception {
        if (!ObjectUtils.isEmpty(message)) {
            if (message.getTo() != 0) {
                return sendMessage(UserProcessor.getInstance().getSession(message.getTo()), message);
            }
            logger.warn("send to server will not response:" + message.toGsonString());
            return false;
        }
        return false;
    }

    //发送消息
    static boolean sendMessage(IoSession session, Message message) throws Exception {
        //session不存在
        if (session == null) {
            logger.info("message from " + message.getFrom() + " the received user " + message.getTo() + " is not online:" + message.getDataContent());
        }
        //发送消息
        else if (session.isConnected()) {
            if (!ObjectUtils.isEmpty(message)) {
                byte[] res = message.toBytes();
                IoBuffer buf = IoBuffer.wrap(res);
                WriteFuture future = session.write(buf);
                future.awaitUninterruptibly(100L);
                if (future.isWritten()) {
                    if (message.getFrom() == 0) {
                        if ((message.isQoS()) && (!QoS4SendDaemonS2C.getInstance().exist(message.getFp()))) {
                            QoS4SendDaemonS2C.getInstance().put(message);
                        }
                    }
                    return true;
                }
                logger.warn("message to " + clientInfoToString(session) + ":" + message.toGsonString() + ",send failed:" + res.length);
            }
        } else {
            logger.warn("user is offline:" + message.getTo());
        }
        return false;
    }

    public static String clientInfoToString(IoSession session) {
        return "{userName:" + UserProcessor.getLoginNameFromSession(session) + ",userId:" + UserProcessor.getUserIdFromSession(session) + "}" + session.getRemoteAddress().toString();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.error("exceptionCaught:" + cause.getMessage(), cause);
        session.closeNow();
    }

    /**
     * 收到消息
     *
     * @param session session
     * @param buffer  消息体
     * @throws Exception 异常
     */
    public void messageReceived(IoSession session, Object buffer) throws Exception {
        if (buffer instanceof IoBuffer) {
            //消息体
            Message message = fromIOBuffer((IoBuffer) buffer);
            //客户端地址
            String remoteAddress = clientInfoToString(session);
            //判断消息类型
            switch (message.getType()) {
                //普通消息
                case MessageType.Client.FROM_CLIENT_TYPE_OF_RECEIVED:
                    onSimpleMessage(remoteAddress, message, session);
                    break;
                //心跳
                case MessageType.Client.FROM_CLIENT_TYPE_OF_KEEP$ALIVE:
                    if (!UserProcessor.isLogined(session)) {
                        replyDataForNotLogin(session, message);
                        break;
                    }
                    sendMessage(MessageFactory.createPKeepAliveResponse(UserProcessor.getUserIdFromSession(session)));
                    break;
                //登陆消息
                case MessageType.Client.FROM_CLIENT_TYPE_OF_LOGIN: {
                    onLoginMessage(message, remoteAddress, session);
                    break;
                }
                //登出消息
                case MessageType.Client.FROM_CLIENT_TYPE_OF_LOGOUT: {
                    logger.info("client logout:" + remoteAddress);
                    session.closeNow();
                    break;
                }
                default:
                    logger.warn("not supported buffer type " + message.getType() + " from " + remoteAddress);
                    break;
            }
        } else {
            logger.error("unknown buffer type:" + buffer.getClass() + ", IoBuffer?" + ", ByteBuffer?" + (buffer instanceof ByteBuffer));
        }
    }

    /**
     * 登陆消息
     *
     * @param message       客户端
     * @param remoteAddress 地址
     * @param session       session
     * @throws Exception 异常
     */
    private void onLoginMessage(Message message, String remoteAddress, IoSession session) throws Exception {
        //获取登陆消息
        LoginInfo loginInfo = MessageFactory.parsePLoginInfo(message.getDataContent());
        logger.info("client login msg:" + remoteAddress + "?username=" + loginInfo.getUsername() + "&password=" + loginInfo.getPassword());
        if (!ObjectUtils.isEmpty(clientMessageListener)) {
            //获取已登录用户的用户ID
            int t_userId = UserProcessor.getUserIdFromSession(session);
            boolean hasLogin = t_userId > 0;
            //已经登陆过了
            if (hasLogin) {
                logger.debug("client has already login:" + remoteAddress + "?username=" + loginInfo.getUsername());
                boolean sendOK = sendMessage(session, MessageFactory.createPLoginInfoResponse(0, t_userId));
                if (sendOK) {
                    saveSession(t_userId, session, loginInfo);
                    return;
                }
                logger.warn("login response failed for:" + remoteAddress);
                return;
            }
            //未登陆，校验登陆
            int code = this.clientMessageListener.onLogin(loginInfo.getUsername(), loginInfo.getPassword(), loginInfo.getExtra());
            int userId = -1;
            //校验通过
            if (code == 0) {
                //获取用户ID
                userId = getNextUserId(loginInfo);
                //响应登陆消息
                boolean sendOK = sendMessage(session, MessageFactory.createPLoginInfoResponse(code, userId));
                if (sendOK) {
                    saveSession(userId, session, loginInfo);
                    return;
                }
                logger.warn("login response failed for:" + remoteAddress);
                return;
            }
            sendMessage(session, MessageFactory.createPLoginInfoResponse(code, userId));
            return;
        }
        logger.warn("login message of client is null:" + remoteAddress);
    }

    /**
     * 普通消息
     *
     * @param remoteAddress 客户端地址
     * @param message       消息体
     * @param session       session
     * @throws Exception 异常
     */
    private void onSimpleMessage(String remoteAddress, Message message, IoSession session) throws Exception {
        logger.info("client normal message:" + remoteAddress);
        // 开始回调
        if (this.clientMessageListener != null) {
            //用户未登陆
            if (!UserProcessor.isLogined(session)) {
                replyDataForNotLogin(session, message);
                return;
            }
            // 客户端发给服务端的消息
            if (message.getTo() == 0) {
                onSimpleMessageToServer(message, session);
                return;
            }
            //打印在线人数
            UserProcessor.getInstance().__printOnline();
            //消息发送到Client
            if (sendMessage(message)) {
                //回调
                this.clientMessageListener.onClientToClient(message.getTo(), message.getFrom(), message.getDataContent());
                return;
            }
            //消息发送失败
            logger.info("message send failed:" + remoteAddress);
            //消息发送失败回调
            boolean offlineProcessedOK = this.clientMessageListener.onClientToClientFailed(message.getTo(), message.getFrom(), message.getDataContent(), message.getFp());
            //消息补偿机制
            if (message.isQoS() && offlineProcessedOK) {
                boolean receivedBackSendSuccess = replyDelegateReceivedBack(session, message);
                if (!receivedBackSendSuccess) return;
                logger.debug("【QoS_伪应答_C2S】向" + message.getFrom() + "发送" + message.getFp() + "的伪应答包成功,from=" + message.getTo() + ".");
                return;
            }
            logger.warn("[IM CORE]>> 客户端" + remoteAddress + "的通用数据传输消息尝试实时发送没有成功，但上层应用层没有成功(或者完全没有)进行离线存储，此消息将被服务端丢弃！");
            return;
        }
        logger.warn("[IM CORE]>> 收到客户端" + remoteAddress + "的通用数据传输消息，但回调对象是null，回调无法继续.");
    }

    private void onSimpleMessageToServer(Message message, IoSession session) throws Exception {
        if (message.getType() == MessageType.Client.FROM_CLIENT_TYPE_OF_RECEIVED) {
            String theFingerPrint = message.getDataContent();
            logger.debug("client msg to server:" + message.getFrom() + "finger:" + theFingerPrint + "received");
            if (this.serverMessageListener != null) {
                this.serverMessageListener.messagesBeReceived(theFingerPrint);
            }
            QoS4SendDaemonS2C.getInstance().remove(theFingerPrint);
            return;
        }
        if (message.isQoS()) {
            if (QoS4ReceiveDaemonC2S.getInstance().hasReceived(message.getFp())) {
                if (QoS4ReceiveDaemonC2S.DEBUG) {
                    logger.debug("【IMCORE】【QoS机制】" + message.getFp() + "已经存在于发送列表中，这是重复包，通知业务处理层收到该包罗！");
                }
                QoS4ReceiveDaemonC2S.getInstance().addReceived(message);
                boolean receivedBackSendSucess = replyDelegateReceivedBack(session, message);
                if (receivedBackSendSucess) {
                    logger.debug("【QoS_应答_C2S】向" + message.getFrom() + "发送" + message.getFp() + "的应答包成功了,from=" + message.getTo() + ".");
                }
                return;
            }
            QoS4ReceiveDaemonC2S.getInstance().addReceived(message);
            boolean receivedBackSendSucess = replyDelegateReceivedBack(session, message);
            if (receivedBackSendSucess) {
                logger.debug("【QoS_应答_C2S】向" + message.getFrom() + "发送" + message.getFp() + "的应答包成功了,from=" + message.getTo() + ".");
            }
        }
        //回调函数
        this.clientMessageListener.onClientToServer(message.getTo(), message.getFrom(), message.getDataContent(), message.getFp());
    }

    private void saveSession(int user_id, IoSession session, LoginInfo loginInfo) {
        // 将用户登陆成功后的id暂存到会话对象中备用
        session.setAttribute(UserProcessor.USER_ID_IN_SESSION_ATTRIBUTE, user_id);
        // 将用户登陆成功后的登陆名暂存到会话对象中备用
        session.setAttribute(UserProcessor.LOGIN_NAME_IN_SESSION_ATTRIBUTE, loginInfo.getUsername());
        // 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
        UserProcessor.getInstance().putUser(user_id, session, loginInfo.getUsername());
        this.clientMessageListener.onLoginSuccess(user_id, loginInfo.getUsername(), session);
    }

    protected int getNextUserId(LoginInfo loginInfo) {
        return UserProcessor.nextUserId(loginInfo);
    }

    protected boolean replyDataForNotLogin(IoSession session, Message message) throws Exception {
        logger.warn("client have not login:" + clientInfoToString(session) + "-msg:" + message.getDataContent());

        return sendMessage(session, MessageFactory.createPErrorResponse(
                301, message.toGsonString(), -1));
    }

    protected boolean replyDelegateReceivedBack(IoSession session, Message message) throws Exception {
        if ((message.isQoS()) && (message.getFp() != null)) {
            Message receivedBackP = MessageFactory.createRecivedBack(message.getTo(), message.getFrom(), message.getFp());

            return sendMessage(session, receivedBackP);
        }
        logger.warn("[IMCORE]收到" + message.getFrom() + "发过来需要QoS的包，但它的指纹码却为null！无法发伪应答包哦！");
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

    private static String bufferToJson(IoBuffer buffer) throws Exception {
        return buffer.getString(CharsetHelper.decoder);
    }

    private static Message fromIOBuffer(IoBuffer buffer) throws Exception {
        return MessageFactory.parse(bufferToJson(buffer), Message.class);
    }
}