/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.12.2008 15:27:37
 * $Id: IFrameLoader.java 69 2009-01-22 12:19:45Z abramov $
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.ScriptingProvider;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.io.IOUtils;
import org.dom4j.Element;

import java.io.InputStream;

public class IFrameLoader extends ContainerLoader implements ComponentLoader {

    public IFrameLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) throws InstantiationException, IllegalAccessException {
        String src = element.attributeValue("src");
        final String screenId = element.attributeValue("screen");
        if (src == null && screenId == null) {
            throw new RuntimeException("Either src or screen must be specified for <iframe>");
        }
        if (src == null) {
            WindowInfo windowInfo = AppBeans.get(WindowConfig.class).getWindowInfo(screenId);
            src = windowInfo.getTemplate();
            if (src == null) {
                throw new RuntimeException("Screen " + screenId + " doesn't have template path configured");
            }
        }
        final LayoutLoader loader = new LayoutLoader(context, factory, LayoutLoaderConfig.getFrameLoaders());
        loader.setLocale(getLocale());
        loader.setMessagesPack(getMessagesPack());

        InputStream stream = ScriptingProvider.getResourceAsStream(src);
        if (stream == null) {
            stream = getClass().getResourceAsStream(src);
            if (stream == null) {
                throw new RuntimeException("Bad template path: " + src);
            }
        }

        final IFrame component;
        try {
            component = (IFrame) loader.loadComponent(stream, parent, context.getParams());
        } finally {
            IOUtils.closeQuietly(stream);
        }
        if (component.getMessagesPack() == null) {
            component.setMessagesPack(messagesPack);
        }

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);

        loadAlign(component, element);

        loadHeight(component, element, ComponentsHelper.getComponentHeigth(component));
        loadWidth(component, element, ComponentsHelper.getComponentWidth(component));

        if (context.getFrame() != null)
            component.setFrame(context.getFrame());

        return component;
    }
}