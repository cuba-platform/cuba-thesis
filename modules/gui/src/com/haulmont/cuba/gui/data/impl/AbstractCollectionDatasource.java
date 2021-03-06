/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.gui.data.impl;

import com.google.common.base.Joiner;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.chile.core.model.utils.InstanceUtils;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.FrameContext;
import com.haulmont.cuba.gui.components.IFrame;
import com.haulmont.cuba.gui.data.*;
import com.haulmont.cuba.gui.filter.QueryFilter;
import com.haulmont.cuba.gui.xml.ParameterInfo;
import com.haulmont.cuba.gui.xml.ParametersHelper;
import com.haulmont.cuba.security.global.UserSession;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @param <T> type of entity
 * @param <K> type of entity ID
 * @author abramov
 * @version $Id$
 */
public abstract class AbstractCollectionDatasource<T extends Entity<K>, K>
        extends DatasourceImpl<T>
        implements CollectionDatasource<T, K>,
        CollectionDatasource.SupportsRefreshMode<T, K> {

    protected String query;
    protected QueryFilter filter;
    protected int maxResults;
    protected ParameterInfo[] queryParameters;
    protected boolean softDeletion = true;
    protected ComponentValueListener componentValueListener;
    protected boolean refreshOnComponentValueChange;
    protected Sortable.SortInfo<MetaPropertyPath>[] sortInfos;
    protected Map<String, Object> savedParameters;
    protected Throwable dataLoadError;
    protected boolean listenersSuspended;
    protected CollectionDatasourceListener.Operation lastCollectionChangeOperation;
    protected List<Entity> lastCollectionChangeItems;
    protected RefreshMode refreshMode = RefreshMode.ALWAYS;
    protected UserSession userSession = AppBeans.<UserSessionSource>get(UserSessionSource.NAME).getUserSession();

    @Override
    public T getItemNN(K id) {
        T it = getItem(id);
        if (it != null)
            return it;
        else
            throw new NullPointerException("Item with id=" + id + " is not found in datasource " + this.id);
    }

    @Override
    public void setItem(T item) {
        if (State.VALID.equals(state)) {
            Object prevItem = this.item;

            if (prevItem != item) {
                if (item != null) {
                    final MetaClass aClass = item.getMetaClass();
                    if (!aClass.equals(this.metaClass) && !this.metaClass.getDescendants().contains(aClass)) {
                        throw new DevelopmentException(String.format("Invalid item metaClass '%s'", aClass));
                    }
                }
                this.item = item;

                fireItemChanged(prevItem);
            }
        }
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public LoadContext getCompiledLoadContext() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public QueryFilter getQueryFilter() {
        return filter;
    }

    @Override
    public void setQuery(String query) {
        setQuery(query, null);
    }

    @Override
    public void setQueryFilter(QueryFilter filter) {
        String query = getQuery();
        if (query == null) {
            throw new DevelopmentException("Unable to use filter on '" + getId() + "' datasource without query");
        }
        setQuery(query, filter);
    }

    @Override
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public Map<String, Object> getLastRefreshParameters() {
        return savedParameters == null ?
                Collections.<String, Object>emptyMap() :
                Collections.unmodifiableMap(savedParameters);
    }

    @Override
    public boolean getRefreshOnComponentValueChange() {
        return refreshOnComponentValueChange;
    }

    @Override
    public void setRefreshOnComponentValueChange(boolean refresh) {
        refreshOnComponentValueChange = refresh;
    }

    @Override
    public void setQuery(String query, QueryFilter filter) {
        if (ObjectUtils.equals(this.query, query) && ObjectUtils.equals(this.filter, filter))
            return;

        this.query = query;
        this.filter = filter;

        queryParameters = ParametersHelper.parseQuery(query, filter);

        for (ParameterInfo info : queryParameters) {
            final ParameterInfo.Type type = info.getType();
            if (ParameterInfo.Type.DATASOURCE.equals(type)) {
                final String path = info.getPath();

                final String[] strings = path.split("\\.");
                String source = strings[0];

                final String property;
                if (strings.length > 1) {
                    final List<String> list = Arrays.asList(strings);
                    final List<String> valuePath = list.subList(1, list.size());
                    property = InstanceUtils.formatValuePath(valuePath.toArray(new String[valuePath.size()]));
                } else {
                    property = null;
                }

                final Datasource ds = dsContext.get(source);
                if (ds != null) {
                    dsContext.registerDependency(this, ds, property);
                } else {
                    ((DsContextImplementation) dsContext).addLazyTask(new DsContextImplementation.LazyTask() {
                        @Override
                        public void execute(DsContext context) {
                            final String[] strings = path.split("\\.");
                            String source = strings[0];

                            final Datasource ds = dsContext.get(source);
                            if (ds != null) {
                                dsContext.registerDependency(AbstractCollectionDatasource.this, ds, property);
                            }
                        }
                    });
                }
            }
        }
    }

    protected Map<String, Object> getQueryParameters(Map<String, Object> params) {
        final Map<String, Object> map = new HashMap<>();
        for (ParameterInfo info : queryParameters) {
            String name = info.getFlatName();

            final String path = info.getPath();
            final String[] elements = path.split("\\.");
            switch (info.getType()) {
                case DATASOURCE: {
                    String dsName = elements[0];
                    final Datasource datasource = dsContext.get(dsName);
                    if (datasource == null) {
                        throw new DevelopmentException("Datasource '" + dsName + "' not found in dsContext",
                                "datasource", dsName);
                    }

                    if (datasource.getState() == State.VALID) {
                        final Entity item = datasource.getItem();
                        if (elements.length > 1) {
                            String[] valuePath = (String[]) ArrayUtils.subarray(elements, 1, elements.length);
                            String propertyName = InstanceUtils.formatValuePath(valuePath);
                            Object value = InstanceUtils.getValueEx(item, propertyName);
                            map.put(name, value);
                        } else {
                            map.put(name, item);
                        }
                    } else {
                        map.put(name, null);
                    }

                    break;
                }
                case PARAM: {
                    Object value;
                    if (dsContext.getFrameContext() == null) {
                        value = null;
                    } else {
                        Map<String, Object> windowParams = dsContext.getFrameContext().getParams();
                        value = windowParams.get(path);
                        if (value == null && elements.length > 1) {
                            Instance instance = (Instance) windowParams.get(elements[0]);
                            if (instance != null) {
                                String[] valuePath = (String[]) ArrayUtils.subarray(elements, 1, elements.length);
                                String propertyName = InstanceUtils.formatValuePath(valuePath);
                                value = InstanceUtils.getValueEx(instance, propertyName);
                            }
                        }
                    }
                    if (value instanceof String && info.isCaseInsensitive()) {
                        value = makeCaseInsensitive((String) value);
                    }
                    map.put(name, value);
                    break;
                }
                case COMPONENT: {
                    Object value = null;
                    if (dsContext.getFrameContext() != null) {
                        value = dsContext.getFrameContext().getValue(path);
                        if (value instanceof String && info.isCaseInsensitive()) {
                            value = makeCaseInsensitive((String) value);
                        }
                        if (java.sql.Date.class.equals(info.getJavaClass()) && value != null && value instanceof Date) {
                            value = new java.sql.Date(((Date) value).getTime());
                        }
                        if (refreshOnComponentValueChange) {
                            if (componentValueListener == null)
                                componentValueListener = new ComponentValueListener();
                            try {
                                dsContext.getFrameContext().addValueListener(path, componentValueListener);
                            } catch (Exception e) {
                                log.error("Unable to add value listener: " + e);
                            }
                        }
                    }
                    map.put(name, value);
                    break;
                }
                case SESSION: {
                    Object value;
                    value = userSession.getAttribute(path);
                    if (value instanceof String && info.isCaseInsensitive()) {
                        value = makeCaseInsensitive((String) value);
                    }
                    map.put(name, value);
                    break;
                }
                case CUSTOM: {
                    Object value = params.get(info.getPath());
                    if (value == null) {
                        //a case when a query contains a parameter like :custom$city.country.id and we passed
                        //just "city" parameter to the datasource refresh() method
                        String[] pathElements = info.getPath().split("\\.");
                        if (pathElements.length > 1) {
                            Object entity = params.get(pathElements[0]);
                            if (entity != null && entity instanceof Instance) {
                                value = InstanceUtils.getValueEx((Instance) entity, Arrays.copyOfRange(pathElements, 1, pathElements.length));
                            }
                        }
                    }
                    if (value instanceof String && info.isCaseInsensitive()) {
                        value = makeCaseInsensitive((String) value);
                    }
                    map.put(name, value);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException("Unsupported parameter type: " + info.getType());
                }
            }
        }

        return map;
    }

    private String makeCaseInsensitive(String value) {
        StringBuilder sb = new StringBuilder();
        sb.append(ParametersHelper.CASE_INSENSITIVE_MARKER);
        if (!value.startsWith("%"))
            sb.append("%");
        sb.append(value);
        if (!value.endsWith("%"))
            sb.append("%");
        return sb.toString();
    }

    protected String getJPQLQuery(Map<String, Object> parameterValues) {
        String query;
        if (filter == null)
            query = this.query;
        else
            query = filter.processQuery(this.query, parameterValues);

        for (ParameterInfo info : queryParameters) {
            final String paramName = info.getName();
            final String jpaParamName = info.getFlatName();

            Pattern p = Pattern.compile(paramName.replace("$", "\\$") + "([^\\.]|$)"); // not ending with "."
            Matcher m = p.matcher(query);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, jpaParamName + "$1");
            }
            m.appendTail(sb);
            query = sb.toString();

            Object value = parameterValues.get(paramName);
            if (value != null) {
                parameterValues.put(jpaParamName, value);
            }
        }
        query = query.replace(":" + ParametersHelper.CASE_INSENSITIVE_MARKER, ":");

        query = TemplateHelper.processTemplate(query, parameterValues);

        return query;
    }

    protected void fireCollectionChanged(CollectionDatasourceListener.Operation operation, List<Entity> items) {
        if (listenersSuspended) {
            lastCollectionChangeOperation = operation;
            lastCollectionChangeItems = items;
            return;
        }
        for (DatasourceListener dsListener : new ArrayList<>(dsListeners)) {
            if (dsListener instanceof CollectionDatasourceListener) {
                ((CollectionDatasourceListener) dsListener).collectionChanged(this, operation, items);
            }
        }
    }

    protected Map<String, Object> getTemplateParams(Map<String, Object> customParams) {

        Map<String, Object> templateParams = new HashMap<>();

        String compPerfix = ParameterInfo.Type.COMPONENT.getPrefix() + "$";
        for (ParameterInfo info : queryParameters) {
            if (ParameterInfo.Type.COMPONENT.equals(info.getType())) {
                Object value = dsContext.getFrameContext() == null ?
                        null : dsContext.getFrameContext().getValue(info.getPath());
                templateParams.put(compPerfix + info.getPath(), value);
            }
        }

        String customPerfix = ParameterInfo.Type.CUSTOM.getPrefix() + "$";
        for (Map.Entry<String, Object> entry : customParams.entrySet()) {
            templateParams.put(customPerfix + entry.getKey(), entry.getValue());
        }

        if (dsContext != null) {
            FrameContext windowContext = dsContext.getFrameContext();
            if (windowContext != null) {
                String paramPerfix = ParameterInfo.Type.PARAM.getPrefix() + "$";
                for (Map.Entry<String, Object> entry : windowContext.getParams().entrySet()) {
                    templateParams.put(paramPerfix + entry.getKey(), entry.getValue());
                }
            }
        }

        String sessionPrefix = ParameterInfo.Type.SESSION.getPrefix() + "$";
        templateParams.put(sessionPrefix + "userId", userSession.getCurrentOrSubstitutedUser().getId());
        templateParams.put(sessionPrefix + "userLogin", userSession.getCurrentOrSubstitutedUser().getLoginLowerCase());
        for (String name : userSession.getAttributeNames()) {
            templateParams.put(sessionPrefix + name, userSession.getAttribute(name));
        }

        return templateParams;
    }

    @Override
    public void suspendListeners() {
        listenersSuspended = true;
    }

    @Override
    public void resumeListeners() {
        listenersSuspended = false;

        if (lastCollectionChangeOperation != null) {
            fireCollectionChanged(lastCollectionChangeOperation,
                    lastCollectionChangeItems != null ? lastCollectionChangeItems : Collections.<Entity>emptyList());
        }

        lastCollectionChangeOperation = null;
        lastCollectionChangeItems = null;
    }

    @Override
    public boolean isSoftDeletion() {
        return softDeletion;
    }

    @Override
    public void setSoftDeletion(boolean softDeletion) {
        this.softDeletion = softDeletion;
    }

    @Override
    public void commit() {
        if (!allowCommit)
            return;

        if (CommitMode.DATASTORE.equals(getCommitMode())) {
            final DataSupplier supplier = getDataSupplier();
            Set<Entity> commitInstances = new HashSet<>();
            Set<Entity> deleteInstances = new HashSet<>();

            commitInstances.addAll(itemToCreate);
            commitInstances.addAll(itemToUpdate);
            deleteInstances.addAll(itemToDelete);

            CommitContext context =
                    new CommitContext(commitInstances, deleteInstances);
            for (Entity entity : commitInstances) {
                context.getViews().put(entity, getView());
            }
            for (Entity entity : deleteInstances) {
                context.getViews().put(entity, getView());
            }

            final Set<Entity> committed = supplier.commit(context);

            committed(committed);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected Comparator<T> createEntityComparator() {
        // In case of generated column the sortInfos can actually contain string as a column identifier.
        if (sortInfos[0].getPropertyPath() instanceof MetaPropertyPath) {
            final MetaPropertyPath propertyPath = sortInfos[0].getPropertyPath();
            final boolean asc = Sortable.Order.ASC.equals(sortInfos[0].getOrder());
            return new EntityComparator<>(propertyPath, asc);
        } else {
            // If we can not sort the datasource, just return the empty comparator.
            return new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return 0;
                }
            };
        }
    }

    protected LoadContext.Query createLoadContextQuery(LoadContext context, Map<String, Object> params) {
        LoadContext.Query q;
        if (query != null && queryParameters != null) {
            Map<String, Object> parameters = getQueryParameters(params);
            for (ParameterInfo info : queryParameters) {
                if (ParameterInfo.Type.DATASOURCE.equals(info.getType())) {
                    Object value = parameters.get(info.getFlatName());
                    if (value == null) {
                        String[] pathElements = info.getPath().split("\\.");
                        if (pathElements.length == 1) {
                            //nothing selected in 'master' datasource, so return null here to clear the 'detail' datasource
                            return null;
                        } else {
                            //The parameter with null value is the path to the datasource item property,
                            //e.g. :ds$User.group.id.
                            //If the 'master' datasource item is not null then do not clear the 'detail' datasource,
                            //a null query parameter value should be processed
                            String dsName = pathElements[0];
                            final Datasource datasource = dsContext.get(dsName);
                            if (datasource == null) {
                                throw new DevelopmentException("Datasource '" + dsName + "' not found in dsContext",
                                        "datasource", dsName);
                            }
                            if (datasource.getState() != State.VALID || datasource.getItem() == null) return null;
                        }
                    }
                }
            }

            String queryString = getJPQLQuery(getTemplateParams(params));
            q = context.setQueryString(queryString);
            // Pass only parameters used in the resulting query
            QueryParser parser = QueryTransformerFactory.createParser(queryString);
            Set<String> paramNames = parser.getParamNames();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (paramNames.contains(entry.getKey()))
                    q.setParameter(entry.getKey(), entry.getValue());
            }
        } else {
            Collection<MetaProperty> properties = metadata.getTools().getNamePatternProperties(metaClass);
            if (!properties.isEmpty()) {
                StringBuilder orderBy = new StringBuilder();
                for (MetaProperty metaProperty : properties) {
                    if (metaProperty != null
                            && metaProperty.getAnnotatedElement().
                            getAnnotation(com.haulmont.chile.core.annotations.MetaProperty.class) == null)
                        orderBy.append("e.").append(metaProperty.getName()).append(", ");
                }
                if (orderBy.length() > 0) {
                    orderBy.delete(orderBy.length() - 2, orderBy.length());
                    orderBy.insert(0, " order by ");
                }
                q = context.setQueryString("select e from " + metaClass.getName() + " e" + orderBy.toString());
            } else
                q = context.setQueryString("select e from " + metaClass.getName() + " e");
        }
        return q;
    }

    /**
     * Return number of rows for the current query set in the datasource.
     * <p>This method transforms the current query to "select count()" query with the same conditions, and sends it
     * to the middleware.</p>
     *
     * @return number of rows. In case of error returns 0 and sets {@link #dataLoadError} field to the exception object
     */
    public int getCount() {
        LoadContext context = new LoadContext(metaClass);
        LoadContext.Query q = createLoadContextQuery(context, savedParameters == null ? Collections.<String, Object>emptyMap() : savedParameters);
        context.setSoftDeletion(isSoftDeletion());
        if (q == null)
            return 0;

        QueryTransformer transformer = QueryTransformerFactory.createTransformer(q.getQueryString(), metaClass.getName());
        transformer.replaceWithCount();
        String jpqlQuery = transformer.getResult();
        q.setQueryString(jpqlQuery);

        prepareLoadContext(context);

        dataLoadError = null;
        try {
            List res = dataSupplier.loadList(context);
            return res.isEmpty() ? 0 : ((Long) res.get(0)).intValue();
        } catch (Throwable e) {
            dataLoadError = e;
        }
        return 0;
    }

    protected String getLoggingTag(String prefix) {
        String windowId = "";
        if (dsContext != null) {
            FrameContext windowContext = dsContext.getFrameContext();
            if (windowContext != null) {
                IFrame frame = windowContext.getFrame();
                if (frame != null) {
                    windowId = ComponentsHelper.getFullFrameId(windowContext.getFrame());
                }
            }
        }
        String tag = prefix + " " + id;
        if (StringUtils.isNotBlank(windowId))
            tag = windowId + "@" + id;
        return tag;
    }

    protected void prepareLoadContext(LoadContext context) {
    }

    protected void checkDataLoadError() {
        if (dataLoadError != null) {
            if (dataLoadError instanceof RuntimeException)
                throw (RuntimeException) dataLoadError;
            else
                throw new RuntimeException(dataLoadError);
        }
    }

    protected void setSortDirection(LoadContext.Query q) {
        boolean asc = Sortable.Order.ASC.equals(sortInfos[0].getOrder());
        MetaPropertyPath propertyPath = sortInfos[0].getPropertyPath();
        String[] sortProperties = null;

        if (metadata.getTools().isPersistent(propertyPath)) {
            sortProperties = getSortPropertiesForPersistentAttribute(propertyPath);
        } else {
            // a non-persistent attribute
            List<String> relProperties = metadata.getTools().getRelatedProperties(propertyPath.getMetaProperty());
            if (!relProperties.isEmpty()) {
                List<String> sortPropertiesList = new ArrayList<>(relProperties.size());
                for (String relProp : relProperties) {
                    String[] ppCopy = Arrays.copyOf(propertyPath.getPath(), propertyPath.getPath().length);
                    ppCopy[ppCopy.length - 1] = relProp;

                    MetaPropertyPath relPropertyPath = propertyPath.getMetaProperties()[0].getDomain().getPropertyPath(Joiner.on(".").join(ppCopy));
                    String[] sortPropertiesForRelProperty = getSortPropertiesForPersistentAttribute(relPropertyPath);
                    Collections.addAll(sortPropertiesList, sortPropertiesForRelProperty);
                }
                sortProperties = sortPropertiesList.toArray(new String[sortPropertiesList.size()]);
            }
        }

        if (sortProperties != null) {
            QueryTransformer transformer = QueryTransformerFactory.createTransformer(q.getQueryString(), metaClass.getName());
            transformer.replaceOrderBy(!asc, sortProperties);
            String jpqlQuery = transformer.getResult();
            q.setQueryString(jpqlQuery);
        }
    }

    protected String[] getSortPropertiesForPersistentAttribute(MetaPropertyPath propertyPath) {
        String[] sortProperties = null;
        if (!propertyPath.getMetaProperty().getRange().isClass()) {
            // a scalar persistent attribute
            sortProperties = new String[1];
            sortProperties[0] = propertyPath.toString();
        } else {
            // a reference attribute
            MetaClass metaClass = propertyPath.getMetaProperty().getRange().asClass();
            if (!propertyPath.getMetaProperty().getRange().getCardinality().isMany()) {
                InstanceUtils.NamePatternRec rec = InstanceUtils.parseNamePattern(metaClass);
                if (rec != null) {
                    sortProperties = new String[rec.fields.length];
                    for (int i = 0; i < rec.fields.length; i++) {
                        sortProperties[i] = propertyPath.toString() + "." + rec.fields[i];
                    }
                } else {
                    sortProperties = new String[1];
                    sortProperties[0] = propertyPath.toString();
                }
            }
        }
        return sortProperties;
    }

    @Override
    public RefreshMode getRefreshMode() {
        return refreshMode;
    }

    @Override
    public void setRefreshMode(RefreshMode refreshMode) {
        this.refreshMode = refreshMode;
    }

    private class ComponentValueListener implements ValueListener {
        @Override
        public void valueChanged(Object source, String property, Object prevValue, Object value) {
            refresh();
        }
    }
}