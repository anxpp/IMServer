package com.anxpp.tinyim.server.sdk.listener;

import com.anxpp.tinyim.server.sdk.message.Message;

import java.util.ArrayList;

/**
 * Server消息监听
 */
public interface ServerMessageListener {

    /**
     * 消息丢失
     *
     * @param paramArrayList 丢失的消息列表
     */
    void messagesLost(ArrayList<Message> paramArrayList);

    /**
     * 消息被接收
     *
     * @param paramString 消息
     */
    void messagesBeReceived(String paramString);
}