/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.exception;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.web.App;
import com.vaadin.ui.Window;
import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.jdbc.ReportingSQLException;

import javax.annotation.Nullable;

/**
 * Handles database "numeric overflow" exception.
 *
 * @author degtyarjov
 * @version $Id$
 */
public class NumericOverflowExceptionHandler extends AbstractExceptionHandler {

    public NumericOverflowExceptionHandler() {
        super(ReportingSQLException.class.getName());
    }

    @Override
    protected boolean canHandle(String className, String message, @Nullable Throwable throwable) {
        return StringUtils.containsIgnoreCase(message, "Numeric field overflow");
    }

    @Override
    protected void doHandle(App app, String className, String message, @Nullable Throwable throwable) {
        if (StringUtils.containsIgnoreCase(message, "Numeric field overflow")) {
            String msg = AppBeans.get(Messages.class).getMessage(getClass(), "numericFieldOverflow.message");
            app.getAppWindow().showNotification(msg, Window.Notification.TYPE_ERROR_MESSAGE);
        }
    }
}