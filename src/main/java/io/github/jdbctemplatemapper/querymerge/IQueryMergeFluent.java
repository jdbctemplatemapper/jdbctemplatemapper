package io.github.jdbctemplatemapper.querymerge;
/**
 * The fluent style interface for QueryMerge
 * 
 * @author ajoseph
 *
 * @param <T> the type
 */
public interface IQueryMergeFluent<T> extends IQueryMergeType<T>, IQueryMergeHasMany<T>, IQueryMergeHasOne<T>,
IQueryMergeJoinColumnOwningSide<T>, IQueryMergeJoinColumnManySide<T>, IQueryMergePopulateProperty<T>, IQueryMergeExecute<T> {
}
