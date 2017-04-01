package com.anxpp.tinyim.server.sdk.listener.impl;

import com.anxpp.tinyim.server.sdk.listener.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.message.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 服务端主动发起消息的QoS回调通知实现类
 * Created by yangtao on 2017/3/30.
 */
@Component
public class SimpleServerMessageListenerImpl implements ServerMessageListener {

    // 消息无法完成实时送达的通知
    @Override
    public void messagesLost(ArrayList<Message> lostMessages) {
        System.out.println("【QoS_S2C事件】收到系统的未实时送达事件通知，当前共有"
                + lostMessages.size() + "个包QoS保证机制结束，判定为【无法实时送达】！");
    }

    // 接收方已成功收到消息的通知
    @Override
    public void messagesBeReceived(String theFingerPrint) {
        if (theFingerPrint != null) {
            System.out.println("【QoS_S2C事件】收到对方已收到消息事件的通知，fp=" + theFingerPrint);
        }
    }
}