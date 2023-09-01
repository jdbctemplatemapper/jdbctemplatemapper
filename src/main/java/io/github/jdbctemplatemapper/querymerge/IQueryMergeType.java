package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeType<T> {
    IQueryMergeHasMany<T> hasMany(Class<?> relatedType);
    IQueryMergeHasOne<T> hasOne(Class<?> relatedType);
}
