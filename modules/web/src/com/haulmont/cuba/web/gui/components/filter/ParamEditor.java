/*
 * Copyright (c) 2009 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 16.10.2009 14:36:33
 *
 * $Id$
 */
package com.haulmont.cuba.web.gui.components.filter;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

public class ParamEditor extends CustomComponent implements Condition.Listener {

    private Condition condition;
    private HorizontalLayout layout;
    private Component field;

    public ParamEditor(final Condition condition, boolean showOperation) {
        this.condition = condition;

        layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setSizeFull();
        setCompositionRoot(layout);

        if (condition.getParam() != null) {
            if (showOperation) {
                Label opLab = new Label(condition.getOperationCaption());
                layout.addComponent(opLab);
            }
            field = condition.getParam().createEditComponent();
            field.setSizeFull();
            layout.addComponent(field);
        }

        condition.addListener(this);
    }

    public void paramChanged() {
        if (field != null) {
            layout.removeComponent(field);
        }
        field = condition.getParam().createEditComponent();
        field.setSizeFull();
        layout.addComponent(field);
    }

    public void captionChanged() {
    }
}
