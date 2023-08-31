package io.github.jdbctemplatemapper.core;

import java.util.List;

public interface IQueryOrderBy<T> {
    IQueryHasMany<T> hasMany(Class<?> clazz);
    IQueryHasOne<T> hasOne(Class<?> clazz);
    List<T> execute(JdbcTemplateMapper jtm);
}
