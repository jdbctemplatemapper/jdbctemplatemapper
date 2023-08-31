package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryWhere<T> {
    IQueryOrderBy<T>orderBy(String orderBy);
    IQueryHasMany<T> hasMany(Class<?> clazz);
    IQueryHasOne<T> hasOne(Class<?> clazz);
    List<T> execute(JdbcTemplateMapper jtm);
}
