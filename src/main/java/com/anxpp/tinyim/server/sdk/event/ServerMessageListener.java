package com.anxpp.tinyim.server.sdk.event;

import com.anxpp.tinyim.server.sdk.protocal.Protocal;

import java.util.ArrayList;

public interface ServerMessageListener {

    void messagesLost(ArrayList<Protocal> paramArrayList);

    void messagesBeReceived(String paramString);
}