/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.ClientType;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
@ManagedBean(Security.NAME)
public class SecurityImpl implements Security {

    @Inject
    protected UserSessionSource userSessionSource;

    @Override
    public boolean isScreenPermitted(ClientType clientType, String windowAlias) {
        return userSessionSource.getUserSession().isScreenPermitted(windowAlias);
    }

    @Override
    public boolean isEntityOpPermitted(MetaClass metaClass, EntityOp entityOp) {
        return userSessionSource.getUserSession().isEntityOpPermitted(metaClass, entityOp);
    }

    @Override
    public boolean isEntityAttrPermitted(MetaClass metaClass, String property, EntityAttrAccess access) {
        return userSessionSource.getUserSession().isEntityAttrPermitted(metaClass, property, access);
    }

    @Override
    public boolean isEntityAttrModificationPermitted(MetaProperty metaProperty) {
        return isEntityAttrModificationPermitted(metaProperty.getDomain(), metaProperty);
    }

    @Override
    public boolean isEntityAttrModificationPermitted(MetaClass metaClass, MetaProperty metaProperty) {
        MetaClass domain = metaProperty.getDomain();
        MetaClass mClass = domain;

        if (domain.getDescendants() != null && domain.getDescendants().contains(metaClass))
            mClass = metaClass;

        return (isEntityOpPermitted(mClass, EntityOp.CREATE) || isEntityOpPermitted(mClass, EntityOp.UPDATE))
                && isEntityAttrPermitted(mClass, metaProperty.getName(), EntityAttrAccess.MODIFY);
    }

    @Override
    public boolean isSpecificPermitted(String name) {
        return userSessionSource.getUserSession().isSpecificPermitted(name);
    }
}
