package com.anxpp.tinyim.server.sdk.processor;

import java.util.HashMap;
import java.util.Iterator;


import com.anxpp.tinyim.server.sdk.ServerCoreHandler;
import com.anxpp.tinyim.server.sdk.protocal.c.LoginInfo;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserProcessor {
    private static Logger logger = LoggerFactory.getLogger(UserProcessor.class);

    private static boolean DEBUG = false;

    public static final String USER_ID_IN_SESSION_ATTRIBUTE = "__user_id__";
    public static final String LOGIN_NAME_IN_SESSION_ATTRIBUTE = "__login_name__";

    private static int __id = 10000;
    private static UserProcessor instance = null;

    private final HashMap<Integer, IoSession> userSessions = new HashMap<>();
    private HashMap<Integer, String> userNames = new HashMap<>();

    public static UserProcessor getInstance() {
        if (instance == null)
            instance = new UserProcessor();
        return instance;
    }

    private UserProcessor() {
//		new SessionChecker().start();
    }

    public static int nextUserId(LoginInfo loginInfo) {
        return ++__id;
    }

    public void putUser(int user_id, IoSession session, String loginName) {
        if (this.userSessions.containsKey(Integer.valueOf(user_id))) {
            logger.debug("[IMCORE]【注意】用户id=" + user_id + "已经在在线列表中了，session也是同一个吗？" + (
                    this.userSessions.get(Integer.valueOf(user_id)).hashCode() == session.hashCode()));
        }

        // 将用户加入到在线列表中
        userSessions.put(user_id, session);
        // 加入用户名列表（用户列表以后或许可用于处理同名用户登陆的登陆问题！）
        if (loginName != null)
            userNames.put(user_id, loginName);

        __printOnline();
    }

    public void __printOnline() {
        logger.debug("【@】当前在线用户共(" + this.userSessions.size() + ")人------------------->");
        if (DEBUG) {
            for (Iterator localIterator = this.userSessions.keySet().iterator(); localIterator.hasNext(); ) {
                int key = (Integer) localIterator.next();
                logger.debug("      > user_id=" + key + ",session=" + this.userSessions.get(key).getRemoteAddress());
            }
        }
    }

    public boolean removeUser(int user_id) {
        synchronized (this.userSessions) {
            if (!this.userSessions.containsKey(user_id)) {
                logger.warn("[IMCORE]！用户id=" + user_id + "不存在在线列表中，本次removeUser没有继续.");

                __printOnline();
                return false;
            }

            boolean removeOK = this.userSessions.remove(user_id) != null;
            this.userNames.remove(user_id);
            return removeOK;
        }
    }

    /**
     * @deprecated
     */
    public boolean removeUser(IoSession session) {
        synchronized (this.userSessions) {
            if (!this.userSessions.containsValue(session)) {
                logger.warn("[IMCORE]！用户" + ServerCoreHandler.clientInfoToString(session) + "的会话=" + "不存在在线列表中，本次removeUser没有继续.");
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
        for (Iterator localIterator = this.userSessions.keySet().iterator(); localIterator.hasNext(); ) {
            int id = (Integer) localIterator.next();

            if (this.userSessions.get(id) == session) {
                return id;
            }
        }
        return -1;
    }

    public IoSession getSession(int user_id) {
        return (IoSession) this.userSessions.get(user_id);
    }

    public String getLoginName(int user_id) {
        return (String) this.userNames.get(user_id);
    }

    public HashMap<Integer, IoSession> getUserSessions() {
        return this.userSessions;
    }

    public HashMap<Integer, String> getUserNames() {
        return this.userNames;
    }

    public static boolean isLogined(IoSession session) {
        return (session != null) && (getUserIdFromSession(session) != -1);
    }

    public static int getUserIdFromSession(IoSession session) {
        Object attr;
        if (session != null) {
            attr = session.getAttribute("__user_id__");
            if (attr != null) {
                return (Integer) attr;
            }
        }
        return -1;
    }

    public static String getLoginNameFromSession(IoSession session) {
        Object attr;
        if (session != null) {
            attr = session.getAttribute("__login_name__");
            if (attr != null) {
                return (String) attr;
            }
        }
        return null;
    }
}