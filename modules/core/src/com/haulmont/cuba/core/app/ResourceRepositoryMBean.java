/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 10.12.2008 15:19:38
 *
 * $Id$
 */
package com.haulmont.cuba.core.app;

/**
 * Management interface of the {@link ResourceRepository} MBean.<br>
 */
public interface ResourceRepositoryMBean
{
    String OBJECT_NAME = "haulmont.cuba:service=ResourceRepository";

    /**
     * Get direct reference to application interface. Direct means no proxies or container interceptors.
     * <p>DEPRECATED - lookup API directly
     */
    @Deprecated
    ResourceRepositoryAPI getAPI();

    String printContent();

    void evict(String name);

    void evictAll();

    String getResAsString(String name);
}
