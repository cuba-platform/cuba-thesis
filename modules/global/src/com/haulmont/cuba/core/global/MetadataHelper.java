/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 *
 * Author: chernov
 * Created: 22.11.2010 14:43:00
 *
 * $Id$
 */
package com.haulmont.cuba.core.global;

import com.haulmont.chile.core.model.*;
import com.haulmont.cuba.core.entity.BaseLongIdEntity;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.StandardEntity;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.persistence.CascadeType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

public abstract class MetadataHelper {
    public static Class getTypeClass(MetaProperty metaProperty) {
        if (metaProperty == null)
            throw new IllegalArgumentException("MetaProperty is null");

        final Range range = metaProperty.getRange();
        if (range.isDatatype()) {
            return range.asDatatype().getJavaClass();
        } else if (range.isClass()) {
            return range.asClass().getJavaClass();
        } else if (range.isEnum()) {
            return range.asEnumeration().getJavaClass();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static boolean isCascade(MetaProperty metaProperty) {
        OneToMany oneToMany = metaProperty.getAnnotatedElement().getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            final Collection<CascadeType> cascadeTypes = Arrays.asList(oneToMany.cascade());
            if (cascadeTypes.contains(CascadeType.ALL) ||
                    cascadeTypes.contains(CascadeType.MERGE))
            {
                return true;
            }
        }
        ManyToMany manyToMany = metaProperty.getAnnotatedElement().getAnnotation(ManyToMany.class);
        if (manyToMany != null && StringUtils.isBlank(manyToMany.mappedBy())) {
            return true;
        }
        return false;
    }

    public static boolean isSystem(MetaProperty metaProperty) {
        final MetaClass metaClass = metaProperty.getDomain();
        final Class javaClass = metaClass.getJavaClass();

        return BaseUuidEntity.class.equals(javaClass) ||
                StandardEntity.class.equals(javaClass) ||
                    BaseLongIdEntity.class.equals(javaClass);
    }

    public static Collection<MetaPropertyPath> getPropertyPaths(MetaClass metaClass) {
        List<MetaPropertyPath> res = new ArrayList<MetaPropertyPath>();
        for (MetaProperty metaProperty : metaClass.getProperties()) {
            res.add(new MetaPropertyPath(metaClass, metaProperty));
        }

        return res;
    }

    public static Collection<MetaPropertyPath> getViewPropertyPaths(View view, MetaClass metaClass) {
        List<MetaPropertyPath> propertyPaths = new ArrayList<MetaPropertyPath>(metaClass.getProperties().size());
        for (final MetaProperty metaProperty : metaClass.getProperties()) {
            final MetaPropertyPath metaPropertyPath = new MetaPropertyPath(metaClass, metaProperty);
            if (viewContainsProperty(view, metaPropertyPath)) {
                propertyPaths.add(metaPropertyPath);
            }
        }
        return propertyPaths;
    }

    public static Collection<MetaPropertyPath> toPropertyPaths(Collection<MetaProperty> properties) {
        List<MetaPropertyPath> res = new ArrayList<MetaPropertyPath>();
        for (MetaProperty metaProperty : properties) {
            res.add(new MetaPropertyPath(metaProperty.getDomain(), metaProperty));
        }

        return res;
    }

    /**
     * Visit all properties of an object graph starting from the specified instance
     */
    public static void walkProperties(Instance instance, PropertyVisitor visitor) {
        Session metadata = MetadataProvider.getSession();
        __walkProperties(instance, visitor, metadata, new HashSet<Instance>());
    }

    private static void __walkProperties(Instance instance, PropertyVisitor visitor,
                                         Session metadata, Set<Instance> visited)
    {
        if (visited.contains(instance))
            return;
        visited.add(instance);

        MetaClass metaClass = metadata.getClass(instance.getClass());
        if (metaClass == null)
            return;

        Collection<MetaProperty> properties = metaClass.getProperties();
        for (MetaProperty property : properties) {

            visitor.visit(instance, property);

            Object value = instance.getValue(property.getName());
            if (value != null && property.getRange().isClass()) {
                if (property.getRange().getCardinality().isMany()) {
                    Collection collection = (Collection) value;
                    for (Object o : collection) {
                        if (o instanceof Instance)
                            __walkProperties((Instance) o, visitor, metadata, visited);
                    }
                } else if (value instanceof Instance) {
                    __walkProperties((Instance) value, visitor, metadata, visited);
                }
            }
        }
    }

    public static boolean viewContainsProperty(View view, MetaPropertyPath propertyPath) {
        View currentView = view;
        for (MetaProperty metaProperty : propertyPath.get()) {
            if (currentView == null) return false;

            final ViewProperty property = currentView.getProperty(metaProperty.getName());
            if (property == null) return false;

            currentView = property.getView();
        }
        return true;
    }

    public static boolean isAnnotationPresent(Object object, String property,
                                              Class<? extends Annotation> annotationClass)
    {
        Field field;
        try {
            field = object.getClass().getDeclaredField(property);
            return field.isAnnotationPresent(annotationClass);
        } catch (NoSuchFieldException e) {
            Class superclass = object.getClass().getSuperclass();
            while (superclass != null) {
                try {
                    field = superclass.getDeclaredField(property);
                    return field.isAnnotationPresent(annotationClass);
                } catch (NoSuchFieldException e1) {
                    superclass = superclass.getSuperclass();
                }
            }
            throw new RuntimeException("Property not found: " + property);
        }
    }

    public static boolean isTransient(Object object, String property) {
        return isAnnotationPresent(object, property, Transient.class);
    }

    public static void deployViews(Element rootFrameElement) {
        final Element metadataContextElement = rootFrameElement.element("metadataContext");
        if (metadataContextElement != null) {
            @SuppressWarnings({"unchecked"})
            List<Element> fileElements = metadataContextElement.elements("deployViews");
            for (Element fileElement : fileElements) {
                final String resource = fileElement.attributeValue("name");
                InputStream resourceInputStream = ScriptingProvider.getResourceAsStream(resource);
                if (resourceInputStream == null) {
                    throw new RuntimeException("View resource not found: " + ((resource == null) ? "[null]" : resource));
                }
                MetadataProvider.getViewRepository().deployViews(resourceInputStream);
            }

            @SuppressWarnings({"unchecked"})
            List<Element> viewElements = metadataContextElement.elements("view");
            for (Element viewElement : viewElements) {
                MetadataProvider.getViewRepository().deployView(metadataContextElement, viewElement);
            }
        }
    }
}
