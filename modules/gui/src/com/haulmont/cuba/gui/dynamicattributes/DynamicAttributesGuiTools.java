/*
 * Copyright (c) 2008-2015 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.dynamicattributes;

import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributes;
import com.haulmont.cuba.core.app.dynamicattributes.DynamicAttributesUtils;
import com.haulmont.cuba.core.entity.BaseGenericIdEntity;
import com.haulmont.cuba.core.entity.Categorized;
import com.haulmont.cuba.core.entity.CategoryAttribute;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.TimeSource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.data.impl.DsListenerAdapter;

import javax.annotation.ManagedBean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * @author degtyarjov
 * @version $Id$
 */
@ManagedBean(DynamicAttributesGuiTools.NAME)
public class DynamicAttributesGuiTools {
    public static final String NAME = "cuba_DynamicAttributesGuiTools";

    @Inject
    protected DynamicAttributes dynamicAttributes;

    @Inject
    protected MetadataTools metadataTools;

    /**
     * Enforce the datasource to change modified status if dynamic attribute is changed
     */
    @SuppressWarnings("unchecked")
    public void listenDynamicAttributesChanges(final Datasource datasource) {
        if (datasource != null && datasource.getLoadDynamicAttributes()) {
            datasource.addListener(new DsListenerAdapter() {
                @Override
                public void valueChanged(Object source, String property, @Nullable Object prevValue, @Nullable Object value) {
                    if (DynamicAttributesUtils.isDynamicAttribute(property)) {
                        ((DatasourceImplementation) datasource).modified((Entity) source);
                    }
                }
            });
        }
    }

    /**
     * Get attributes which should be added automatically to the screen and component.
     * Based on visibility settings from category attribute editor.
     */
    public Set<CategoryAttribute> getAttributesToShowOnTheScreen(MetaClass metaClass, String screen, @Nullable String component) {
        Collection<CategoryAttribute> attributesForMetaClass =
                dynamicAttributes.getAttributesForMetaClass(metaClass);
        Set<CategoryAttribute> categoryAttributes = new LinkedHashSet<>();

        for (CategoryAttribute attribute : attributesForMetaClass) {
            if (attributeShouldBeShownOnTheScreen(screen, component, attribute)) {
                categoryAttributes.add(attribute);
            }
        }

        return categoryAttributes;
    }




    public void initDefaultAttributeValues(BaseGenericIdEntity item, MetaClass metaClass) {
        Preconditions.checkNotNullArgument(metaClass, "metaClass is null");
        Collection<CategoryAttribute> attributes =
                dynamicAttributes.getAttributesForMetaClass(metaClass);
        if (item.getDynamicAttributes() == null) {
            item.setDynamicAttributes(new HashMap<>());
        }
        Date currentTimestamp = AppBeans.get(TimeSource.NAME, TimeSource.class).currentTimestamp();
        boolean entityIsCategorized = item instanceof Categorized && ((Categorized) item).getCategory() != null;

        for (CategoryAttribute categoryAttribute : attributes) {
            String code = DynamicAttributesUtils.encodeAttributeCode(categoryAttribute.getCode());
            if (entityIsCategorized && !categoryAttribute.getCategory().equals(((Categorized) item).getCategory())) {
                item.setValue(code, null);//cleanup attributes from not dedicated category
                continue;
            }

            if (item.getValue(code) != null) {
                continue;//skip not null attributes
            }

            if (categoryAttribute.getDefaultValue() != null) {
                item.setValue(code, categoryAttribute.getDefaultValue());
            } else if (Boolean.TRUE.equals(categoryAttribute.getDefaultDateIsCurrent())) {
                item.setValue(code, currentTimestamp);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void listenCategoryChanges(Datasource ds) {
        ds.addListener(new DsListenerAdapter() {
            @Override
            public void valueChanged(Entity source, String property, @Nullable Object prevValue, @Nullable Object value) {
                if ("category".equals(property)) {
                    BaseGenericIdEntity entity = (BaseGenericIdEntity) source;
                    initDefaultAttributeValues(entity, entity.getMetaClass());
                }
            }
        });
    }

    protected boolean attributeShouldBeShownOnTheScreen(String screen, String component, CategoryAttribute attribute) {
        Set<String> targetScreensSet = attribute.targetScreensSet();
        return targetScreensSet.contains(screen) || targetScreensSet.contains(screen + "#" + component);
    }
}
