package com.anxpp.tinyim.server.sdk;

import com.anxpp.tinyim.server.sdk.event.ClientMessageListener;
import com.anxpp.tinyim.server.sdk.event.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.message.Message;
import com.anxpp.tinyim.server.sdk.qos.QoS4ReceiveDaemonC2S;
import com.anxpp.tinyim.server.sdk.qos.QoS4SendDaemonS2C;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public abstract class ServerLauncher {
    protected static String appKey = null;
    protected static int PORT = 7901;
    protected static int SESSION_RECYCLER_EXPIRE = 10;
    private static Logger logger = LoggerFactory.getLogger(ServerLauncher.class);
    protected ServerCoreHandler serverCoreHandler = null;
    private boolean running = false;
    private NioDatagramAcceptor acceptor = null;

    public ServerLauncher() throws IOException {
    }

    public static boolean sendData(int from_user_id, int to_user_id, String dataContent) throws Exception {
        return ServerCoreHandler.sendMessage(from_user_id, to_user_id, dataContent);
    }

    public static boolean sendData(int from_user_id, int to_user_id, String dataContent, boolean QoS) throws Exception {
        return ServerCoreHandler.sendMessage(from_user_id, to_user_id, dataContent, QoS);
    }

    public static boolean sendData(int from_user_id, int to_user_id
            , String dataContent, boolean QoS, String fingerPrint) throws Exception {
        return ServerCoreHandler.sendMessage(from_user_id, to_user_id, dataContent,
                QoS, fingerPrint);
    }

    public static boolean sendData(Message p) throws Exception {
        return ServerCoreHandler.sendMessage(p);
    }

    public static boolean sendData(IoSession session, Message p) throws Exception {
        return ServerCoreHandler.sendMessage(session, p);
    }

    public static void setSenseMode(SenseMode mode) {
        int expire = 0;

        switch (mode) {
            case MODE_3S:
                // 误叛容忍度为丢3个包
                expire = 3 * 3 + 1;
                break;
            case MODE_10S:
                // 误叛容忍度为丢2个包
                expire = 10 * 2 + 1;
                break;
            case MODE_30S:
                // 误叛容忍度为丢2个包
                expire = 30 * 2 + 2;
                break;
            case MODE_60S:
                // 误叛容忍度为丢2个包
                expire = 60 * 2 + 2;
                break;
            case MODE_120S:
                // 误叛容忍度为丢2个包
                expire = 120 * 2 + 2;
                break;
        }

        if (expire > 0)
            SESSION_RECYCLER_EXPIRE = expire;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void shutdown() {

        // ** 取消服务端网络监听
        if (acceptor != null)
            acceptor.dispose();

        // ** 停止QoS机制（目前服务端只支持C2S模式的QoS）下的防重复检查线程
        QoS4ReceiveDaemonC2S.getInstance().stop();
        // ** 停止服务端对S2C模式下QoS机制的丢包重传和离线通知线程
        QoS4SendDaemonS2C.getInstance().stop();

        // ** 设置启动标识
        this.running = false;
    }

    public void startup() throws IOException {
        this.serverCoreHandler = initServerCoreHandler();
        initListeners();
        this.acceptor = initAcceptor();
        initFilter(this.acceptor);
        initSessionConfig(this.acceptor);
        QoS4ReceiveDaemonC2S.getInstance().startup();
        QoS4SendDaemonS2C.getInstance().startup(true).setServerLauncher(this);
        this.acceptor.bind(new InetSocketAddress(PORT));

        this.running = true;
        logger.info("server started at " + PORT);
    }

    private ServerCoreHandler initServerCoreHandler() {
        return new ServerCoreHandler();
    }

    protected abstract void initListeners();

    private NioDatagramAcceptor initAcceptor() {
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        acceptor.getFilterChain()
                .addLast("threadPool", new ExecutorFilter(Executors.newCachedThreadPool()));
        acceptor.setHandler(this.serverCoreHandler);
        acceptor.setSessionRecycler(new ExpiringSessionRecycler(SESSION_RECYCLER_EXPIRE));
        return acceptor;
    }

    private void initFilter(NioDatagramAcceptor acceptor) {
        DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
    }

    private void initSessionConfig(NioDatagramAcceptor acceptor) {
        DatagramSessionConfig config = acceptor.getSessionConfig();
        config.setReuseAddress(true);
//     	config.setReadBufferSize(4096);//设置接收最大字节默认2048
        config.setReceiveBufferSize(1024);//设置输入缓冲区的大小，调整到2048后性能反而降低
        config.setSendBufferSize(1024);//1024//设置输出缓冲区的大小，调整到2048后性能反而降低
    }

    public ClientMessageListener getServerEventListener() {
        return this.serverCoreHandler.getClientMessageListener();
    }

    public void setServerEventListener(ClientMessageListener clientMessageListener) {
        this.serverCoreHandler.setClientMessageListener(clientMessageListener);
    }

    public ServerMessageListener getServerMessageQoSEventListener() {
        return this.serverCoreHandler.getServerMessageListener();
    }

    public void setServerMessageQoSEventListener(ServerMessageListener serverMessageQoSEventListener) {
        this.serverCoreHandler.setServerMessageListener(serverMessageQoSEventListener);
    }

    public static enum SenseMode {
        MODE_3S,
        MODE_10S,
        MODE_30S,
        MODE_60S,
        MODE_120S
    }

}