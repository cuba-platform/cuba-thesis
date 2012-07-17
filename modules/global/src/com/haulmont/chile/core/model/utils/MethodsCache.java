/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 15.12.2008 17:24:17
 * $Id: MethodsCache.java 6249 2011-10-18 05:13:00Z krivopustov $
 */
package com.haulmont.chile.core.model.utils;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;

public class MethodsCache {
    private final transient Map<String, Method> getters = new HashMap<String, Method>();
    private final transient Map<String, Method> setters = new HashMap<String, Method>();

    public MethodsCache(Class clazz) {
        final Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") && method.getParameterTypes().length == 0) {
                name = StringUtils.uncapitalize(name.substring(3));
                getters.put(name, method);
            } if (name.startsWith("is") && method.getParameterTypes().length == 0) {
                name = StringUtils.uncapitalize(name.substring(2));
                getters.put(name, method);
            } else if (name.startsWith("set") && method.getParameterTypes().length == 1) {
                name = StringUtils.uncapitalize(name.substring(3));
                setters.put(name, method);
            }
        }
    }

    public void invokeSetter(Object object, String property, Object value)
    {
        final Method method = setters.get(property);
        if (method == null)
            throw new IllegalArgumentException(
                    String.format("Can't find setter for property '%s' at class %s", property, object.getClass()));
        try {
            method.invoke(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Object invokeGetter(Object object, String property)
    {
        final Method method = getters.get(property);
        if (method == null)
            throw new IllegalArgumentException(
                    String.format("Can't find getter for property '%s' at class %s", property, object.getClass()));
        try {
            return method.invoke(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
