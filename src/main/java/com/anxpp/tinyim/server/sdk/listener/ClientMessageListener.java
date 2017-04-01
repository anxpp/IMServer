package com.anxpp.tinyim.server.sdk.listener;

import org.apache.mina.core.session.IoSession;

/**
 * 客户端消息监听
 */
public interface ClientMessageListener {
    /**
     * 客户端登陆消息
     *
     * @param username 用户名
     * @param password 密码
     * @param extra    额外信息
     * @return 0表示成功，其他为错误码
     */
    int onLogin(String username, String password, String extra);

    /**
     * 登陆成功的消息
     *
     * @param userId   用户ID
     * @param userName 用户名
     * @param session  会话
     */
    void onLoginSuccess(int userId, String userName, IoSession session);

    /**
     * 登出消息
     *
     * @param userId      用户id
     * @param paramObject 参数
     */
    void onLogout(int userId, Object paramObject);

    /**
     * 客户端发给服务器的消息
     *
     * @param serverId     服务器id=0
     * @param userId       用户id
     * @param msg          消息
     * @param paramString2 参数
     * @return 处理成功与否
     */
    boolean onClientToServer(int serverId, int userId, String msg, String paramString2);

    /**
     * 客户端发给客户端的消息
     *
     * @param toUserId   消息接收者id
     * @param fromUserId 消息发送者id
     * @param msg        消息
     */
    void onClientToClient(int toUserId, int fromUserId, String msg);

    /**
     * 客户端给客户端发送消息失败的回调
     *
     * @param toUserId     接收者id
     * @param fromUserId   发送者id
     * @param msg          消息
     * @param paramString2 参数
     * @return 处理状态
     */
    boolean onClientToClientFailed(int toUserId, int fromUserId, String msg, String paramString2);
}