/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.gui.executors;

import com.haulmont.cuba.core.global.TimeProvider;
import com.haulmont.cuba.core.sys.AppContext;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * WatchDog for BackgroundWorker
 * <p>$Id$</p>
 *
 * @author artamonov
 */
public class TasksWatchDog implements WatchDog {

    private final Set<BackgroundWorker.TaskHandler> watches;

    public TasksWatchDog() {
        watches = new LinkedHashSet<BackgroundWorker.TaskHandler>();
    }

    /**
     * {@inheritDoc}
     */
    public void cleanupTasks() {
        if (!AppContext.isStarted())
            return;

        synchronized (watches) {
            long actual = TimeProvider.currentTimestamp().getTime();

            List<BackgroundWorker.TaskHandler> forRemove = new LinkedList<BackgroundWorker.TaskHandler>();
            for (BackgroundWorker.TaskHandler task : watches) {
                if (task.isCancelled() || task.isDone()) {
                    forRemove.add(task);
                } else if (task.checkHangup(actual)) {
                    task.close();
                    forRemove.add(task);
                }
            }

            watches.removeAll(forRemove);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stopTasks() {
        if (!AppContext.isStarted())
            return;

        synchronized (watches) {
            for (BackgroundWorker.TaskHandler task : watches) {
                task.close();
            }
            watches.clear();
        }
    }

    /**
     * {@inheritDoc}
     * @param backroundTask Task handler
     */
    public void manageTask(BackgroundWorker.TaskHandler backroundTask) {
        synchronized (watches) {
            watches.add(backroundTask);
        }
    }
}