package io.github.jdbctemplatemapper.querymerge;
/**
 * interface with the next methods in the chain
 * 
 * @author ajoseph
 *
 * @param <T> the type
 */
public interface IQueryMergeType<T> {
    IQueryMergeHasMany<T> hasMany(Class<?> relatedType);
    IQueryMergeHasOne<T> hasOne(Class<?> relatedType);
}
