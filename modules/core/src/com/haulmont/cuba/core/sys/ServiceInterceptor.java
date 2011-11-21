/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 13.11.2008 14:13:23
 *
 * $Id$
 */
package com.haulmont.cuba.core.sys;

import com.haulmont.cuba.core.global.RemoteException;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;

public class ServiceInterceptor
{
    private UserSessionSource userSessionSource;

    public void setUserSessionSource(UserSessionSource userSessionSource) {
        this.userSessionSource = userSessionSource;
    }

    private Object aroundInvoke(ProceedingJoinPoint ctx) throws Throwable {
        Log log = LogFactory.getLog(ctx.getTarget().getClass());

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (element.getClassName().equals(ServiceInterceptor.class.getName())) {
                log.error("Service invoked from another service");
                break;
            }
        }

        try {
            UserSession userSession = userSessionSource.getUserSession();
            if (log.isTraceEnabled())
                log.trace("Invoking: " + ctx.getSignature() + ", session=" + userSession);

            Object res = ctx.proceed();
            return res;
        } catch (Throwable e) {
            log.error("ServiceInterceptor caught exception: ", e);
            // Propagate the special exception to avoid serialization errors on remote clients
            throw new RemoteException(e);
        }
    }
}
