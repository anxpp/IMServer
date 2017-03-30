package com.anxpp.tinyim.server.sdk.message.server;

/**
 * 错误响应消息
 */
public class ErrorResponse {
    //错误码
    private int errorCode = -1;
    //错误信息
    private String errorMsg;

    public ErrorResponse(int errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}