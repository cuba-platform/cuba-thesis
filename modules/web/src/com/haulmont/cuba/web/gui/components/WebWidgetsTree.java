/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.gui.components;

import com.haulmont.cuba.gui.components.Action;
import com.haulmont.cuba.gui.components.CaptionMode;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.WidgetsTree;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.haulmont.cuba.web.gui.data.HierarchicalDsWrapper;
import com.haulmont.cuba.web.toolkit.ui.CubaWidgetsTree;
import org.apache.commons.lang.StringUtils;

/**
 * @author gorodnov
 * @version $Id$
 */
public class WebWidgetsTree extends WebAbstractList<CubaWidgetsTree> implements WidgetsTree {

    protected String hierarchyProperty;

    public WebWidgetsTree() {
        component = new CubaWidgetsTree();
        component.setSelectable(false);
        component.setImmediate(true); 
    }

    @Override
    protected ContextMenuButton createContextMenuButton() {
        return new ContextMenuButton() {
            @Override
            protected void performAction(Action action) {
                action.actionPerform(WebWidgetsTree.this);
            }

            @Override
            protected void beforeActionPerformed() {
                WebWidgetsTree.this.component.hideContextMenuPopup();
            }
        };
    }

    @Override
    public String getHierarchyProperty() {
        return hierarchyProperty;
    }

    @Override
    public void setDatasource(HierarchicalDatasource datasource) {
        this.datasource = datasource;
        this.hierarchyProperty = datasource.getHierarchyPropertyName();

        HierarchicalDsWrapper wrapper = new HierarchicalDsWrapper(datasource);
        component.setContainerDataSource(wrapper);

        for (Action action : getActions()) {
            action.refreshState();
        }
    }

    @Override
    public void setItemClickAction(Action action) {
        //do nothing
    }

    @Override
    public Action getItemClickAction() {
        return null;
    }

    @Override
    public void collapseTree() {
        component.collapseAll();
    }

    @Override
    public void expandTree() {
        component.expandAll();
    }

    @Override
    public void collapse(Object itemId) {
        component.collapseItemRecursively(itemId);
    }

    @Override
    public void expand(Object itemId) {
        component.expandItemWithParents(itemId);
    }

    @Override
    public void expandUpTo(int level) {
        component.expandUpTo(level);
    }

    @Override
    public boolean isExpanded(Object itemId) {
        return component.isExpanded(itemId);
    }

    @Override
    public void setWidgetBuilder(final WidgetBuilder widgetBuilder) {
        if (widgetBuilder != null) {
            component.setWidgetBuilder(new CubaWidgetsTree.WidgetBuilder() {
                @Override
                public com.vaadin.ui.Component buildWidget(
                        CubaWidgetsTree source,
                        Object itemId,
                        boolean leaf
                ) {
                    Component widget = widgetBuilder.build((HierarchicalDatasource) datasource, itemId, leaf);
                    return WebComponentsHelper.getComposition(widget);
                }
            });
        } else {
            component.setWidgetBuilder(null);
        }
    }

    @Override
    public CaptionMode getCaptionMode() {
        return null;
    }

    @Override
    public void setCaptionMode(CaptionMode captionMode) {
        //do nothing
    }

    @Override
    public String getCaptionProperty() {
        return null;
    }

    @Override
    public void setCaptionProperty(String captionProperty) {
        //do nothing
    }

    @Override
    public boolean isEditable() {
        return !component.isReadOnly();
    }

    @Override
    public void setEditable(boolean editable) {
        component.setReadOnly(!editable);
    }

    @Override
    protected String getAlternativeDebugId() {
        if (id != null) {
            return id;
        }
        if (datasource != null && StringUtils.isNotEmpty(datasource.getId())) {
            return getClass().getSimpleName() + "_" + datasource.getId();
        }

        return getClass().getSimpleName();
    }
}