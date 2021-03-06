package com.anxpp.tinyim.server.sdk.service;

import com.anxpp.tinyim.server.sdk.message.StatusCode;
import com.anxpp.tinyim.server.sdk.message.client.LoginInfo;
import org.springframework.util.ObjectUtils;

/**
 * 用户登陆服务
 * Created by yangtao on 2017/4/1.
 */
public interface UserLoginService {
    /**
     * 获取登陆用户ID
     *
     * @param loginInfo 登陆信息
     * @return 用户ID，为0时表示获取失败
     */
    default Integer findUserId(LoginInfo loginInfo) {
        return loginInfo.getUsername().hashCode();
    }

    /**
     * 用户登陆
     *
     * @param loginInfo 登陆信息
     * @return 登陆返回标志
     */
    default int login(LoginInfo loginInfo) {
        return ObjectUtils.isEmpty(loginInfo) ? StatusCode.FAIL : StatusCode.SUCCESS;
    }

}