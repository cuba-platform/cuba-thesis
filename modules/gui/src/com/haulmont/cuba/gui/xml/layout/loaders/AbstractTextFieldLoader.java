/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.TextInputField;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

/**
 * @author abramov
 * @version $Id$
 */
public abstract class AbstractTextFieldLoader extends AbstractFieldLoader {

    public AbstractTextFieldLoader(Context context, LayoutLoaderConfig config, ComponentsFactory factory) {
        super(context, config, factory);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent)
            throws InstantiationException, IllegalAccessException {
        final TextInputField component = (TextInputField) super.loadComponent(factory, element, parent);

        loadStyleName(component, element);

        return component;
    }

    protected void loadTrimming(Element element, TextInputField.TrimSupported component) {
        String trim = element.attributeValue("trim");
        if (!StringUtils.isEmpty(trim)) {
            component.setTrimming(Boolean.valueOf(trim));
        }
    }

    protected void loadMaxLength(Element element, TextInputField.MaxLengthLimited component) {
        final String maxLength = element.attributeValue("maxLength");
        if (!StringUtils.isEmpty(maxLength)) {
            component.setMaxLength(Integer.valueOf(maxLength));
        }
    }
}