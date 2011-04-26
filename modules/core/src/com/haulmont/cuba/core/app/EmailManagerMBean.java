/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.app;

/**
 * <p>$Id$</p>
 *
 * @author ovchinnikov
 */

public interface EmailManagerMBean {
    String OBJECT_NAME = "haulmont.cuba:service=Emailer";

    String getDelayCallCountAsString();

    String getMessageQueueCapacityAsString();

}
