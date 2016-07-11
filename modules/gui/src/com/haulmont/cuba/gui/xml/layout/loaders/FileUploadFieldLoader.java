/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.components.BoxLayout;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Component.HasDropZone.DropZone;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.IFrame;
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
        FileUploadField component = factory.createComponent(element.getName());

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

        loadDropZone(component, element);

        assignFrame(component);
    }

    protected void loadAccept(FileUploadField uploadField, Element element) {
        String accept = element.attributeValue("accept");
        if (StringUtils.isNotEmpty(accept)) {
            uploadField.setAccept(accept);
        }
    }

    protected void loadDropZone(final FileUploadField uploadField, Element element) {
        final String dropZoneId = element.attributeValue("dropZone");
        if (StringUtils.isNotEmpty(dropZoneId)) {
            context.addPostInitTask(new PostInitTask() {
                @Override
                public void execute(Context context, IFrame window) {
                    Component dropZone = window.getComponent(dropZoneId);
                    if (dropZone instanceof BoxLayout) {
                        uploadField.setDropZone(new DropZone((BoxLayout) dropZone));
                    }
                }
            });
        }

        String dropZonePrompt = element.attributeValue("dropZonePrompt");
        if (StringUtils.isNotEmpty(dropZonePrompt)) {
            uploadField.setDropZonePrompt(loadResourceString(dropZonePrompt));
        }
    }
}