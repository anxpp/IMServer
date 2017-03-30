package com.anxpp.tinyim.server.sdk.event;

import org.apache.mina.core.session.IoSession;

/**
 * 客户端消息监听
 */
public interface ClientMessageListener {
    int onLogin(String username, String password, String extra);

    void onLoginSuccess(int userId, String userName, IoSession session);

    void onLogout(int userId, Object paramObject);

    boolean onClientToServer(int serverId, int userId, String msg, String paramString2);

    void onClientToClient(int toUserId, int fromUserId, String msg);

    boolean onClientToClientFailed(int toUserId, int fromUserId, String msg, String paramString2);
}