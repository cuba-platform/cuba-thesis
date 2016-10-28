/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.backgroundwork;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.IFrame.NotificationType;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

public class LocalizedTaskWrapper<T, V> extends BackgroundTask<T, V> {

    protected BackgroundTask<T, V> wrappedTask;
    protected Window window;

    protected LocalizedTaskWrapper(BackgroundTask<T, V> wrappedTask, Window window) {
        super(wrappedTask.getTimeoutSeconds(), window);
        this.wrappedTask = wrappedTask;
        this.window = window;
    }

    @Override
    public Map<String, Object> getParams() {
        return wrappedTask.getParams();
    }

    @Override
    public V run(TaskLifeCycle<T> lifeCycle) throws Exception {
        return wrappedTask.run(lifeCycle);
    }

    @Override
    public boolean handleException(final Exception ex) {
        boolean handled = wrappedTask.handleException(ex);

        if (handled || wrappedTask.getOwnerFrame() == null) {
            window.close("", true);
        } else {
            window.closeAndRun("close", new Runnable() {
                @Override
                public void run() {
                    showExecutionError(ex);
                }
            });

            handled = true;
        }
        return handled;
    }

    @Override
    public boolean handleTimeoutException() {
        boolean handled = wrappedTask.handleTimeoutException();
        if (handled || wrappedTask.getOwnerFrame() == null) {
            window.close("", true);
        } else {
            window.closeAndRun("close", new Runnable() {
                @Override
                public void run() {
                    Messages messages = AppBeans.get(Messages.NAME);

                    wrappedTask.getOwnerFrame().showNotification(
                            messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.timeout"),
                            messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.timeoutMessage"),
                            NotificationType.WARNING);
                }
            });
            handled = true;
        }
        return handled;
    }

    @Override
    public void done(V result) {
        window.close("", true);

        try {
            wrappedTask.done(result);
        } catch (Exception ex) {
            // we should show exception messages immediately
            showExecutionError(ex);
        }
    }

    @Override
    public void canceled() {
        try {
            wrappedTask.canceled();
        } catch (Exception ex) {
            // we should show exception messages immediately
            showExecutionError(ex);
        }
    }

    @Override
    public void progress(List<T> changes) {
        wrappedTask.progress(changes);
    }

    protected void showExecutionError(Exception ex) {
        IFrame ownerFrame = wrappedTask.getOwnerFrame();
        if (ownerFrame != null) {
            String localizedMessage = ex.getLocalizedMessage();

            Messages messages = AppBeans.get(Messages.NAME);
            if (StringUtils.isNotBlank(localizedMessage)) {
                ownerFrame.showNotification(
                        messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.executionError"),
                        localizedMessage, NotificationType.WARNING);
            } else {
                ownerFrame.showNotification(
                        messages.getMessage(LocalizedTaskWrapper.class, "backgroundWorkProgress.executionError"),
                        NotificationType.WARNING);
            }
        }
    }
}