/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 01.11.2008 13:23:09
 * $Id$
 */
package com.haulmont.cuba.core;

import com.haulmont.cuba.core.entity.BaseEntity;
import com.haulmont.cuba.core.sys.ManagedPersistenceProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.FieldMetaData;

import java.lang.annotation.Annotation;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class PersistenceProvider
{
    public static final int LOGIN_FIELD_LEN = 20;

    private static PersistenceProvider instance;

    public static final String PERSISTENCE_XML = "cuba.PersistenceXml";
    public static final String PERSISTENCE_UNIT = "cuba.PersistenceUnit";

    protected static final String DEFAULT_PERSISTENCE_XML = "META-INF/cuba-persistence.xml";
    protected static final String DEFAULT_PERSISTENCE_UNIT = "cuba";

    private static PersistenceProvider getInstance() {
        if (instance == null) {
            instance = new ManagedPersistenceProvider(Locator.getJndiContext());
        }
        return instance;
    }

    public static String getPersistenceXmlPath() {
        String xmlPath = System.getProperty(PERSISTENCE_XML);
        if (StringUtils.isBlank(xmlPath))
            xmlPath = DEFAULT_PERSISTENCE_XML;
        return xmlPath;
    }

    public static String getPersistenceUnitName() {
        String unitName = System.getProperty(PERSISTENCE_UNIT);
        if (StringUtils.isBlank(unitName))
            unitName = DEFAULT_PERSISTENCE_UNIT;
        return unitName;
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return getInstance().__getEntityManagerFactory();
    }

    /**
     * Returns a reference to EntityManager<p>
     * Inside JTA transaction returns existing or creates new transaction-bound EntityManager,
     * which will be closed on transaction commit/rollback.<br>
     * Outside a transaction always creates new EntityManager which must be closed explicitly after use.
     */
    public static EntityManager getEntityManager() {
        return getInstance().__getEntityManager();
    }

    public static String getEntityName(Class entityClass) {
        Annotation annotation = entityClass.getAnnotation(javax.persistence.Entity.class);
        if (annotation == null)
            throw new IllegalArgumentException("Class " + entityClass + " is not an entity");
        String name = ((javax.persistence.Entity) annotation).name();
        if (!StringUtils.isEmpty(name))
            return name;
        else
            return entityClass.getSimpleName();
    }

    protected abstract EntityManagerFactory __getEntityManagerFactory();

    protected abstract EntityManager __getEntityManager();
}
