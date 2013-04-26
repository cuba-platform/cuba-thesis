/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Component;
import com.vaadin.ui.RichTextArea;

/**
 * @author abramov
 * @version $Id$
 */
public class WebRichTextArea
    extends
        WebAbstractField<RichTextArea>
    implements
        com.haulmont.cuba.gui.components.RichTextArea, Component.Wrapper {

    public WebRichTextArea() {
        component = new RichTextArea();
        attachListener(component);

        component.setImmediate(true);
        component.setNullRepresentation("");
        component.setInvalidAllowed(false);
        component.setInvalidCommitted(true);
    }
}