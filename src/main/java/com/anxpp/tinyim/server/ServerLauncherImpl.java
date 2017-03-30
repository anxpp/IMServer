package com.anxpp.tinyim.server;

import com.anxpp.tinyim.server.sdk.ServerLauncher;
import com.anxpp.tinyim.server.sdk.qos.QoS4SendDaemonS2C;

import java.io.IOException;

/**
 * 服务端最终配置和实现
 * Created by yangtao on 2017/3/30.
 */
public class ServerLauncherImpl extends ServerLauncher {
    private static ServerLauncherImpl instance = null;

    private ServerLauncherImpl() throws IOException {
        super();
    }

    public static ServerLauncherImpl getInstance() throws IOException {
        if (instance == null) {
            ServerLauncher.appKey = "tinyim";
            QoS4SendDaemonS2C.DEBUG = false;
            ServerLauncherImpl.PORT = 1114;
            instance = new ServerLauncherImpl();
        }
        return instance;
    }

    public static void main(String[] args) throws IOException {
        ServerLauncherImpl.getInstance().startup();
    }

    /**
     * 初始化消息处理事件监听者.
     */
    @Override
    protected void initListeners() {
        //设置回调
        this.setServerEventListener(new ClientMessageListenerImpl());
        this.setServerMessageQoSEventListener(new ServerMessageListenerImpl());
    }
}