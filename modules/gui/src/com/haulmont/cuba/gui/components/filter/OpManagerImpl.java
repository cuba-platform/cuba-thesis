/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.components.filter;

import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.app.PersistenceManagerService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;

import static com.haulmont.cuba.gui.components.filter.Op.*;

/**
 * @author gorbunkov
 * @version $Id$
 */
@ManagedBean(OpManager.NAME)
public class OpManagerImpl implements OpManager {

    @Inject
    protected MetadataTools metadataTools;

    public EnumSet<Op> availableOps(Class javaClass) {
        if (String.class.equals(javaClass))
            return EnumSet.of(EQUAL, IN, NOT_IN, NOT_EQUAL, CONTAINS, DOES_NOT_CONTAIN, NOT_EMPTY, STARTS_WITH, ENDS_WITH);

        else if (Date.class.isAssignableFrom(javaClass)
                || Number.class.isAssignableFrom(javaClass))
            return EnumSet.of(EQUAL, IN, NOT_IN, NOT_EQUAL, GREATER, GREATER_OR_EQUAL, LESSER, LESSER_OR_EQUAL, NOT_EMPTY);

        else if (Boolean.class.equals(javaClass))
            return EnumSet.of(EQUAL, NOT_EQUAL, NOT_EMPTY);

        else if (UUID.class.equals(javaClass)
                || Enum.class.isAssignableFrom(javaClass)
                || Entity.class.isAssignableFrom(javaClass))
            return EnumSet.of(EQUAL, IN, NOT_IN, NOT_EQUAL, NOT_EMPTY);

        else
            throw new UnsupportedOperationException("Unsupported java class: " + javaClass);
    }

    @Override
    public EnumSet<Op> availableOps(MetaProperty metaProperty) {
        Class javaClass = metaProperty.getJavaType();
        if (String.class.equals(javaClass) && metadataTools.isLob(metaProperty)) {
            PersistenceManagerService persistenceManagerService = AppBeans.get(PersistenceManagerService.class);
            if (!persistenceManagerService.supportsLobSortingAndFiltering()) {
                return EnumSet.of(CONTAINS, DOES_NOT_CONTAIN, NOT_EMPTY, STARTS_WITH, ENDS_WITH);
            }
        }
        return availableOps(javaClass);
    }
}
