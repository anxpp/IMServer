package com.anxpp.tinyim.server.sdk.listener.impl;

import com.anxpp.tinyim.server.sdk.listener.ClientMessageListener;
import org.apache.mina.core.session.IoSession;
import org.springframework.stereotype.Component;

/**
 * 框架基本事件回调实现类
 * Created by yangtao on 2017/3/30.
 */
@Component
public class SimpleClientMessageListenerImpl implements ClientMessageListener {

    // 用户身份验证回调方法定义
    // 服务端的应用层可在本方法中实现用户登陆验证。详细请参见API文档说明。
    @Override
    public int onLogin(String username, String password, String extra, int code) {
        System.out.println("login:user=");
        return 0;
    }

    // 用户登录验证成功后的回调方法定义
    // 服务端的应用层通常可在本方法中实现用户上线通知等。详细请参见API文档说明。
    @Override
    public void onLoginSuccess(int userId, String userName, IoSession session) {
        System.out.println("onLoginSuccess");
    }

    // 用户退出登录回调方法定义。
    // 服务端的应用层通常可在本方法中实现用户下线通知等。详细请参见API文档说明。
    @Override
    public void onLogout(int userId, Object obj) {
        System.out.println("onLogout");
    }

    // 通用数据回调方法定义（客户端发给服务端的（即接收user_id=0））
    // 上层通常可在本方法中实现如：添加好友请求等业务实现。详细请参见API文档说明。
    @Override
    public boolean onClientToServer(int serverId, int userId, String msg, String fingerPrint) {
        System.out.println("client to server :" + userId + ":msg=" + msg);
        return true;
    }

    // 通道数据回调函数定义（客户端发给客户端的（即接收user_id>0））。详细请参见API文档说明。
    // 上层通常可在本方法中实现用户聊天信息的收集，以便后期监控分析用户的行为等^_^。
    @Override
    public void onClientToClient(int toUserId, int fromUserId, String msg) {
        System.out.println("client to client:" + fromUserId + "to" + toUserId + ":msg=" + msg);
    }

    // 通用数据实时发送失败后的回调函数定义（客户端发给客户端的（即接收user_id>0））
    // 开发者可在此方法中处理离线消息的保存等。详细请参见API文档说明。
    @Override
    public boolean onClientToClientFailed(int toUserId, int fromUserId, String msg, String fingerString) {
        System.out.println("client to client Failed:" + fromUserId + "to" + toUserId + ":msg=" + msg);
        return false;
    }
}