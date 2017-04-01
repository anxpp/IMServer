package com.anxpp.tinyim.server.sdk.message;

/**
 * 消息类型
 */
public interface MessageType {
    /**
     * 客户端消息
     */
    interface Client {
        //客户端登陆
        int LOGIN = 0;
        //客户端心跳
        int HEART = 1;
        //客户端通用
        int SIMPLE = 2;
        //客户端登出
        int LOGOUT = 3;
        //客户端消息已接收
        int REPORT = 4;
    }

    /**
     * 服务端消息
     */
    interface Server {
        int RESPONSE_LOGIN = 50;
        int RESPONSE_HEART = 51;
        int RESPONSE$FOR_ERROR = 52;
    }
}