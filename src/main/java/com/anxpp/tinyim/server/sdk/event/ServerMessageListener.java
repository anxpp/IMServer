package com.anxpp.tinyim.server.sdk.event;

import com.anxpp.tinyim.server.sdk.message.Message;

import java.util.ArrayList;

/**
 * Server消息监听
 */
public interface ServerMessageListener {

    void messagesLost(ArrayList<Message> paramArrayList);

    void messagesBeReceived(String paramString);
}