/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 26.12.2008 9:53:52
 *
 * $Id$
 */
package com.haulmont.cuba.core.global;

import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link QueryTransformer} based on regular expressions
 */
public class QueryTransformerRegex extends QueryParserRegex implements QueryTransformer
{
    private String targetEntity;
    private StringBuffer buffer;
    private Set<String> addedParams;

    QueryTransformerRegex(String source, String targetEntity) {
        super(source);
        this.targetEntity = targetEntity;
        buffer = new StringBuffer(source);
        addedParams = new HashSet<String>();
    }

    public void addWhere(String where) {
        String alias = null;
        Matcher entityMatcher = ENTITY_PATTERN.matcher(buffer);
        while (entityMatcher.find()) {
            if (targetEntity.equals(entityMatcher.group(1))) {
                alias = entityMatcher.group(3);
                break;
            }
        }
        if (StringUtils.isBlank(alias))
            error("No alias for target entity " + targetEntity + " found");

        int insertPos = buffer.length();
        Matcher lastClauseMatcher = LAST_CLAUSE_PATTERN.matcher(buffer);
        if (lastClauseMatcher.find(entityMatcher.end()))
            insertPos = lastClauseMatcher.start() - 1;

        StringBuilder sb = new StringBuilder();
        Matcher whereMatcher = WHERE_PATTERN.matcher(buffer);
        if (whereMatcher.find(entityMatcher.end()))
            sb.append(" and ");
        else
            sb.append(" where ");

        addReplacingAlias(sb, where, alias);

        buffer.insert(insertPos, sb);

        // replace ALIAS_PLACEHOLDER
        int idx;
        while ((idx = buffer.indexOf(ALIAS_PLACEHOLDER)) >= 0) {
            buffer.replace(idx, idx + ALIAS_PLACEHOLDER.length(), alias);
        }

        Matcher paramMatcher = PARAM_PATTERN.matcher(where);
        while (paramMatcher.find()) {
            addedParams.add(paramMatcher.group(1));
        }
    }

    public void addWhereAsIs(String where) {
        boolean entityFound = false;
        Matcher entityMatcher = ENTITY_PATTERN.matcher(buffer);
        while (entityMatcher.find()) {
            if (targetEntity.equals(entityMatcher.group(1))) {
                entityFound = true;
                break;
            }
        }
        if (!entityFound)
            error("No target entity " + targetEntity + " specified");

        int insertPos = buffer.length();
        Matcher lastClauseMatcher = LAST_CLAUSE_PATTERN.matcher(buffer);
        if (lastClauseMatcher.find(entityMatcher.end()))
            insertPos = lastClauseMatcher.start() - 1;

        StringBuilder sb = new StringBuilder();
        Matcher whereMatcher = WHERE_PATTERN.matcher(buffer);
        if (whereMatcher.find(entityMatcher.end()))
            sb.append(" and ");
        else
            sb.append(" where ");

        sb.append(where);

        buffer.insert(insertPos, sb);

        Matcher paramMatcher = PARAM_PATTERN.matcher(where);
        while (paramMatcher.find()) {
            addedParams.add(paramMatcher.group(1));
        }
    }

    public void addJoinAsIs(String join) {
        boolean entityFound = false;
        Matcher entityMatcher = ENTITY_PATTERN.matcher(buffer);
        while (entityMatcher.find()) {
            if (targetEntity.equals(entityMatcher.group(1))) {
                entityFound = true;
                break;
            }
        }
        if (!entityFound)
            error("No target entity " + targetEntity + " specified");

        int insertPos = buffer.length();

        Matcher whereMatcher = WHERE_PATTERN.matcher(buffer);
        if (whereMatcher.find(entityMatcher.end())) {
            insertPos = whereMatcher.start() - 1;
        } else {
            Matcher lastClauseMatcher = LAST_CLAUSE_PATTERN.matcher(buffer);
            if (lastClauseMatcher.find(entityMatcher.end()))
                insertPos = lastClauseMatcher.start() - 1;
        }

        buffer.insert(insertPos, " ");
        insertPos++;

        buffer.insert(insertPos, join);

        Matcher paramMatcher = PARAM_PATTERN.matcher(join);
        while (paramMatcher.find()) {
            addedParams.add(paramMatcher.group(1));
        }
    }

