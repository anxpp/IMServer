package com.anxpp.tinyim.server.sdk.message;

import com.google.gson.Gson;

import java.util.UUID;

/**
 * 消息体
 */
public class Message {
    //消息类型
    private int type;
    //消息类型
    private String content;
    //消息来源
    private int from = -1;
    //消息目的地
    private int to = -1;
    //消息唯一key
    private String key = null;
    //是否支持消息容错
    private boolean qos = false;
    //消息重发次数
    private transient int retryCount = 0;

    public Message(int type, String content, int from, int to) {
        this(type, content, from, to, false, null);
    }

    public Message(int type, String content, int from, int to, boolean qos, String key) {
        this.type = type;
        this.content = content;
        this.from = from;
        this.to = to;
        this.qos = qos;
        this.key = qos && key == null ? createKey() : key;
    }

    public static String createKey() {
        return UUID.randomUUID().toString();
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getFrom() {
        return this.from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return this.to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public String getKey() {
        return this.key;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public void increaseRetryCount() {
        this.retryCount += 1;
    }

    public boolean isQos() {
        return this.qos;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public byte[] toBytes() {
        return CharsetHelper.getBytes(toJson());
    }

    public Object clone() {
        // 克隆一个Protocal对象（该对象已重置retryCount数值为0）
        return new Message(getType(), getContent(), getFrom(), getTo(), isQos(), getKey());
    }
}