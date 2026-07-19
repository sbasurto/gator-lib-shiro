package gator.lib.shiro.sessions;

import java.io.Serializable;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;

/** Administrative operations over the configured persistent session DAO. */
public final class GappSessionControl {

    private GappSessionControl() {
    }

    public static void kill(Serializable sessionId) {
        dao().killSession(sessionId);
    }

    public static void killAll() {
        dao().killAllSessions();
    }

    private static GappSessionDAO dao() {
        if (SecurityUtils.getSecurityManager() instanceof SessionsSecurityManager securityManager
                && securityManager.getSessionManager() instanceof DefaultSessionManager sessionManager
                && sessionManager.getSessionDAO() instanceof GappSessionDAO sessionDAO) {
            return sessionDAO;
        }
        throw new SessionException("GappSessionDAO is not configured");
    }
}
