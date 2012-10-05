/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.logging;

/**
 * Logger class for UI performance stats
 * Contains constants for screen life cycle
 *
 * @author artamonov
 * @version $Id$
 */
public interface UIPerformanceLogger {

    enum LifeCycle {
        LOAD_DESCRIPTOR("loadDescriptor"),
        INIT("init"),
        SET_ITEM("setItem"),
        UI_PERMISSIONS("uiPermissions"),
        INJECTION("inject"),
        COMPANION("companion");

        private String name;

        LifeCycle(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}