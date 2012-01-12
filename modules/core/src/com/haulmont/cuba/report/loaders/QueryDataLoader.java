/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.report.loaders;

import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.report.Band;
import com.haulmont.cuba.report.DataSetType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author artamonov
 * @version $Id$
 */
public abstract class QueryDataLoader extends AbstractDbDataLoader {

    protected QueryDataLoader(Map<String, Object> params) {
        super(params);
    }

    protected static class QueryPack {
        private String query;
        private Object[] params;

        private QueryPack(String query, Object[] params) {
            this.query = query;
            this.params = params;
        }

        public String getQuery() {
            return query;
        }

        public Object[] getParams() {
            return params;
        }
    }

    protected List<Map<String, Object>> fillOutputData(List resList, List<String> parametersNames) {
        List<Map<String, Object>> outputData = new ArrayList<Map<String, Object>>();

        for (Object _resultRecord : resList) {
            Map<String, Object> outputParameters = new HashMap<String, Object>();
            if (_resultRecord instanceof Object[]) {
                Object[] resultRecord = (Object[]) _resultRecord;
                for (Integer i = 0; i < resultRecord.length; i++) {
                    outputParameters.put(parametersNames.get(i), resultRecord[i]);
                }
            } else {
                outputParameters.put(parametersNames.get(0), _resultRecord);
            }
            outputData.add(outputParameters);
        }
        return outputData;
    }

    protected QueryPack prepareQuery(String query, Band parentBand) {
        Map<String, Object> currentParams = new HashMap<String, Object>();
        if (params != null) currentParams.putAll(params);

        //adds parameters from parent bands hierarchy
        while (parentBand != null) {
            addParentBandParameters(parentBand, currentParams);
            parentBand = parentBand.getParentBand();
        }

        ArrayList<Object> values = new ArrayList<Object>();
        int i = 1;
        for (Map.Entry<String, Object> entry : currentParams.entrySet()) {
            //replaces ${alias} marks with ? and remembers their positions
            String alias = "${" + entry.getKey() + "}";
            String regexp = "\\$\\{" + entry.getKey() + "\\}";
            //todo: another regexp to remove parameter
            String deleteRegexp = "(?i)(and)?(or)? ?[\\w|\\d|\\.|\\_]+ ?(=|>=|<=|like) ?\\$\\{" + entry.getKey() + "\\}";

            if (entry.getValue() == null) {
                query = query.replaceAll(deleteRegexp, "");
            } else if (query.contains(alias)) {
                values.add(entry.getValue());
                query = query.replaceAll(regexp, "?" + i++);
            }
        }

        query = query.trim();
        if (query.endsWith("where")) query = query.replace("where", "");

        return new QueryPack(query, values.toArray());
    }

    protected Query insertParameters(String query, Band parentBand, DataSetType dataSetType) {
        QueryPack pack = prepareQuery(query, parentBand);

        boolean inserted = pack.params.length > 0;
        EntityManager em = PersistenceProvider.getEntityManager();
        Query select = DataSetType.SQL.equals(dataSetType) ? em.createNativeQuery(query) : em.createQuery(query);
        if (inserted) {
            //insert parameters to their position
            int i = 1;
            for (Object value : pack.params) {
                select.setParameter(i++, value instanceof Entity ? ((Entity) value).getId() : value);
            }
        }
        return select;
    }
}