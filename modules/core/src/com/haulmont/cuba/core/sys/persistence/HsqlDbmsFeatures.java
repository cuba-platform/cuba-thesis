/*
 * Copyright (c) 2008-2014 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.persistence;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author krivopustov
 * @version $Id$
 */
@SuppressWarnings("UnusedDeclaration")
public class HsqlDbmsFeatures implements DbmsFeatures {

    @Override
    public Map<String, String> getJpaParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("openjpa.jdbc.DBDictionary",
                "com.haulmont.cuba.core.sys.persistence.CubaHSQLDictionary(RequiresCastForComparisons=true,SupportsSelectForUpdate=true)");
        params.put("openjpa.jdbc.MappingDefaults",
                "FieldStrategies='java.util.UUID=com.haulmont.cuba.core.sys.persistence.UuidStringValueHandler'");
        return params;
    }

    @Override
    public String getIdColumn() {
        return "ID";
    }

    @Override
    public String getDeleteTsColumn() {
        return "DELETE_TS";
    }

    @Override
    public String getTimeStampType() {
        return "timestamp";
    }

    @Nullable
    @Override
    public String getUuidTypeClassName() {
        return null;
    }

    @Nullable
    @Override
    public String getTransactionTimeoutStatement() {
        return null;
    }

    @Override
    public String getUniqueConstraintViolationPattern() {
        return "Violation of unique index (.+): duplicate value\\(s\\) for column\\(s\\) (.+) in statement";
    }

    @Override
    public boolean isNullsLastSorting() {
        return false;
    }

    @Override
    public boolean isSchemaByUser() {
        return false;
    }

    @Override
    public boolean supportsLobSortingAndFiltering() {
        return true;
    }
}
