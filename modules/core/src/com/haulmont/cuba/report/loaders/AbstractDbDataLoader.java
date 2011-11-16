/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Eugeniy Degtyarjov
 * Created: 25.06.2010 17:24:27
 *
 * $Id$
 */
package com.haulmont.cuba.report.loaders;

import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.PersistenceProvider;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.report.Band;
import com.haulmont.cuba.report.DataSetType;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGmoney;
import org.postgresql.util.PGobject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractDbDataLoader implements DataLoader {
    private static final String QUERY_END = "%%END%%";
    private static final String OUTPUT_PARAMS_PATTERN = "as ([\\w|\\d|_]+\\b)[\\s]*[,|from|" + QUERY_END + "]";

    protected Map<String, Object> params = new HashMap<String, Object>();

    protected AbstractDbDataLoader(Map<String, Object> params) {
        this.params = params;
    }

    protected List<String> parseQueryOutputParametersNames(String query) {
        ArrayList<String> result = new ArrayList<String>();
        query += QUERY_END;
        Pattern namePattern = Pattern.compile(OUTPUT_PARAMS_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = namePattern.matcher(query);

        while (matcher.find()) {
            String group = matcher.group(matcher.groupCount());
            if (group != null)
                result.add(group.trim());
        }
        return result;
    }

    protected List<Map<String, Object>> fillOutputData(List resList, List<String> parametersNames) {
        List<Map<String, Object>> outputData = new ArrayList<Map<String, Object>>();

        for (Object _resultRecord : resList) {
            Map<String, Object> outputParameters = new HashMap<String, Object>();
            if (_resultRecord instanceof Object[]) {
                Object[] resultRecord = (Object[]) _resultRecord;
                for (Integer i = 0; i < resultRecord.length; i++) {
                    Object value = resultRecord[i];
                    if (isUUID(value))
                        value = UUID.fromString(value.toString());
                    outputParameters.put(parametersNames.get(i), value);
                }
            } else {
                outputParameters.put(parametersNames.get(0),
                        (isUUID(_resultRecord)) ? UUID.fromString(((PGobject) _resultRecord).getValue())
                        : _resultRecord);//todo: do we need to support another postgres objects?
            }
            outputData.add(outputParameters);
        }
        return outputData;
    }

    private boolean isUUID(Object value) {
        if (value instanceof PGobject) {
            if (!(value instanceof PGInterval) &&
                    !(value instanceof PGmoney))
                return true;
        }
        return false;
    }

    protected Query insertParameters(String query, Band parentBand, DataSetType dataSetType) {
        Map<String, Object> currentParams = new HashMap<String, Object>();
        if (params != null) currentParams.putAll(params);

        while (parentBand != null) {//adds parameters from parent bands hierarchy
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

        boolean inserted = values.size() > 0;
        EntityManager em = PersistenceProvider.getEntityManager();
        Query select = DataSetType.SQL.equals(dataSetType) ? em.createNativeQuery(query) : em.createQuery(query);
        if (inserted) {
            //insert parameters to their position
            i = 1;
            for (Object value : values) {
                select.setParameter(i++, value instanceof Entity ? ((Entity) value).getId() : value);
            }
        }
        return select;
    }

    protected void addParentBandParameters(Band parentBand, Map<String, Object> currentParams) {
        if (parentBand != null) {
            String parentBandName = parentBand.getName();

            for (Map.Entry<String, Object> entry : parentBand.getData().entrySet()) {
                currentParams.put(parentBandName + "." + entry.getKey(), entry.getValue());
            }
        }
    }
}