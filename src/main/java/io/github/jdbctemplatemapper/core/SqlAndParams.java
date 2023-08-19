package io.github.jdbctemplatemapper.core;

import java.util.Set;

import org.springframework.util.ObjectUtils;

/**
 * This holds the sql and the sql params needed to issue and update.
 *
 * @author ajoseph
 */
class SqlAndParams {
    private String sql; // the sql string
    private Set<String> params; // the parameters for the sql

    public SqlAndParams(String sql, Set<String> params) {
        if (ObjectUtils.isEmpty(sql) || params == null) {
            throw new IllegalArgumentException("sql and params cannot be null");
        }
        this.sql = sql;
        this.params = params;
    }

    public String getSql() {
        return sql;
    }

    public Set<String> getParams() {
        return params;
    }
}
