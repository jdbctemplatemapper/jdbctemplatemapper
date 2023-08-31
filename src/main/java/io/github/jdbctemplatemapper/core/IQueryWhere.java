package io.github.jdbctemplatemapper.core;

public interface IQueryWhere<T> {
    IQueryOrderBy<T>orderBy(String orderBy);
    IQueryHasMany<T> hasMany(Class<?> clazz);
    IQueryHasOne<T> hasOne(Class<?> clazz);
}
