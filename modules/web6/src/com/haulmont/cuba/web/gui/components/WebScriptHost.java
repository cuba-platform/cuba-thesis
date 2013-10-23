/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.ValueProvider;
import com.haulmont.cuba.toolkit.gwt.client.utils.VScriptHost;
import com.haulmont.cuba.web.toolkit.ui.JavaScriptHost;
import com.vaadin.ui.ClientWidget;

/**
 * @author artamonov
 * @version $Id$
 */
public class WebScriptHost extends WebAbstractComponent<JavaScriptHost> {

    public WebScriptHost() {
        this.component = new JavaScriptHost();

        this.setWidth("0");
        this.setHeight("0");

        this.setVisible(true);
        this.setExpandable(false);
    }

    public ValueProvider getComponentParams() {
        return component.getValueProvider();
    }

    public void evaluateScript(String script) {
        this.component.evaluateScript(script);
    }

    public void viewDocument(String documentUrl) {
        this.component.viewDocument(documentUrl);
    }

    public void getResource(String resourceUrl) {
        this.component.getResource(resourceUrl);
    }
}