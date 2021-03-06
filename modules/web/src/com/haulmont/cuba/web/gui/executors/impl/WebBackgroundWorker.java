/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.executors.impl;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.TimeSource;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.core.sys.SecurityContext;
import com.haulmont.cuba.gui.executors.*;
import com.haulmont.cuba.gui.executors.impl.TaskExecutor;
import com.haulmont.cuba.gui.executors.impl.TaskHandlerImpl;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.AppWindow;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.toolkit.ui.CubaTimer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.vaadin.server.VaadinSession;
import javax.annotation.ManagedBean;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Web implementation of {@link BackgroundWorker}
 */
@ManagedBean(BackgroundWorker.NAME)
public class WebBackgroundWorker implements BackgroundWorker {
    private Log log = LogFactory.getLog(WebBackgroundWorker.class);

    private WatchDog watchDog;

    @Inject
    private UserSessionSource userSessionSource;

    @Inject
    private Configuration configuration;

    @Inject
    public WebBackgroundWorker(WatchDog watchDog) {
        this.watchDog = watchDog;
    }

    /**
     * Simple wrapper for {@link com.haulmont.cuba.gui.components.Timer.TimerListener}
     */
    private static class WebTimerListener {

        private CubaTimer timer;

        private CubaTimer.TimerListener timerListener;

        private WebTimerListener(CubaTimer timer) {
            this.timer = timer;
        }

        public CubaTimer.TimerListener getTimerListener() {
            return timerListener;
        }

        public void setTimerListener(CubaTimer.TimerListener timerListener) {
            this.timerListener = timerListener;
        }

        public void startListen() {
            if (timerListener != null)
                timer.addTimerListener(timerListener);
        }

        public void stopListen() {
            timer.removeTimerListener(timerListener);
        }
    }

    @Override
    public <T, V> BackgroundTaskHandler<V> handle(final BackgroundTask<T, V> task) {
        checkNotNull(task);

        App appInstance;
        try {
            appInstance = App.getInstance();
        } catch (IllegalStateException ex) {
            log.error("Couldn't handle task", ex);
            throw ex;
        }

        // UI timer
        AppWindow appWindow = appInstance.getAppWindow();
        CubaTimer pingTimer = appWindow.getWorkerTimer();

        final WebTimerListener webTimerListener = new WebTimerListener(pingTimer);

        // create task executor
        final WebTaskExecutor<T, V> taskExecutor = new WebTaskExecutor<>(appInstance, task, webTimerListener);

        // add thread to taskSet
        appInstance.addBackgroundTask(taskExecutor);

        // create task handler
        final TaskHandlerImpl<T, V> taskHandler = new TaskHandlerImpl<>(taskExecutor, watchDog);

        taskExecutor.setTaskHandler(taskHandler);

        // add timer to AppWindow for UI ping
        CubaTimer.TimerListener timerListener = new CubaTimer.TimerListener() {
            private long intentVersion = 0;

            @Override
            public void onTimer(CubaTimer timer) {
                UserSessionSource sessionSource = AppBeans.get(UserSessionSource.NAME);
                if (sessionSource.getUserSession() == null) {
                    log.debug("Null UserSession in background task");
                    return;
                }

                // handle intents
                if (!taskExecutor.isCancelled()) {
                    long newIntent = taskExecutor.getIntentVersion();
                    if (intentVersion != newIntent) {
                        intentVersion = newIntent;
                        taskExecutor.handleIntents();
                    }
                }

                // if completed
                if (taskExecutor.isDone()) {
                    taskExecutor.handleDone();
                } else {
                    if (!taskExecutor.isCancelled()) {
                        TimeSource timeSource = AppBeans.get(TimeSource.NAME);
                        long actualTimeMs = timeSource.currentTimestamp().getTime();
                        long timeout = taskHandler.getTimeoutMs();

                        if (timeout > 0 && (actualTimeMs - taskHandler.getStartTimeStamp()) > timeout) {
                            taskHandler.timeoutExceeded();
                        }
                    }
                }

                if (!taskExecutor.isAlive()) {
                    webTimerListener.stopListen();
                }
            }

            @Override
            public void onStopTimer(CubaTimer timer) {
                // Do nothing
            }
        };

        // Start listen only if task started
        webTimerListener.setTimerListener(timerListener);

        return taskHandler;
    }

    /*
     * Task runner
    */
    private class WebTaskExecutor<T, V> extends Thread implements TaskExecutor<T, V> {

        private VaadinSession session;
        private App app;

        private BackgroundTask<T, V> runnableTask;
        private WebTimerListener webTimerListener;
        private Runnable finalizer;

        // canceled
        private volatile boolean canceled = false;

        // task body completed
        private volatile boolean done = false;

        // handleDone completed or canceled
        private volatile boolean closed = false;

        private volatile long intentVersion = 0;
        private final List<T> intents = Collections.synchronizedList(new LinkedList<T>());

        private SecurityContext securityContext;
        private UUID userId;

        private volatile V result = null;
        private volatile Exception taskException = null;

        private Map<String, Object> params;
        private TaskHandlerImpl<T, V> taskHandler;

