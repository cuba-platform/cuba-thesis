/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components.presentations.actions;

import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.presentations.Presentations;
import com.haulmont.cuba.security.entity.Presentation;

/**
 * @author shishov
 * @version $Id$
 */

public class EditPresentationAction extends AbstractEditPresentationAction {

    public EditPresentationAction(Table table) {
        super(table, "PresentationsPopup.edit");
    }

    @Override
    public void actionPerform(Component component) {
        tableImpl.hidePresentationsPopup();

        Presentations presentations = table.getPresentations();
        Presentation current = presentations.getCurrent();
        openEditor(current);
    }
}
