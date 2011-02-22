/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 22.05.2009 12:54:58
 *
 * $Id$
 */
package com.haulmont.cuba.core.app;

import com.haulmont.bali.db.QueryRunner;
import com.haulmont.bali.db.ResultSetHandler;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.*;
import com.haulmont.cuba.core.entity.EntityStatistics;
import com.haulmont.cuba.core.entity.SoftDelete;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.MetadataProvider;
import com.haulmont.cuba.core.global.QueryParser;
import com.haulmont.cuba.core.global.QueryTransformerFactory;
import com.haulmont.cuba.core.sys.DbUpdater;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.ManagedBean;
import javax.inject.Inject;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * PersistentConfig MBean implementation.
 * <p>
 * This MBean is intended for reading on startup and caching database metadata information,
 * mainly to support soft delete functionality and entity statistics
 */
@ManagedBean(PersistenceManagerAPI.NAME)
public class PersistenceManager extends ManagementBean implements PersistenceManagerMBean, PersistenceManagerAPI
{
    private static Log log = LogFactory.getLog(PersistenceManager.class);
    private boolean metadataLoaded;
    private Set<String> softDeleteTables = new HashSet<String>();

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile Map<String, EntityStatistics> statisticsCache;

    @Inject
    private DbUpdater dbUpdater;

    private PersistenceConfig config;

    @Inject
    public void setConfigProvider(ConfigProvider configProvider) {
        config = configProvider.doGetConfig(PersistenceConfig.class);
    }

