package com.anxpp.tinyim.server.sdk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * qos配置
 * Created by yangtao on 2017/4/1.
 */
@ConfigurationProperties(prefix = "qos")
@Component
public class QosConfig {

    //检查间隔
    private Integer checkInterval;

    //多少时间内算刚发送的时间
    private Integer justNowTime;

    //最大重发次数
    private Integer maxResendTimes;

    public Integer getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Integer checkInterval) {
        this.checkInterval = checkInterval;
    }

    public Integer getJustNowTime() {
        return justNowTime;
    }

    public void setJustNowTime(Integer justNowTime) {
        this.justNowTime = justNowTime;
    }

    public Integer getMaxResendTimes() {
        return maxResendTimes;
    }

    public void setMaxResendTimes(Integer maxResendTimes) {
        this.maxResendTimes = maxResendTimes;
    }
}