        private WebTaskExecutor(App app, BackgroundTask<T, V> runnableTask,
                                WebTimerListener webTimerListener) {
            this.runnableTask = runnableTask;
            this.webTimerListener = webTimerListener;
            this.app = app;
            this.session = app.getAppUI().getSession();

            this.params = runnableTask.getParams() != null ?
                    Collections.unmodifiableMap(runnableTask.getParams()) :
                    Collections.<String, Object>emptyMap();

            // copy security context
            this.securityContext = new SecurityContext(AppContext.getSecurityContext().getSession());
            this.userId = userSessionSource.getUserSession().getId();
        }

        @Override
        public final void run() {
            Thread.currentThread().setName("BackgroundTaskThread-" + userId);
            // Set security permissions
            AppContext.setSecurityContext(securityContext);

            V result = null;
            try {
                if (!isInterrupted()) {
                    // do not run any activity if canceled before start
                    result = runnableTask.run(new TaskLifeCycle<T>() {
                        @SafeVarargs
                        @Override
                        public final void publish(T... changes) {
                            handleProgress(changes);
                        }

                        @Override
                        public boolean isInterrupted() {
                            return WebTaskExecutor.this.isInterrupted();
                        }

                        @Override
                        @Nonnull
                        public Map<String, Object> getParams() {
                            return params;
                        }
                    });
                }
            } catch (Exception ex) {
                if (!(ex instanceof InterruptedException) && !canceled) {
                    this.taskException = ex;
                }
            } finally {
                // Set null security permissions
                securityContext = null;
                AppContext.setSecurityContext(null);
                // Save result
                this.result = result;
                // Is done
                if (!isInterrupted()) {
                    done = true;
                }
                // Remove from executions
                app.removeBackgroundTask(this);

                watchDog.removeTask(taskHandler);
            }
        }

        @SafeVarargs
        @Override
        public final void handleProgress(T... changes) {
            synchronized (intents) {
                intentVersion++;
                intents.addAll(Arrays.asList(changes));
            }
        }

        @Override
        public final boolean cancelExecution() {
            if (closed) {
                return false;
            }

            if (log.isTraceEnabled()) {
                log.debug("Cancel task. User: " + userId);
            }

            // Interrupt
            interrupt();

            Runnable removeTaskRunnable = new Runnable() {
                @Override
                public void run() {
                    // Remove task from execution
                    app.removeBackgroundTask(WebTaskExecutor.this);

                    WebTaskExecutor.this.canceled = true;
                    WebTaskExecutor.this.closed = true;

                    stopTimer();
                }
            };

            if (VaadinSession.getCurrent() != session) {
                // access to vaadin session asynchronously
                // to prevent deadlock with com.haulmont.cuba.gui.executors.impl.TasksWatchDog
                session.access(removeTaskRunnable);
            } else {
                removeTaskRunnable.run();
            }

            return true;
        }

        private void stopTimer() {
            if (webTimerListener != null) {
                webTimerListener.stopListen();
            }
        }

        @Override
        public final V getResult() {
            try {
                if (!this.closed) {
                    this.join();
                    this.stopTimer();

                    if (!isCancelled()) {
                        handleIntents();
                    }

                    if (isDone()) {
                        handleDone();
                    }
                }
            } catch (InterruptedException e) {
                return null;
            }
            return result;
        }

        @Override
        public final BackgroundTask<T, V> getTask() {
            return runnableTask;
        }

        @Override
        public final void startExecution() {
            WebConfig webConfig = configuration.getConfig(WebConfig.class);
            int activeTasksCount = watchDog.getActiveTasksCount();

            if (activeTasksCount >= webConfig.getMaxActiveBackgroundTasksCount()) {
                throw new ActiveBackgroundTasksLimitException("Maximum active background tasks limit exceeded");
            }

            // Run timer listener
            webTimerListener.startListen();

            // Start thread
            start();
        }

        @Override
        public final boolean isCancelled() {
            return canceled;
        }

        @Override
        public final boolean isDone() {
            return done;
        }

        @Override
        public final boolean inProgress() {
            return !closed;
        }

        @Override
        public final void setFinalizer(Runnable finalizer) {
            this.finalizer = finalizer;
        }

        @Override
        public final Runnable getFinalizer() {
            return finalizer;
        }

        public final long getIntentVersion() {
            return intentVersion;
        }

        public final void handleIntents() {
            synchronized (intents) {
                if (intents.size() > 0) {
                    runnableTask.progress(intents);
                    // notify listeners
                    for (BackgroundTask.ProgressListener<T, V> listener : runnableTask.getProgressListeners()) {
                        listener.onProgress(intents);
                    }
                    intents.clear();
                }
            }
        }

        public final void handleDone() {
            if (this.closed) {
                return;
            }

            // task cancel here not available
            this.closed = true;

            try {
                if (taskException == null) {
                    runnableTask.done(result);
                    // notify listeners
                    for (BackgroundTask.ProgressListener<T, V> listener : runnableTask.getProgressListeners()) {
                        listener.onDone(result);
                    }
                } else {
                    boolean handled = runnableTask.handleException(taskException);
                    if (!handled) {
                        log.error("Unhandled exception in background task", taskException);
                    }
                }
            } finally {
                if (finalizer != null) {
                    finalizer.run();
                    finalizer = null;
                }
            }
        }

        public void setTaskHandler(TaskHandlerImpl<T,V> taskHandler) {
            this.taskHandler = taskHandler;
        }
    }
}