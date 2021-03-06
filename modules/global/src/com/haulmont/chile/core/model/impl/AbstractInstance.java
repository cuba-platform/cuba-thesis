/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.chile.core.model.impl;

import com.haulmont.chile.core.common.ValueListener;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.chile.core.model.utils.MethodsCache;
import org.apache.commons.lang.ObjectUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author abramov
 * @version $Id$
 */
public abstract class AbstractInstance implements Instance {

    protected transient Collection<WeakReference<ValueListener>> __valueListeners;

    private static transient Map<Class, MethodsCache> methodCacheMap = new ConcurrentHashMap<>();

    protected void propertyChanged(String s, Object obj, Object obj1) {
        if (__valueListeners != null) {
            Collection<WeakReference<ValueListener>> listenersCopy = new ArrayList<>(__valueListeners);
            Collection<WeakReference<ValueListener>> listenersToRemove = new ArrayList<>();
            for (WeakReference<ValueListener> reference : listenersCopy) {
                ValueListener listener = reference.get();
                if (listener == null)
                    listenersToRemove.add(reference);
                else
                    listener.propertyChanged(this, s, obj, obj1);
            }
            __valueListeners.removeAll(listenersToRemove);
        }
    }

    @Override
    public String getInstanceName() {
        return InstanceUtils.getInstanceName(this);
    }

    @Override
    public void addListener(ValueListener valuelistener) {
        if (__valueListeners == null)
            __valueListeners = new ArrayList<>();
        __valueListeners.add(new WeakReference<>(valuelistener));
    }

    @Override
    public void removeListener(ValueListener valuelistener) {
        if (__valueListeners != null) {
            for (Iterator<WeakReference<ValueListener>> it = __valueListeners.iterator(); it.hasNext(); ) {
                ValueListener listener = it.next().get();
                if (listener == null || listener.equals(valuelistener))
                    it.remove();
            }
        }
    }

    @Override
    public void removeAllListeners() {
        if (__valueListeners != null)
            __valueListeners.clear();
    }

    @Override
    public <T> T getValue(String s) {
        return (T) getMethodsCache().invokeGetter(this, s);
    }

    protected MethodsCache getMethodsCache() {
        Class cls = getClass();
        MethodsCache cache = methodCacheMap.get(cls);
        if (cache == null) {
            cache = new MethodsCache(cls);
            methodCacheMap.put(cls, cache);
        }
        return cache;
    }

    @Override
    public void setValue(String s, Object obj) {
        setValue(s, obj, true);
    }

    /**
     * Set value to property in instance
     *
     * @param property    property name
     * @param obj         value
     * @param checkEquals check equals for previous and new value.
     *                    If flag is true and objects equals, then setter will not be invoked
     */
    public void setValue(String property, Object obj, boolean checkEquals) {
        Object oldValue = getValue(property);
        if ((!checkEquals) || (!ObjectUtils.equals(oldValue, obj))) {
            getMethodsCache().invokeSetter(this, property, obj);
        }
    }

    @Override
    public <T> T getValueEx(String s) {
        return InstanceUtils.<T>getValueEx(this, s);
    }

    @Override
    public void setValueEx(String s, Object obj) {
        InstanceUtils.setValueEx(this, s, obj);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractInstance that = (AbstractInstance) o;

        return !(getUuid() != null ? !getUuid().equals(that.getUuid()) : that.getUuid() != null);
    }

    @Override
    public int hashCode() {
        return getUuid() != null ? getUuid().hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getName() + "-" + getUuid();
    }
}
