package com.anxpp.tinyim.server.sdk.message.server;

/**
 * 登陆响应消息
 */
public class LoginInfoResponse {
    private int code;
    private int userId;

    public LoginInfoResponse(int code, int userId) {
        this.code = code;
        this.userId = userId;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}