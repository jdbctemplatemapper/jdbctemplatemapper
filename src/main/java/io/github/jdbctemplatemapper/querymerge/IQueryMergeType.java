package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeType<T> {
    IQueryMergeHasMany<T> hasMany(Class<?> clazz);
    IQueryMergeHasOne<T> hasOne(Class<?> clazz);
}
