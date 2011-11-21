/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.exception;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.sys.AppContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import java.util.LinkedList;
import java.util.Map;

/**
 * Class that holds the collection of exception handlers and delegates unhandled exception processing to them. Handlers
 * form the chain of responsibility.
 *
 * <p>A set of exception handlers is configured by defining <code>ExceptionHandlersConfiguration</code> beans
 * in spring.xml. If a project needs specific handlers, it should define a bean of such type with its own
 * <strong>id</strong>, e.g. <code>refapp_ExceptionHandlersConfiguration</code></p>
 *
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@ManagedBean("cuba_ExceptionHandlers")
public class ExceptionHandlers {

    protected LinkedList<ExceptionHandler> handlers = new LinkedList<ExceptionHandler>();

    protected ExceptionHandler defaultHandler;

    private Log log = LogFactory.getLog(getClass());

    public ExceptionHandlers() {
        this.defaultHandler = new DefaultExceptionHandler();
    }

    /**
     * Adds new handler if it is not yet registered.
     * @param handler   handler instance
     */
    public void addHandler(ExceptionHandler handler) {
        if (!handlers.contains(handler))
            handlers.add(handler);
    }

    /**
     * Return all registered handlers.
     * @return  modifiable handlers list
     */
    public LinkedList<ExceptionHandler> getHandlers() {
        return handlers;
    }

    /**
     * Delegates exception handling to registered handlers.
     * @param thread    current thread
     * @param exception exception instance
     */
    public void handle(Thread thread, Throwable exception) {
        for (ExceptionHandler handler : handlers) {
            if (handler.handle(thread, exception))
                return;
        }
        defaultHandler.handle(thread, exception);
    }

    /**
     * Create all handlers defined by <code>ExceptionHandlersConfiguration</code> beans in spring.xml.
     */
    public void createByConfiguration() {
        Map<String, ExceptionHandlersConfiguration> map = AppContext.getBeansOfType(ExceptionHandlersConfiguration.class);
        for (ExceptionHandlersConfiguration conf : map.values()) {
            for (Class aClass : conf.getHandlerClasses()) {
                try {
                    handlers.add(ReflectionHelper.<ExceptionHandler>newInstance(aClass));
                } catch (NoSuchMethodException e) {
                    log.error("Unable to instantiate " + aClass, e);
                }
            }
        }
    }

    /**
     * Remove all handlers.
     */
    public void removeAll() {
        handlers.clear();
    }
}
