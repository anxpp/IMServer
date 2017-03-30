package com.anxpp.tinyim.server.sdk.message;

/**
 * 消息类型
 */
public interface MessageType {
    /**
     * 客户端消息
     */
    interface Client {
        int FROM_CLIENT_TYPE_OF_LOGIN = 0;
        int FROM_CLIENT_TYPE_OF_KEEP$ALIVE = 1;
        int FROM_CLIENT_TYPE_OF_COMMON$DATA = 2;
        int FROM_CLIENT_TYPE_OF_LOGOUT = 3;
        int FROM_CLIENT_TYPE_OF_RECEIVED = FROM_CLIENT_TYPE_OF_COMMON$DATA;
        int FROM_CLIENT_TYPE_OF_ECHO = 5;
    }

    /**
     * 服务端消息
     */
    interface Server {
        int FROM_SERVER_TYPE_OF_RESPONSE$LOGIN = 50;
        int FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE = 51;
        int FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR = 52;
        int FROM_SERVER_TYPE_OF_RESPONSE$ECHO = 53;
    }
}