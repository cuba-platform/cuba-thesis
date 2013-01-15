/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.portal.restapi;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.security.app.UserSessionService;
import com.haulmont.cuba.security.global.UserSession;

import java.util.UUID;

/**
 * @author chevelev
 * @version $Id$
 */
public class Authentication {
    public static Authentication me(String sessionId) {
        UserSession userSession = getSession(sessionId);
        if (userSession == null) {
            return null;
        }
        AppContext.setSecurityContext(new SecurityContext(userSession));
        return new Authentication();
    }

    public void forget() {
        AppContext.setSecurityContext(null);
    }

    private static UserSession getSession(String sessionIdStr) {
        UUID sessionId;
        try {
            sessionId = UUID.fromString(sessionIdStr);
        } catch (Exception e) {
            return null;
        }
        return AppBeans.get(UserSessionService.class).getUserSession(sessionId);
    }
}