package com.anxpp.tinyim.server.sdk.qos;

import com.anxpp.tinyim.server.sdk.config.BaseConfig;
import com.anxpp.tinyim.server.sdk.QosConfig;
import com.anxpp.tinyim.server.sdk.handler.ServerMessageHandler;
import com.anxpp.tinyim.server.sdk.listener.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.swing.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息发送服务质量
 */
@Component
public class QosSend {

    //配置
    @Resource
    private QosConfig qosConfig;
    //日志工具
    private static Logger logger = LoggerFactory.getLogger(QosSend.class);
    @Resource
    private ServerMessageListener serverMessageListener;
    @Resource
    private ServerMessageHandler serverMessageHandler;
    /**
     * 失败消息map
     * key   消息key
     * value 消息体
     */
    private ConcurrentHashMap<String, Message> allFailedMessage = new ConcurrentHashMap<>();
    /**
     * 失败消息的时间map
     * key   消息key
     * value 时间
     */
    private ConcurrentHashMap<String, Long> allFailedMessageTimeStrap = new ConcurrentHashMap<>();
    //服务是否在运行
    private volatile boolean isRunning = false;
    //定时器
    private Timer timer;

    /**
     * 开启服务
     */
    public synchronized void startup() {
        stop();
        (timer = new Timer(qosConfig.getCheckInterval(), actionListener -> listen())).setInitialDelay(qosConfig.getCheckInterval());
        timer.start();
    }

    private void listen() {
        if (!isRunning) {
            isRunning = true;
            //重发失败的消息
            ArrayList<Message> lostMessages = new ArrayList<>();
            if (BaseConfig.DEBUG) {
                logger.debug("qos of send begin check:" + size());
            }
            for (String key : allFailedMessage.keySet()) {
                Message message = allFailedMessage.get(key);
                if (message != null && message.isQos()) {
                    doQos(message);
                } else {
                    allFailedMessageTimeStrap.remove(key);
                }
            }
            if (lostMessages.size() > 0) {
                notifyMessageLost(lostMessages);
            }
            isRunning = false;
        }
    }

    /**
     * 处理消息重发
     *
     * @param message 消息体
     * @return 最终重发失败的消息
     */
    private ArrayList<Message> doQos(Message message) {
        ArrayList<Message> failedMessages = new ArrayList<>();
        if (message.getRetryCount() >= qosConfig.getMaxResendTimes()) {
            if (BaseConfig.DEBUG) {
                logger.debug("key of qos send:" + message.getKey() + " has retry for max " + message.getRetryCount() + " times and will be remove");
            }
            //发送失败消息
            failedMessages.add((Message) message.clone());
            //移除消息
            allFailedMessageTimeStrap.remove(message.getKey());
        } else {
            long delay = System.currentTimeMillis() - allFailedMessageTimeStrap.get(message.getKey());
            if (delay <= qosConfig.getJustNowTime()) {
                if (BaseConfig.DEBUG)
                    logger.warn("key of qos send is a just now message");
            } else {
                boolean sendOK = false;
                try {
                    sendOK = serverMessageHandler.sendMessage(message);
                } catch (Exception ignored) {
                }
                if (sendOK)
                    allFailedMessageTimeStrap.remove(message.getKey());
                else
                    message.increaseRetryCount();
                if (BaseConfig.DEBUG) {
                    logger.debug(sendOK ? "key of qos send:" + message.getKey() + " has be retry success" : "key of qos send:" + message.getKey() + " resend failed  times:" + message.getRetryCount());
                }
            }
        }
        return failedMessages;
    }

    /**
     * 通知消息发送失败
     *
     * @param lostMessages 失败的消息
     */
    private void notifyMessageLost(ArrayList<Message> lostMessages) {
        serverMessageListener.messagesLost(lostMessages);
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    public boolean exist(String key) {
        return allFailedMessage.get(key) != null;
    }

    public void put(Message message) {
        if (message == null) {
            if (BaseConfig.DEBUG)
                logger.warn("Invalid arg message==null.");
            return;
        }
        if (message.getKey() == null) {
            if (BaseConfig.DEBUG)
                logger.warn("Invalid arg message.getKey() == null.");
            return;
        }
        if (!message.isQos()) {
            if (BaseConfig.DEBUG)
                logger.warn("This protocal is not QoS pkg, ignore it!");
            return;
        }
        if (this.allFailedMessage.get(message.getKey()) != null) {
            if (BaseConfig.DEBUG) {
                logger.warn("【IMCORE】【QoS发送方】指纹为" + message.getKey() + "的消息已经放入了发送质量保证队列，该消息为何会重复？（生成的指纹码重复？还是重复put？）");
            }
        }
        // save it
        allFailedMessage.put(message.getKey(), message);
        // 同时保存时间戳
        allFailedMessageTimeStrap.put(message.getKey(), System.currentTimeMillis());
    }

    public void remove(String key) {
        allFailedMessageTimeStrap.remove(key);
    }

    private int size() {
        return this.allFailedMessage.size();
    }
}