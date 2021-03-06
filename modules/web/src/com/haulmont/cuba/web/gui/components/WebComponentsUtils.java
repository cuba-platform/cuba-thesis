/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.LookupField;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.ComboBox;

/**
 * @author artamonov
 * @version $Id$
 */
public class WebComponentsUtils {

    public static void allowHtmlContent(Label label) {
        com.vaadin.ui.Label vLabel = WebComponentsHelper.unwrap(label);
        vLabel.setContentMode(ContentMode.HTML);
    }

    public static void disallowHtmlContent(Label label) {
        com.vaadin.ui.Label vLabel = WebComponentsHelper.unwrap(label);
        vLabel.setContentMode(ContentMode.TEXT);
    }

    public static void allowNullSelection(LookupField lookupField) {
        ComboBox vCombobox = WebComponentsHelper.unwrap(lookupField);
        vCombobox.setNullSelectionAllowed(true);
    }

    public static void disallowNullSelection(LookupField lookupField) {
        ComboBox vCombobox = WebComponentsHelper.unwrap(lookupField);
        vCombobox.setNullSelectionAllowed(false);
    }
}