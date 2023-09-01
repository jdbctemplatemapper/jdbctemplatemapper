package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryOrderBy<T> {
    IQueryHasMany<T> hasMany(Class<?> relatedType);
    IQueryHasOne<T> hasOne(Class<?> relatedType);
    List<T> execute(JdbcTemplateMapper jtm);
}
