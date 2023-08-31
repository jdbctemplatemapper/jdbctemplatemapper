package io.github.jdbctemplatemapper.querymerge;

import io.github.jdbctemplatemapper.query.IQueryHasMany;
import io.github.jdbctemplatemapper.query.IQueryHasOne;

public interface IQueryMergeType<T> {
    IQueryMergeHasMany<T> hasMany(Class<?> clazz);
    IQueryMergeHasOne<T> hasOne(Class<?> clazz);
}
