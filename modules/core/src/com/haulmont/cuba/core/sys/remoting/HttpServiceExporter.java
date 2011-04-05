/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.remoting;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class HttpServiceExporter extends HttpInvokerServiceExporter implements BeanNameAware {

    public HttpServiceExporter() {
        super();
        setRemoteInvocationExecutor(new CubaRemoteInvocationExecutor());
    }

    @Override
    public void setService(Object service) {
        super.setService(service);
    }

    public void setBeanName(String name) {
        Object service = getService();
        if (service == null)
            throw new IllegalStateException("Target service is null");

        LocalServiceInvoker invoker = new LocalServiceInvokerImpl(service);
        LocalServiceDirectory.registerInvoker(name, invoker);
    }
}
