/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.executors.impl;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.TimeSource;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.WatchDog;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;

/**
 * Task handler
 */
public class TaskHandlerImpl<T, V> implements BackgroundTaskHandler<V> {

    private Log log = LogFactory.getLog(BackgroundWorker.class);

    private final TaskExecutor<T, V> taskExecutor;
    private final WatchDog watchDog;

    private volatile boolean started = false;

    private long startTimeStamp;
    private UserSession userSession;
    private Window.CloseListener closeListener;

    public TaskHandlerImpl(final TaskExecutor<T, V> taskExecutor, WatchDog watchDog) {
        this.taskExecutor = taskExecutor;
        this.watchDog = watchDog;

        UserSessionSource sessionSource = AppBeans.get(UserSessionSource.NAME);
        this.userSession = sessionSource.getUserSession();

        BackgroundTask<T, V> task = taskExecutor.getTask();
        if (task.getOwnerFrame() != null) {
            IFrame ownerFrame = task.getOwnerFrame();
            if (ownerFrame.getFrame() != null) {
                closeListener = new Window.CloseListener() {
                    @Override
                    public void windowClosed(String actionId) {
                        ownerWindowClosed();
                    }
                };
                Window ownerWindow = ComponentsHelper.getWindowImplementation(ownerFrame);
                ownerWindow.addListener(closeListener);
            }
        }

        // remove close listener on done
        taskExecutor.setFinalizer(new Runnable() {
            @Override
            public void run() {
                String taskDescription = taskExecutor.getTask().toString();

                if (log.isTraceEnabled()) {
                    log.trace("Start task finalizer. Task: " + taskDescription);
                }

                disposeResources();

                if (log.isTraceEnabled()) {
                    log.trace("Finish task finalizer. Task: " + taskDescription);
                }
            }
        });
    }

    private void ownerWindowClosed() {
        if (log.isTraceEnabled()) {
            UUID userId = getUserSession().getId();
            IFrame ownerFrame = getTask().getOwnerFrame();
            String windowClass = ownerFrame.getClass().getCanonicalName();
            log.trace("Window closed. User: " + userId + " Window: " + windowClass);
        }

        taskExecutor.cancelExecution();
    }

    @Override
    public final void execute() {
        String taskDescription = taskExecutor.getTask().toString();

        checkState(!started, "Task is already started. Task: " + taskDescription);

        this.started = true;

        TimeSource timeSource = AppBeans.get(TimeSource.NAME);
        this.startTimeStamp = timeSource.currentTimestamp().getTime();

        this.watchDog.manageTask(this);

        if (log.isTraceEnabled()) {
            UUID userId = getUserSession().getId();
            log.trace(String.format("Run task: %s. User: %s", taskDescription, userId.toString()));
        }

        taskExecutor.startExecution();
    }

    @Override
    public final boolean cancel() {
        String taskDescription = taskExecutor.getTask().toString();

        checkState(started, "Task is not running. Task: " + taskDescription);

        boolean canceled = taskExecutor.cancelExecution();
        if (canceled) {
            BackgroundTask<T, V> task = taskExecutor.getTask();
            try {
                task.canceled();
                // Notify listeners
                for (BackgroundTask.ProgressListener listener : task.getProgressListeners()) {
                    listener.onCancel();
                }
            } finally {
                disposeResources();
            }

            if (log.isTraceEnabled()) {
                UUID userId = getUserSession().getId();
                IFrame ownerFrame = getTask().getOwnerFrame();
                String windowClass = ownerFrame.getClass().getCanonicalName();

                log.trace(String.format("Task was cancelled. Task: %s. User: %s. Frame: %s", taskDescription, userId.toString(), windowClass));
            }
        } else {
            log.trace("Task wasn't cancelled. Execution is already cancelled. Task: " + taskDescription);
        }

        return canceled;
    }

    private void disposeResources() {
        String taskDescription = taskExecutor.getTask().toString();

        // force remove close listener
        IFrame ownerFrame = getTask().getOwnerFrame();
        if (ownerFrame != null) {
            Window ownerWindow = ComponentsHelper.getWindowImplementation(ownerFrame);
            if (ownerWindow != null) {
                ownerWindow.removeListener(closeListener);

                if (log.isTraceEnabled()) {
                    String windowClass = ownerFrame.getClass().getCanonicalName();
                    log.trace(String.format("Resources were disposed. Task: %s. Frame: %s", taskDescription, windowClass));
                }
            } else {
                log.trace("Empty ownerWindow. Resources were not disposed. Task: " + taskDescription);
            }
        } else {
            log.trace("Empty ownerFrame. Resources were not disposed. Task: " + taskDescription);
        }
        closeListener = null;
    }

    /**
     * Join task thread to current <br/>
     * <b>Caution!</b> Call this method only from synchronous gui action
     *
     * @return Task result
     */
    @Override
    public final V getResult() {
        checkState(started, "Task is not running");

        return taskExecutor.getResult();
    }

    /**
     * Cancel without events for tasks
     */
    public final void close() {
        if (AppContext.isStarted()) {
            UUID userId = getUserSession().getId();
            IFrame ownerFrame = getTask().getOwnerFrame();

            disposeResources();

            if (log.isTraceEnabled()) {
                String taskDescription = taskExecutor.getTask().toString();

                if (ownerFrame != null) {
                    String windowClass = ownerFrame.getClass().getCanonicalName();
                    log.trace(String.format("Task killed. Task: %s. User: %s. Frame: %s", taskDescription, userId.toString(), windowClass));
                } else {
                    log.trace(String.format("Task killed. Task: %s. User: %s", taskDescription, userId.toString()));
                }
            }
        }

        taskExecutor.cancelExecution();
    }

    /**
     * Cancel with timeout exceeded event
     */
    public final void timeoutExceeded() {
        IFrame ownerFrame = getTask().getOwnerFrame();
        if (log.isTraceEnabled()) {
            String taskDescription = taskExecutor.getTask().toString();
            if (ownerFrame != null) {
                String windowClass = ownerFrame.getClass().getCanonicalName();
                log.trace(String.format("Task timeout happened. Task: %s. Frame: %s", taskDescription, windowClass));
            } else {
                log.trace("Task timeout happened. Task: " + taskDescription);
            }
        }

        checkState(started, "Task is not running");

        boolean canceled = taskExecutor.cancelExecution();
        if (canceled) {
            try {
                BackgroundTask<T, V> task = taskExecutor.getTask();
                task.handleTimeoutException();
            } finally {
                disposeResources();
            }
        }

        if (log.isTraceEnabled()) {
            String taskDescription = taskExecutor.getTask().toString();
            if (ownerFrame != null) {
                String windowClass = ownerFrame.getClass().getCanonicalName();
                log.trace(String.format("Timeout was processed. Task: %s. Frame: %s", taskDescription, windowClass));
            } else {
                log.trace("Timeout was processed. Task: " + taskDescription);
            }
        }
    }

    @Override
    public final boolean isDone() {
        return taskExecutor.isDone();
    }

    @Override
    public final boolean isCancelled() {
        return taskExecutor.isCancelled();
    }

    @Override
    public final boolean isAlive() {
        return taskExecutor.inProgress() && started;
    }

    public final BackgroundTask<T, V> getTask() {
        return taskExecutor.getTask();
    }

    public final UserSession getUserSession() {
        return userSession;
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public long getTimeoutMs() {
        return taskExecutor.getTask().getTimeoutMilliseconds();
    }
}