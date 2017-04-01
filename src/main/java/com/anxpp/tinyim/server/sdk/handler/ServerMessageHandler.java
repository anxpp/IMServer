package com.anxpp.tinyim.server.sdk.handler;

import com.anxpp.tinyim.server.sdk.config.Config;
import com.anxpp.tinyim.server.sdk.listener.ClientMessageListener;
import com.anxpp.tinyim.server.sdk.listener.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.manager.SessionManager;
import com.anxpp.tinyim.server.sdk.message.CharsetHelper;
import com.anxpp.tinyim.server.sdk.message.Message;
import com.anxpp.tinyim.server.sdk.message.MessageFactory;
import com.anxpp.tinyim.server.sdk.message.MessageType;
import com.anxpp.tinyim.server.sdk.message.client.LoginInfo;
import com.anxpp.tinyim.server.sdk.qos.QosReceived;
import com.anxpp.tinyim.server.sdk.qos.QosSend;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.nio.ByteBuffer;

/**
 * 服务端消息集中处理器
 */
@Component
public class ServerMessageHandler extends IoHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(ServerMessageHandler.class);

    //会话管理器
    @Resource
    private SessionManager sessionManager;

    // 服务端事件回调实现
    @Resource
    private ClientMessageListener clientMessageListener;

    // QoS机制下的S2C模式中，由服务端主动发起消息的QoS事件回调实现
    @Resource
    private ServerMessageListener serverMessageListener;

    @Resource
    private QosReceived qosReceived;

    @Resource
    private QosSend qosSend;

    /**
     * 发送消息
     *
     * @param message 消息体
     * @return 发送状态
     * @throws Exception 异常
     */
    public boolean sendMessage(Message message) throws Exception {
        return !(ObjectUtils.isEmpty(message) || message.getTo() == 0) && sendMessage(sessionManager.getSession(message.getTo()), message);
    }

    /**
     * 发送消息
     *
     * @param session 会话
     * @param message 消息体
     * @return 发送状态
     * @throws Exception 异常
     */
    private boolean sendMessage(IoSession session, Message message) throws Exception {
        if (ObjectUtils.isEmpty(message)) { //消息为空
            logger.info("message is null:");
            return false;
        }
        if (ObjectUtils.isEmpty(session)) { //session为空
            logger.info("session is null for message from " + message.getFrom() + " the received user " + message.getTo() + ":" + message.getContent());
            return false;
        }
        if (!session.isConnected()) {       //用户不在线 TODO
            logger.warn("user is offline:" + message.getTo());
        }
        //发送消息
        WriteFuture future = session.write(IoBuffer.wrap(message.toBytes()));
        future.awaitUninterruptibly(100L);
        if (future.isWritten()) {
            if (message.getFrom() == 0) {
                if (message.isQos() && !qosSend.exist(message.getKey())) {
                    qosSend.put(message);
                }
            }
            return true;
        }
        logger.info("message to " + clientInfoToString(session) + ":" + message.toJson() + " send failed");
        return false;
    }

    public String clientInfoToString(IoSession session) {
        return "{userName:" + sessionManager.getUsernameFromSession(session) + ",userId:" + sessionManager.getUserIdFromSession(session) + "}" + session.getRemoteAddress().toString();
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
                //客户端已接收到消息
                case MessageType.Client.REPORT:
                    onReportMessage(message);
                    break;
                //普通消息
                case MessageType.Client.SIMPLE:
                    onSimpleMessage(remoteAddress, message, session);
                    break;
                //心跳
                case MessageType.Client.HEART:
                    if (!sessionManager.isLogin(session)) {
                        replyForNotLogin(session, message);
                        break;
                    }
                    sendMessage(MessageFactory.createPKeepAliveResponse(sessionManager.getUserIdFromSession(session)));
                    break;
                //登陆
                case MessageType.Client.LOGIN: {
                    onLoginMessage(message, remoteAddress, session);
                    break;
                }
                //登出
                case MessageType.Client.LOGOUT: {
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
        LoginInfo loginInfo = MessageFactory.parsePLoginInfo(message.getContent());
        logger.info("client login msg:" + remoteAddress + "?username=" + loginInfo.getUsername() + "&password=" + loginInfo.getPassword());
        //获取已登录用户的用户ID
        int t_userId = sessionManager.getUserIdFromSession(session);
        boolean hasLogin = t_userId > 0;
        //已经登陆过了
        if (hasLogin) {
            logger.debug("client has already login:" + remoteAddress + "?username=" + loginInfo.getUsername());
            boolean sendSuccess = sendMessage(session, MessageFactory.createPLoginInfoResponse(0, t_userId));
            if (sendSuccess) {
                saveSession(t_userId, session, loginInfo);
                return;
            }
            logger.warn("login response failed for:" + remoteAddress);
            return;
        }
        //未登陆，校验登陆
        int code = clientMessageListener.onLogin(loginInfo.getUsername(), loginInfo.getPassword(), loginInfo.getExtra());
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
    }

    /**
     * 客户端报告消息
     *
     * @param message 消息体
     * @throws Exception 异常
     */
    private void onReportMessage(Message message) throws Exception {
        logger.info("client report message:" + message.getFrom());
        String key = message.getKey();
        //回调消息已被接收
        serverMessageListener.messagesBeReceived(key);
        qosSend.remove(key);
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
        //用户未登陆
        if (!sessionManager.isLogin(session)) {
            replyForNotLogin(session, message);
            return;
        }
        // 客户端发给服务端的消息
        if (message.getTo() == 0) {
            onSimpleMessageToServer(message, session);
            return;
        }
        //打印在线人数
        sessionManager.printOnline();
        //消息发送到Client
        if (sendMessage(message)) {
            //回调
            clientMessageListener.onClientToClient(message.getTo(), message.getFrom(), message.getContent());
            return;
        }
        //消息发送失败
        logger.info("message send failed:" + remoteAddress);
        //消息发送失败回调
        boolean offlineProcessedOK = clientMessageListener.onClientToClientFailed(message.getTo(), message.getFrom(), message.getContent(), message.getKey());
        //消息补偿机制
        if (message.isQos() && offlineProcessedOK) {
            boolean receivedBackSendSuccess = replyDelegateReceivedBack(session, message);
            if (!receivedBackSendSuccess)
                return;
            logger.debug("【QoS_伪应答_C2S】向" + message.getFrom() + "发送" + message.getKey() + "的伪应答包成功,from=" + message.getTo() + ".");
        }
    }

    /**
     * client到server的消息
     *
     * @param message 消息体
     * @param session 会话
     * @throws Exception 异常
     */
    private void onSimpleMessageToServer(Message message, IoSession session) throws Exception {
        //客户端已接收消息
        if (message.isQos()) {
            if (qosReceived.hasReceived(message.getKey())) {
                if (Config.DEBUG) {
                    logger.debug("【IMCORE】【QoS机制】" + message.getKey() + "已经存在于发送列表中，这是重复包，通知业务处理层收到该包罗！");
                }
                qosReceived.addReceived(message);
                boolean receivedBackSendSucess = replyDelegateReceivedBack(session, message);
                if (receivedBackSendSucess) {
                    logger.debug("【QoS_应答_C2S】向" + message.getFrom() + "发送" + message.getKey() + "的应答包成功了,from=" + message.getTo() + ".");
                }
                return;
            }
            qosReceived.addReceived(message);
            boolean receivedBackSendSucess = replyDelegateReceivedBack(session, message);
            if (receivedBackSendSucess) {
                logger.debug("【QoS_应答_C2S】向" + message.getFrom() + "发送" + message.getKey() + "的应答包成功了,from=" + message.getTo() + ".");
            }
        }
        //回调函数
        this.clientMessageListener.onClientToServer(message.getTo(), message.getFrom(), message.getContent(), message.getKey());
    }

    private void saveSession(int userId, IoSession session, LoginInfo loginInfo) {
        // 将用户登陆成功后的id暂存到会话对象中备用
        session.setAttribute(SessionManager.USER_ID_IN_SESSION_ATTRIBUTE, userId);
        // 将用户登陆成功后的登陆名暂存到会话对象中备用
        session.setAttribute(SessionManager.LOGIN_NAME_IN_SESSION_ATTRIBUTE, loginInfo.getUsername());
        // 将用户信息放入到在线列表中（理论上：每一个存放在在线列表中的session都对应了user_id）
        sessionManager.putUser(userId, session, loginInfo.getUsername());
        this.clientMessageListener.onLoginSuccess(userId, loginInfo.getUsername(), session);
    }

    private int getNextUserId(LoginInfo loginInfo) {
        return sessionManager.nextUserId(loginInfo);
    }

    private boolean replyForNotLogin(IoSession session, Message message) throws Exception {
        logger.warn("client have not login:" + clientInfoToString(session) + "-msg:" + message.getContent());
        return sendMessage(session, MessageFactory.createPErrorResponse(301, message.toJson(), -1));
    }

    private boolean replyDelegateReceivedBack(IoSession session, Message message) throws Exception {
        if ((message.isQos()) && (message.getKey() != null)) {
            Message receivedBackP = MessageFactory.createRecivedBack(message.getTo(), message.getFrom(), message.getKey());
            return sendMessage(session, receivedBackP);
        }
        return false;
    }

    public void sessionClosed(IoSession session) throws Exception {
        int user_id = sessionManager.getUserIdFromSession(session);
        String loginName = sessionManager.getUsernameFromSession(session);
        logger.info("[IMCORE]与" + clientInfoToString(session) + "的会话关闭(user_id=" + user_id + ",loginName=" + loginName + ")了...");
        if (user_id != -1) {
            sessionManager.removeUser(user_id);

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

    private String bufferToJson(IoBuffer buffer) throws Exception {
        return buffer.getString(CharsetHelper.decoder);
    }

    private Message fromIOBuffer(IoBuffer buffer) throws Exception {
        return MessageFactory.parse(bufferToJson(buffer), Message.class);
    }
}