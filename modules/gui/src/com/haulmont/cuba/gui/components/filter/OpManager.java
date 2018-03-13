/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.chile.core.model.MetaProperty;

import java.util.EnumSet;

/**
 * @author gorbunkov
 * @version $Id$
 */
public interface OpManager {
    String NAME = "cuba_OpManager";

    EnumSet<Op> availableOps(Class javaClass);

    EnumSet<Op> availableOps(MetaProperty metaProperty);
}
