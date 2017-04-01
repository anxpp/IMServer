package com.anxpp.tinyim.server.sdk.message.client;

/**
 * 登陆信息
 */
public class LoginInfo {

    //用户名
    private String username;
    //密码
    private String password;
    //额外信息
    private String extra;

    public LoginInfo(String username, String password, String extra) {
        this.username = username;
        this.password = password;
        this.extra = extra;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}