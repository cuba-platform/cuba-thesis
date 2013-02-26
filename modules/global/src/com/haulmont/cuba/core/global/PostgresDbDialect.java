/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.core.global;

/**
 * PostgreSQL dialect.
 *
 * @author krivopustov
 * @version $Id$
 */
public class PostgresDbDialect extends DbDialect implements SequenceSupport {

    public String sequenceExistsSql(String sequenceName) {
        return "select relname from pg_class where relkind = 'S' and relname = '"
                + sequenceName.toLowerCase() + "'";
    }

    public String createSequenceSql(String sequenceName, long startValue, long increment) {
        return "create sequence " + (sequenceName != null ? sequenceName.toLowerCase() : sequenceName)
                + " increment by " + increment + " start with " + startValue;
    }

    public String modifySequenceSql(String sequenceName, long startWith) {
        return "select setval('" + (sequenceName != null ? sequenceName.toLowerCase() : sequenceName) + "', " + startWith + ")";
    }

    public String getNextValueSql(String sequenceName) {
        return "select nextval('" + (sequenceName != null ? sequenceName.toLowerCase() : sequenceName) + "')";
    }

    public String getCurrentValueSql(String sequenceName) {
        return "select currval('" + (sequenceName != null ? sequenceName.toLowerCase() : sequenceName) + "')";
    }

    @Override
    public String getName() {
        return "postgres";
    }

    @Override
    public String getIdColumn() {
        return "id";
    }

    @Override
    public String getDeleteTsColumn() {
        return "delete_ts";
    }

    @Override
    public String getUniqueConstraintViolationPattern() {
        return "ERROR: duplicate key value violates unique constraint \"(.+)\"";
    }

    @Override
    public String getScriptSeparator() {
        return "^";
    }
}
