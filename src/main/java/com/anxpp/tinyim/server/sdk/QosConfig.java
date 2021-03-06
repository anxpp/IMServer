package com.anxpp.tinyim.server.sdk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * qos配置
 * Created by yangtao on 2017/4/1.
 */
@Component
@ConfigurationProperties(prefix = "qos")
public class QosConfig {

    //检查间隔
    private Integer checkInterval = 5000;

    //多少时间内算刚发送的时间
    private Integer justNowTime = 2000;

    //最大重发次数
    private Integer maxResendTimes = 3;

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

    @Override
    public String toString() {
        return "QosConfig{" +
                "checkInterval=" + checkInterval +
                ", justNowTime=" + justNowTime +
                ", maxResendTimes=" + maxResendTimes +
                '}';
    }
}