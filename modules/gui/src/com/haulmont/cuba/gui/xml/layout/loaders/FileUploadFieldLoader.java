/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

/**
 * @author abramov
 * @version $Id$
 */
public class FileUploadFieldLoader extends ComponentLoader {

    public FileUploadFieldLoader(Context context) {
        super(context);
    }

    @Override
    public Component loadComponent(ComponentsFactory factory, Element element, Component parent) {
        FileUploadField component = (FileUploadField) factory.createComponent(element.getName());

        initComponent(element, component, parent);

        return component;
    }

    protected void initComponent(Element element, FileUploadField component, Component parent) {
        loadId(component, element);
        loadEnable(component, element);
        loadVisible(component, element);

        loadStyleName(component, element);
        loadAlign(component, element);

        loadHeight(component, element);
        loadWidth(component, element);
        loadIcon(component, element);

        loadCaption(component, element);
        loadDescription(component, element);

        loadAccept(component, element);

        assignFrame(component);
    }

    protected void loadAccept(FileUploadField uploadField, Element element) {
        String accept = element.attributeValue("accept");
        if (StringUtils.isNotEmpty(accept)) {
            uploadField.setAccept(accept);
        }
    }
}