/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 23.04.2009 15:33:04
 * $Id$
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public class ScrollBoxLayoutLoader extends ContainerLoader implements com.haulmont.cuba.gui.xml.layout.ComponentLoader {

    private Log log = LogFactory.getLog(getClass());

    public ScrollBoxLayoutLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) throws InstantiationException, IllegalAccessException {
        final ScrollBoxLayout component = factory.createComponent(ScrollBoxLayout.NAME);

        assignXmlDescriptor(component, element);
        loadId(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);

        loadAlign(component, element);
        loadOrientation(component, element);

        loadSpacing(component, element);
        loadMargin(component, element);

        loadSubComponents(component, element, "visible");

        for (Component child : component.getOwnComponents()) {
            if (component.getOrientation() == ScrollBoxLayout.Orientation.VERTICAL && ComponentsHelper.hasFullHeight(child)) {
                child.setHeight("-1px");
                log.warn("100% height of " + child.getClass().getSimpleName() + " id=" + child.getId()
                        + " inside vertical scrollBox replaced with -1px height");
            }
            if (component.getOrientation() == ScrollBoxLayout.Orientation.HORIZONTAL && ComponentsHelper.hasFullWidth(child)) {
                child.setWidth("-1px");
                log.warn("100% width of " + child.getClass().getSimpleName() + " id=" + child.getId()
                        + " inside horizontal scrollBox replaced with -1px width");
            }
        }

        loadHeight(component, element);
        loadWidth(component, element);

        assignFrame(component);

        return component;
    }

    protected void loadOrientation(ScrollBoxLayout component, Element element) {
        String orientation = element.attributeValue("orientation");
        if (orientation == null)
            return;

        if ("horizontal".equalsIgnoreCase(orientation)) {
            component.setOrientation(ScrollBoxLayout.Orientation.HORIZONTAL);
        } else if ("vertical".equalsIgnoreCase(orientation)) {
            component.setOrientation(ScrollBoxLayout.Orientation.VERTICAL);
        } else {
            throw new IllegalStateException("Invalid scrollbox orientation value: " + orientation);
        }
    }
}