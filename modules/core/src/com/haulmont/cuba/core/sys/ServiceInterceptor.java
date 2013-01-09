/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.global.Logging;
import com.haulmont.cuba.core.global.RemoteException;
import com.haulmont.cuba.security.app.UserSessionsAPI;
import com.haulmont.cuba.security.global.NoUserSessionException;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Intercepts invocations of the middleware services.
 * <p/> Checks {@link UserSession} validity and wraps exceptions into {@link RemoteException}.
 *
 * @author krivopustov
 * @version $Id$
 */
public class ServiceInterceptor {

    private UserSessionsAPI userSessions;

    private Log log = LogFactory.getLog(getClass());

    public void setUserSessions(UserSessionsAPI userSessions) {
        this.userSessions = userSessions;
    }

    private Object aroundInvoke(ProceedingJoinPoint ctx) throws Throwable {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (element.getClassName().equals(ServiceInterceptor.class.getName())) {
                log.error("Invoking " + ctx.getSignature() + " from another service");
                break;
            }
        }

        try {
            UserSession userSession = getUserSession(ctx);
            if (log.isTraceEnabled())
                log.trace("Invoking: " + ctx.getSignature() + ", session=" + userSession);

            Object res = ctx.proceed();
            return res;
        } catch (Throwable e) {
            logException(e, ctx);
            // Propagate the special exception to avoid serialization errors on remote clients
            throw new RemoteException(e);
        }
    }

    private UserSession getUserSession(ProceedingJoinPoint ctx) {
        SecurityContext securityContext = AppContext.getSecurityContext();
        if (securityContext == null)
            throw new SecurityException("No security context bound to the current thread");

        // Using UserSessionsAPI directly to make sure the session's "last used" timestamp is propagated to the cluster
        UserSession userSession = userSessions.get(securityContext.getSessionId(), true);
        if (userSession == null)
            throw new NoUserSessionException(securityContext.getSessionId());

        return userSession;
    }

    private void logException(Throwable e, ProceedingJoinPoint ctx) {
        Logging annotation = e.getClass().getAnnotation(Logging.class);
        if (annotation == null || annotation.value() == Logging.Type.FULL) {
            log.error("Uncaught exception: ", e);
        } else if (annotation.value() == Logging.Type.BRIEF) {
            log.error("Uncaught exception in " + ctx.getSignature().toShortString() + ": " + e.toString());
        }
    }
}