    private void initDbMetadata() {
        log.info("Initializing DB metadata");
        DataSource datasource = Locator.getDataSource();
        Connection conn = null;
        try {
            conn = datasource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, null, PersistenceProvider.getDbDialect().getDeleteTsColumn());
            lock.writeLock().lock();
            try {
                softDeleteTables.clear();
                while (rs.next()) {
                    String table = rs.getString("TABLE_NAME");
                    softDeleteTables.add(table);
                }
            } finally {
                lock.writeLock().unlock();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
            }
        }
        metadataLoaded = true;
    }

    public int getDefaultLazyCollectionThreshold() {
        return config.getDefaultLazyCollectionThreshold();
    }

    public void setDefaultLazyCollectionThreshold(int value) {
        config.setDefaultLazyCollectionThreshold(value);
    }

    public int getDefaultFetchUI() {
        return config.getDefaultFetchUI();
    }

    public void setDefaultFetchUI(int value) {
        config.setDefaultFetchUI(value);
    }

    public int getDefaultMaxFetchUI() {
        return config.getDefaultMaxFetchUI();
    }

    public void setDefaultMaxFetchUI(int value) {
        config.setDefaultMaxFetchUI(value);
    }

    public String printSoftDeleteTables() {
        lock.readLock().lock();
        if (!metadataLoaded) {
            lock.readLock().unlock();
            initDbMetadata();
            lock.readLock().lock();
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (String table : softDeleteTables) {
                sb.append(table).append("\n");
            }
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isSoftDeleteFor(String table) {
        lock.readLock().lock();
        if (!metadataLoaded) {
            lock.readLock().unlock();
            initDbMetadata();
            lock.readLock().lock();
        }
        try {
            return softDeleteTables.contains(table);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean useLazyCollection(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es == null || es.getInstanceCount() == null)
            return false;
        else {
            int threshold = es.getLazyCollectionThreshold() != null ? es.getLazyCollectionThreshold() : config.getDefaultLazyCollectionThreshold();
            return es.getInstanceCount() > threshold;
        }
    }

    public int getFetchUI(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es != null && es.getFetchUI() != null)
            return es.getFetchUI();
        else
            return config.getDefaultFetchUI();
    }

    public int getMaxFetchUI(String entityName) {
        EntityStatistics es = getStatisticsCache().get(entityName);
        if (es != null && es.getMaxFetchUI() != null)
            return es.getMaxFetchUI();
        else
            return config.getDefaultMaxFetchUI();
    }

    public String updateDatabase() {
        try {
            dbUpdater.updateDatabase();
            return "Updated";
        } catch (Throwable e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public String findUpdateDatabaseScripts() {
        try {
            List<String> list = dbUpdater.findUpdateDatabaseScripts();
            StrBuilder sb = new StrBuilder();
            sb.appendWithSeparators(list, "\n");
            return sb.toString();
        } catch (Throwable e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public String jpqlLoadList(String queryString) {
        try {
            Transaction tx = Locator.createTransaction();
            try {
                EntityManager em = PersistenceProvider.getEntityManager();
                Query query = em.createQuery(queryString);
                QueryParser parser = QueryTransformerFactory.createParser(queryString);
                Set<String> paramNames = parser.getParamNames();
                for (String paramName : paramNames) {
                    SecurityProvider.setQueryParam(query, paramName);
                }
                List resultList = query.getResultList();
                tx.commit();

                StrBuilder sb = new StrBuilder();
                sb.appendWithSeparators(resultList, "\n");
                return sb.toString();
            } finally {
                tx.end();
            }
        } catch (Throwable e) {
            log.error("jpqlLoadList error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public String jpqlExecuteUpdate(String queryString, boolean softDeletion) {
        try {
            login();
            Transaction tx = Locator.createTransaction();
            try {
                EntityManager em = PersistenceProvider.getEntityManager();
                em.setSoftDeletion(softDeletion);
                Query query = em.createQuery(queryString);
                int count = query.executeUpdate();
                tx.commit();

                return "Done: " + count + " entities affected, softDeletion=" + softDeletion;
            } finally {
                tx.end();
            }
        } catch (Throwable e) {
            log.error("jpqlExecuteUpdate error", e);
            return ExceptionUtils.getStackTrace(e);
        } finally {
            logout();
        }
    }

    private Map<String, EntityStatistics> getStatisticsCache() {
        if (statisticsCache == null) {
            synchronized (this) {
                if (statisticsCache == null) {
                    statisticsCache = new ConcurrentHashMap<String, EntityStatistics>();
                    internalLoadStatisticsCache();
                }
            }
        }
        return statisticsCache;
    }

    private void internalLoadStatisticsCache() {
        log.info("Loading statistics cache");
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();
            Query q = em.createQuery("select s from core$EntityStatistics s");
            List<EntityStatistics> list = q.getResultList();
            for (EntityStatistics es : list) {
                statisticsCache.put(es.getName(), es);
            }
            tx.commit();
        } finally {
            tx.end();
        }
    }

    public String loadStatisticsCache() {
        try {
            getStatisticsCache();
            internalLoadStatisticsCache();
            return "Done";
        } catch (Exception e) {
            log.error("loadStatisticsCache error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public String refreshStatistics(String entityName) {
        try {
            log.info("Refreshing statistics");
            login();
            if (StringUtils.isBlank(entityName)) {
                for (MetaClass metaClass : MetadataProvider.getSession().getClasses()) {
                    Class javaClass = metaClass.getJavaClass();
                    Table annotation = (Table) javaClass.getAnnotation(Table.class);
                    if (annotation != null) {
                        refreshStatisticsForEntity(metaClass.getName());
                    }
                }
            } else {
                MetaClass metaClass = MetadataProvider.getSession().getClass(entityName);
                if (metaClass == null)
                    return "MetaClass not found: " + entityName;
                Class javaClass = metaClass.getJavaClass();
                Table annotation = (Table) javaClass.getAnnotation(Table.class);
                if (annotation != null) {
                    refreshStatisticsForEntity(metaClass.getName());
                }
            }
            return "Done";
        } catch (Exception e) {
            log.error("refreshStatistics error", e);
            return ExceptionUtils.getStackTrace(e);
        } finally {
            logout();
        }
    }

    public String showStatistics(String entityName) {
        try {
            if (StringUtils.isBlank(entityName)) {
                List<String> names = new ArrayList(getStatisticsCache().keySet());
                Collections.sort(names);

                StringBuilder sb = new StringBuilder();
                for (String name : names) {
                    sb.append(getStatisticsCache().get(name)).append("\n");
                }
                return sb.toString();
            } else {
                EntityStatistics es = getStatisticsCache().get(entityName);
                return es == null ? "No such entity" : es.toString();
            }
        } catch (Exception e) {
            log.error("showStatistics error", e);
            return ExceptionUtils.getStackTrace(e);
        }
    }

    private void refreshStatisticsForEntity(String name) {
        log.debug("Refreshing statistics for entity " + name);
        Transaction tx = Locator.createTransaction();
        try {
            EntityManager em = PersistenceProvider.getEntityManager();

            Query q = em.createQuery("select count(e) from " + name + " e");
            Long count = (Long) q.getSingleResult();

            q = em.createQuery("select s from core$EntityStatistics s where s.name = ?1");
            q.setParameter(1, name);
            List<EntityStatistics> list = q.getResultList();

            EntityStatistics es;
            if (list.isEmpty()) {
                es = new EntityStatistics();
                es.setName(name);
                es.setInstanceCount(count);
                em.persist(es);
            } else {
                es = list.get(0);
                es.setInstanceCount(count);
            }
            getStatisticsCache().put(name, es);

            tx.commit();
        } finally {
            tx.end();
        }
    }
}
