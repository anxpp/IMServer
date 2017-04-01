package com.anxpp.tinyim.server;

import com.anxpp.tinyim.server.sdk.config.Config;
import com.anxpp.tinyim.server.sdk.handler.ServerMessageHandler;
import com.anxpp.tinyim.server.sdk.listener.ServerMessageListener;
import com.anxpp.tinyim.server.sdk.qos.QosReceived;
import com.anxpp.tinyim.server.sdk.qos.QosSend;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 服务端启动入口
 */
@Component
public abstract class ServerStarter {

    private static int SESSION_RECYCLER_EXPIRE = 10;
    private static Logger logger = LoggerFactory.getLogger(ServerStarter.class);

    @Resource
    private ServerMessageHandler serverMessageHandler;
    @Resource
    private ServerMessageListener serverMessageListener;
    @Resource
    private QosReceived qosReceived;
    @Resource
    private QosSend qosSend;

    private volatile boolean isRunning = false;
    private NioDatagramAcceptor acceptor;

    public void shutdown() {
        // ** 取消服务端网络监听
        if (acceptor != null)
            acceptor.dispose();
        qosReceived.stop();
        qosSend.stop();
        this.isRunning = false;
    }

    public void startup() throws IOException {
        if (isRunning)
            return;
        isRunning = true;
        initFilter(acceptor = initAcceptor());
        DatagramSessionConfig config = acceptor.getSessionConfig();
        config.setReuseAddress(true);
        // config.setReadBufferSize(4096);//设置接收最大字节默认2048
        config.setReceiveBufferSize(1024);//设置输入缓冲区的大小，调整到2048后性能反而降低
        config.setSendBufferSize(1024);//1024//设置输出缓冲区的大小，调整到2048后性能反而降低
        qosReceived.startup();
        qosSend.startup();
        acceptor.bind(new InetSocketAddress(Config.PORT));
        logger.info("server started at " + Config.PORT);
    }

    private NioDatagramAcceptor initAcceptor() {
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        acceptor.getFilterChain().addLast("threadPool", new ExecutorFilter(Executors.newCachedThreadPool()));
        acceptor.setHandler(this.serverMessageHandler);
        //回收超时失效的会话
        acceptor.setSessionRecycler(new ExpiringSessionRecycler(SESSION_RECYCLER_EXPIRE));
        return acceptor;
    }

    private void initFilter(NioDatagramAcceptor acceptor) {
        DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
    }

    public static enum SenseMode {
        MODE_3S,
        MODE_10S,
        MODE_30S,
        MODE_60S,
        MODE_120S
    }

    public ServerMessageListener getServerMessageListener() {
        return serverMessageListener;
    }
}