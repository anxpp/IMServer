package com.anxpp.tinyim.server.sdk.manager;

import com.anxpp.tinyim.server.sdk.handler.ServerMessageHandler;
import com.anxpp.tinyim.server.sdk.config.BaseConfig;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;

/**
 * 用户会话管理
 */
@Component
public class SessionManager {

    @Resource
    private ServerMessageHandler serverMessageHandler;

    public static final String USER_ID_IN_SESSION_ATTRIBUTE = "__user_id__";
    public static final String LOGIN_NAME_IN_SESSION_ATTRIBUTE = "__login_name__";
    private static Logger logger = LoggerFactory.getLogger(SessionManager.class);

    //用户session
    private final HashMap<Integer, IoSession> userSessions = new HashMap<>();
    //用户名session
    private HashMap<Integer, String> userNames = new HashMap<>();

    /**
     * 是否已登陆
     *
     * @param session 会话
     * @return true为已登陆
     */
    public boolean isLogin(IoSession session) {
        return (session != null) && (getUserIdFromSession(session) != -1);
    }

    /**
     * 从session中获取用户id
     *
     * @param session 会话
     * @return 用户id
     */
    public int getUserIdFromSession(IoSession session) {
        Object attr;
        if (session != null) {
            attr = session.getAttribute("__user_id__");
            if (attr != null) {
                return (Integer) attr;
            }
        }
        return -1;
    }

    /**
     * 从session获取用户名
     *
     * @param session 会话
     * @return 用户名
     */
    public String getUsernameFromSession(IoSession session) {
        Object attr;
        if (session != null) {
            attr = session.getAttribute("__login_name__");
            if (attr != null) {
                return (String) attr;
            }
        }
        return null;
    }

    /**
     * 添加用户session
     *
     * @param userId   用户id
     * @param session  会话
     * @param username 用户名
     */
    public void putUser(int userId, IoSession session, String username) {
        if (this.userSessions.containsKey(userId)) {
            return;
        }
        // 将用户加入到在线列表中
        userSessions.put(userId, session);
        // 加入用户名列表（用户列表以后或许可用于处理同名用户登陆的登陆问题！）
        if (username != null)
            userNames.put(userId, username);

        printOnline();
    }

    /**
     * 打印在线用户信息
     */
    public void printOnline() {
        logger.debug("count of online people:" + this.userSessions.size());
        if (BaseConfig.DEBUG) {
            for (Integer key : this.userSessions.keySet()) {
                logger.debug("user_id=" + key + ",session=" + this.userSessions.get(key).getRemoteAddress());
            }
        }
    }

    /**
     * 从session移除用户
     *
     * @param userId 用户id
     * @return 是否成功
     */
    public boolean removeUser(int userId) {
        synchronized (this.userSessions) {
            if (!this.userSessions.containsKey(userId)) {
                logger.warn("user id is not in sessions:" + userId);
                printOnline();
                return false;
            }
            boolean removeOK = this.userSessions.remove(userId) != null;
            this.userNames.remove(userId);
            return removeOK;
        }
    }

    /**
     * @deprecated
     */
    public boolean removeUser(IoSession session) {
        synchronized (this.userSessions) {
            if (!this.userSessions.containsValue(session)) {
                logger.warn("[IMCORE]！用户" + serverMessageHandler.clientInfoToString(session) + "的会话=" + "不存在在线列表中，本次removeUser没有继续.");
            } else {
                int user_id = getId(session);
                if (user_id != -1) {
                    boolean removeOK = this.userSessions.remove(user_id) != null;
                    this.userNames.remove(user_id);
                    return removeOK;
                }

            }

        }

        return false;
    }

    /**
     * @deprecated
     */
    public int getId(IoSession session) {
        for (Integer id : this.userSessions.keySet()) {
            if (this.userSessions.get(id) == session) {
                return id;
            }
        }
        return -1;
    }

    public IoSession getSession(int userId) {
        return this.userSessions.get(userId);
    }
}