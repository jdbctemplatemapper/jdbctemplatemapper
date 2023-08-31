package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryOrderBy<T> {
    IQueryHasMany<T> hasMany(Class<?> clazz);
    IQueryHasOne<T> hasOne(Class<?> clazz);
    List<T> execute(JdbcTemplateMapper jtm);
}
