package com.anxpp.tinyim.server.sdk.qos;

import com.anxpp.tinyim.server.sdk.config.Config;
import com.anxpp.tinyim.server.sdk.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息接收
 */
@Component
public class QosReceived implements ActionListener {
    private static final int CHECK_INTERVAL = 300000;
    //日志工具
    private static Logger logger = LoggerFactory.getLogger(QosReceived.class);
    //消息最长保留时间
    private static final int MESSAGES_VALID_TIME = 600000;
    //未接收消息
    private ConcurrentHashMap<String, Long> receivedMessages = new ConcurrentHashMap<>();
    private Timer timer = null;
    private Runnable runnable = null;
    private boolean isRunning = false;

    public QosReceived() {
        init();
    }

    private void init() {
        timer = new Timer(CHECK_INTERVAL, this);
        runnable = () -> {
            // 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
            // 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
            if (!isRunning) {
                isRunning = true;
                if (Config.DEBUG) {
                    logger.debug("failed message cache length:" + size());
                }
                //遍历未接收消息
                for (String key : receivedMessages.keySet()) {
                    long delay = System.currentTimeMillis() - receivedMessages.get(key);
                    if (delay < MESSAGES_VALID_TIME)
                        continue;
                    if (Config.DEBUG)
                        logger.debug("receiver key:" + key + " will be delete because if MESSAGES_VALID_TIME");
                    //移除消息
                    receivedMessages.remove(key);
                }
            }
            if (Config.DEBUG) {
                QosReceived.logger.debug("qos for receive thread running for length:" + size());
            }
            isRunning = false;
        };
    }

    public void actionPerformed(ActionEvent e) {
        runnable.run();
    }

    public void startup() {
        stop();
        if ((this.receivedMessages != null) && (this.receivedMessages.size() > 0)) {
            for (String key : this.receivedMessages.keySet()) {
                if (key != null)
                    receivedMessages.put(key, System.currentTimeMillis());
            }
        }
        timer.start();
    }

    public void stop() {
        if (timer.isRunning())
            timer.stop();
    }

    public void addReceived(Message message) {
        if ((message != null) && (message.isQos())) {
            if (message.getKey() == null) {
                logger.debug("unknown key for message");
                return;
            }
            if (receivedMessages.containsKey(message.getKey())) {
                logger.debug("key of message has been contained:" + message.getKey());
            }
            if (message.getKey() != null)
                receivedMessages.put(message.getKey(), System.currentTimeMillis());
        }
    }

    public boolean hasReceived(String fingerPrintOfProtocal) {
        return receivedMessages.containsKey(fingerPrintOfProtocal);
    }

    private int size() {
        return receivedMessages.size();
    }
}