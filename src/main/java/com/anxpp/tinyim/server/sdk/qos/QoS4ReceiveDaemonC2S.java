package com.anxpp.tinyim.server.sdk.qos;

import com.anxpp.tinyim.server.sdk.protocal.Protocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ConcurrentHashMap;

public class QoS4ReceiveDaemonC2S implements ActionListener {
    private static final int CHECH_INTERVAL = 300000;
    private static final int MESSAGES_VALID_TIME = 600000;
    public static boolean DEBUG = false;
    private static Logger logger = LoggerFactory.getLogger(QoS4ReceiveDaemonC2S.class);
    private static QoS4ReceiveDaemonC2S instance = null;
    private ConcurrentHashMap<String, Long> receivedMessages = new ConcurrentHashMap<>();
    private Timer timer = null;
    private Runnable runnable = null;
    private boolean _excuting = false;

    private QoS4ReceiveDaemonC2S() {
        init();
    }

    public static QoS4ReceiveDaemonC2S getInstance() {
        if (instance == null) {
            instance = new QoS4ReceiveDaemonC2S();
        }

        return instance;
    }

    private void init() {
        this.timer = new Timer(CHECH_INTERVAL, this);
        this.runnable = () -> {
            // 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
            // 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
            if (!QoS4ReceiveDaemonC2S.this._excuting) {
                QoS4ReceiveDaemonC2S.this._excuting = true;

                if (QoS4ReceiveDaemonC2S.DEBUG) {
                    QoS4ReceiveDaemonC2S.logger.debug("【IMCORE】【QoS接收方】++++++++++ START 暂存处理线程正在运行中，当前长度" + QoS4ReceiveDaemonC2S.this.receivedMessages.size() + ".");
                }

                for (String key : QoS4ReceiveDaemonC2S.this.receivedMessages.keySet()) {
                    long delta = System.currentTimeMillis() - QoS4ReceiveDaemonC2S.this.receivedMessages.get(key);

                    if (delta < MESSAGES_VALID_TIME)
                        continue;
                    if (QoS4ReceiveDaemonC2S.DEBUG)
                        QoS4ReceiveDaemonC2S.logger.debug("【IMCORE】【QoS接收方】指纹为" + key + "的包已生存" + delta +
                                "ms(最大允许" + MESSAGES_VALID_TIME + "ms), 马上将删除之.");
                    QoS4ReceiveDaemonC2S.this.receivedMessages.remove(key);
                }
            }

            if (QoS4ReceiveDaemonC2S.DEBUG) {
                QoS4ReceiveDaemonC2S.logger.debug("【IMCORE】【QoS接收方】++++++++++ END 暂存处理线程正在运行中，当前长度" + QoS4ReceiveDaemonC2S.this.receivedMessages.size() + ".");
            }

            QoS4ReceiveDaemonC2S.this._excuting = false;
        };
    }

    public void actionPerformed(ActionEvent e) {
        this.runnable.run();
    }

    public void startup() {
        stop();

        if ((this.receivedMessages != null) && (this.receivedMessages.size() > 0)) {
            for (String key : this.receivedMessages.keySet()) {
                putImpl(key);
            }

        }

        this.timer.start();
    }

    public void stop() {
        if (this.timer.isRunning())
            this.timer.stop();
    }

    public boolean isRunning() {
        return this.timer.isRunning();
    }

    public void addReceived(Protocal p) {
        if ((p != null) && (p.isQoS()))
            addReceived(p.getFp());
    }

    private void addReceived(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal == null) {
            logger.debug("【IMCORE】无效的 fingerPrintOfProtocal==null!");
            return;
        }

        if (this.receivedMessages.containsKey(fingerPrintOfProtocal)) {
            logger.debug("【IMCORE】【QoS接收方】指纹为" + fingerPrintOfProtocal +
                    "的消息已经存在于接收列表中，该消息重复了（原理可能是对方因未收到应答包而错误重传导致），更新收到时间戳哦.");
        }

        putImpl(fingerPrintOfProtocal);
    }

    private void putImpl(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal != null)
            this.receivedMessages.put(fingerPrintOfProtocal, System.currentTimeMillis());
    }

    public boolean hasReceived(String fingerPrintOfProtocal) {
        return this.receivedMessages.containsKey(fingerPrintOfProtocal);
    }

    public int size() {
        return this.receivedMessages.size();
    }
}