/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.security.jmx;

/**
 * JMX interface for {@link com.haulmont.cuba.security.app.UserSessionsAPI}
 *
 * @author krivopustov
 * @version $Id$
 */
public interface UserSessionsMBean {

    /**
     * User session expiration timeout. Not the same as HTTP session timeout, but should have the same value.
     * @return  timeout in seconds
     */
    int getExpirationTimeoutSec();

    /**
     * Set user session expiration timeout for the current server session.
     * @param value timeout in seconds
     */
    void setExpirationTimeoutSec(int value);

    /**
     * @return session send timeout in cluster
     */
    int getSendTimeoutSec();

    /**
     * Set user session ping timeout in cluster.
     * If ping performed {@link com.haulmont.cuba.security.app.UserSessions#get},
     * user session sends in cluster only after specified timeout
     * @param timeout in seconds
     */
    void setSendTimeoutSec(int timeout);

    int getCount();

    String printSessions();

    void processEviction();

    /**
     * Kill specified session
     *
     * @param id Session id
     * @return Result status
     */
    String killSession(String id);
}