    public void addJoinAndWhere(String join, String where) {
        String alias = null;
        Matcher entityMatcher = ENTITY_PATTERN.matcher(buffer);
        while (entityMatcher.find()) {
            if (targetEntity.equals(entityMatcher.group(1))) {
                alias = entityMatcher.group(3);
                break;
            }
        }
        if (StringUtils.isBlank(alias))
            error("No alias for target entity " + targetEntity + " found");

        int insertPos = buffer.length();

        Matcher whereMatcher = WHERE_PATTERN.matcher(buffer);
        if (whereMatcher.find(entityMatcher.end())) {
            insertPos = whereMatcher.start() - 1;
        } else {
            Matcher lastClauseMatcher = LAST_CLAUSE_PATTERN.matcher(buffer);
            if (lastClauseMatcher.find(entityMatcher.end()))
                insertPos = lastClauseMatcher.start() - 1;
        }

        if (!StringUtils.isBlank(join)) {
            buffer.insert(insertPos, " ");
            insertPos++;
            int joinLen = insertReplacingAlias(buffer, insertPos, join, alias);
            insertPos += joinLen;

            Matcher paramMatcher = PARAM_PATTERN.matcher(join);
            while (paramMatcher.find()) {
                addedParams.add(paramMatcher.group(1));
            }
        }
        if (!StringUtils.isBlank(where)) {
            StringBuilder sb = new StringBuilder();
            whereMatcher = WHERE_PATTERN.matcher(buffer);
            if (whereMatcher.find(entityMatcher.end()))
                sb.append(" and ");
            else
                sb.append(" where ");
            sb.append(where);

            insertPos = buffer.length();
            Matcher lastClauseMatcher = LAST_CLAUSE_PATTERN.matcher(buffer);
            if (lastClauseMatcher.find(entityMatcher.end()))
                insertPos = lastClauseMatcher.start() - 1;

            buffer.insert(insertPos, sb);

            Matcher paramMatcher = PARAM_PATTERN.matcher(where);
            while (paramMatcher.find()) {
                addedParams.add(paramMatcher.group(1));
            }
        }

        // replace ALIAS_PLACEHOLDER
        int idx;
        while ((idx = buffer.indexOf(ALIAS_PLACEHOLDER)) >= 0) {
            buffer.replace(idx, idx + ALIAS_PLACEHOLDER.length(), alias);
        }
    }

    public void mergeWhere(String query) {
        int startPos = 0;
        Matcher whereMatcher = WHERE_PATTERN.matcher(query);
        if (whereMatcher.find())
            startPos = whereMatcher.end() + 1;

        int endPos = query.length();
        Matcher lastClauseMatcher = LAST_CLAUSE_PATTERN.matcher(query);
        if (lastClauseMatcher.find())
            endPos = lastClauseMatcher.start();

        addWhere(query.substring(startPos, endPos));
    }

    private void addReplacingAlias(StringBuilder sb, String clause, String alias) {
        Matcher matcher = ALIAS_PATTERN.matcher(clause);
        int pos = 0;
        while (matcher.find()) {
            sb.append(clause.substring(pos, matcher.start(2)));
            pos = matcher.end(2);
            sb.append(alias);
        }
        sb.append(clause.substring(pos));
    }

    private int insertReplacingAlias(StringBuffer sb, int insertPos, String clause, String alias) {
        Matcher matcher = ALIAS_PATTERN.matcher(clause);
        int pos = 0;
        while (matcher.find()) {
            sb.insert(insertPos, clause.substring(pos, matcher.start(2)));
            insertPos += clause.substring(pos, matcher.start(2)).length();
            pos = matcher.end(2);
            sb.insert(insertPos, alias);
            insertPos += alias.length();
        }
        sb.insert(insertPos, clause.substring(pos));
        return alias.length() + clause.substring(pos).length();
    }

    public void replaceWithCount() {
        String alias = null;
        Matcher entityMatcher = ENTITY_PATTERN.matcher(buffer);
        while (entityMatcher.find()) {
            if (targetEntity.equals(entityMatcher.group(1))) {
                alias = entityMatcher.group(3);
                break;
            }
        }
        if (StringUtils.isBlank(alias))
            error("No alias for target entity " + targetEntity + " found");

        buffer.replace(0, entityMatcher.start(), "select count(" + alias + ") from ");
    }

    public void replaceOrderBy(String property, boolean desc) {
        String alias = null;
        Matcher entityMatcher = ENTITY_PATTERN.matcher(buffer);
        while (entityMatcher.find()) {
            if (targetEntity.equals(entityMatcher.group(1))) {
                alias = entityMatcher.group(3);
                break;
            }
        }
        if (StringUtils.isBlank(alias))
            error("No alias for target entity " + targetEntity + " found");

        String orderBy = alias + "." + property + (desc ? " desc" : "");

        Matcher matcher = ORDER_BY_PATTERN.matcher(buffer);
        if (matcher.find()) {
            buffer.replace(matcher.end(), buffer.length(), " " + orderBy);
        } else {
            buffer.append(" order by ").append(orderBy);
        }
    }

    public void reset() {
        buffer = new StringBuffer(source);
        addedParams.clear();
    }

    public String getResult() {
        return buffer.toString();
    }

    public Set<String> getAddedParams() {
        return Collections.unmodifiableSet(addedParams);
    }

    private void error(String message) {
        throw new RuntimeException(message + " [" + buffer.toString() + "]");
    }
}
