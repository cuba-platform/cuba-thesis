/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */
package com.haulmont.cuba.web.toolkit.ui;

import com.haulmont.cuba.web.toolkit.ui.client.fieldgroup.CubaFieldGroupState;
import com.vaadin.ui.Layout;

/**
 * @author gorodnov
 */
public class CubaFieldGroup extends CubaGroupBox {
    public CubaFieldGroup() {
        setLayout(new CubaFieldGroupLayout());
        setSizeUndefined();
    }

    public boolean isBorderVisible() {
        return getState(false).borderVisible;
    }

    public void setBorderVisible(boolean borderVisible) {
        if (getState().borderVisible != borderVisible) {
            getState().borderVisible = borderVisible;
            markAsDirty();
        }
    }

    @Override
    protected CubaFieldGroupState getState() {
        return (CubaFieldGroupState) super.getState();
    }

    @Override
    protected CubaFieldGroupState getState(boolean markAsDirty){
        return (CubaFieldGroupState) super.getState(markAsDirty);
    }

    public CubaFieldGroupLayout getLayout() {
        return (CubaFieldGroupLayout) super.getContent();
    }

    public void setLayout(Layout newLayout) {
        if (newLayout == null) {
            newLayout = new CubaFieldGroupLayout();
        }
        if (newLayout instanceof CubaFieldGroupLayout) {
            super.setContent(newLayout);
        } else {
            throw new IllegalArgumentException("FieldGroup supports only CubaFieldGroupLayout");
        }
    }
}