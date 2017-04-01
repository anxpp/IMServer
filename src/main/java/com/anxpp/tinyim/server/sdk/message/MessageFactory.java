package com.anxpp.tinyim.server.sdk.message;

import com.anxpp.tinyim.server.sdk.message.client.KeepAlive;
import com.anxpp.tinyim.server.sdk.message.client.LoginInfo;
import com.anxpp.tinyim.server.sdk.message.server.ErrorResponse;
import com.anxpp.tinyim.server.sdk.message.server.KeepAliveResponse;
import com.anxpp.tinyim.server.sdk.message.server.LoginInfoResponse;
import com.google.gson.Gson;

/**
 * 消息工厂
 */
public class MessageFactory {
    private static String create(Object data) {
        return new Gson().toJson(data);
    }

    public static <T> T parse(byte[] fullProtocalJASOnBytes, int len, Class<T> clazz) {
        return parse(CharsetHelper.getString(fullProtocalJASOnBytes, len), clazz);
    }

    public static <T> T parse(String dataContentOfProtocal, Class<T> clazz) {
        return new Gson().fromJson(dataContentOfProtocal, clazz);
    }

    public static Message parse(byte[] fullProtocalJASOnBytes, int len) {
        return (Message) parse(fullProtocalJASOnBytes, len, Message.class);
    }

    public static Message createPKeepAliveResponse(int to_user_id) {
        return new Message(MessageType.Server.RESPONSE_HEART,
                create(new KeepAliveResponse()), 0, to_user_id);
    }

    public static KeepAliveResponse parsePKeepAliveResponse(String dataContentOfProtocal) {
        return (KeepAliveResponse) parse(dataContentOfProtocal, KeepAliveResponse.class);
    }

    public static Message createPKeepAlive(int from_user_id) {
        return new Message(MessageType.Client.HEART,
                create(new KeepAlive()), from_user_id, 0);
    }

    public static KeepAlive parsePKeepAlive(String dataContentOfProtocal) {
        return (KeepAlive) parse(dataContentOfProtocal, KeepAlive.class);
    }

    public static Message createPErrorResponse(int errorCode, String errorMsg, int user_id) {
        return new Message(MessageType.Server.RESPONSE$FOR_ERROR,
                create(new ErrorResponse(errorCode, errorMsg)), 0, user_id);
    }

    public static ErrorResponse parsePErrorResponse(String dataContentOfProtocal) {
        return (ErrorResponse) parse(dataContentOfProtocal, ErrorResponse.class);
    }

    public static Message createPLoginoutInfo(int user_id, String loginName) {
        return new Message(MessageType.Client.LOGOUT
//				, create(new PLogoutInfo(user_id, loginName))
                , null
                , user_id, 0);
    }

    public static Message createPLoginInfo(String loginName, String loginPsw, String extra) {
        return new Message(MessageType.Client.LOGIN
                , create(new LoginInfo(loginName, loginPsw, extra)), -1, 0);
    }

    public static LoginInfo parsePLoginInfo(String dataContentOfProtocal) {
        return (LoginInfo) parse(dataContentOfProtocal, LoginInfo.class);
    }

    public static Message createPLoginInfoResponse(int code, int user_id) {
        return new Message(MessageType.Server.RESPONSE_LOGIN,
                create(new LoginInfoResponse(code, user_id)), 0, user_id, true, Message.createKey());
    }

    public static LoginInfoResponse parsePLoginInfoResponse(String dataContentOfProtocal) {
        return parse(dataContentOfProtocal, LoginInfoResponse.class);
    }

    public static Message createCommonData(String dataContent, int from_user_id, int to_user_id, boolean QoS, String fingerPrint) {
        return new Message(MessageType.Client.SIMPLE,
                dataContent, from_user_id, to_user_id, QoS, fingerPrint);
    }

    public static Message createCommonData(String dataContent, int from_user_id, int to_user_id) {
        return new Message(MessageType.Client.SIMPLE,
                dataContent, from_user_id, to_user_id);
    }

    public static Message createRecivedBack(int from_user_id, int to_user_id, String recievedMessageFingerPrint) {
        return new Message(MessageType.Client.REPORT
                , recievedMessageFingerPrint, from_user_id, to_user_id);// 该包当然不需要QoS支持！
    }
}