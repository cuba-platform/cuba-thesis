/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.web.app.core.entityinspector;

import com.haulmont.cuba.gui.app.core.entityinspector.EntityInspectorBrowse;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.toolkit.ui.CubaEnhancedTable;

public class EntityInspectorBrowseCompanion implements EntityInspectorBrowse.Companion {

    @Override
    public void setHorizontalScrollEnabled(Table table, boolean enabled) {
        // Horizontal scroll is always enabled by default for Web
    }

    @Override
    public void setTextSelectionEnabled(Table table, boolean enabled) {
        CubaEnhancedTable enhancedTable = WebComponentsHelper.unwrap(table);
        enhancedTable.setTextSelectionEnabled(enabled);
    }
}