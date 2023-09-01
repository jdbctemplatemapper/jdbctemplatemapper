package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeFluent<T> extends IQueryMergeType<T>, IQueryMergeHasMany<T>, IQueryMergeHasOne<T>,
IQueryMergeJoinColumn<T>, IQueryMergePopulateProperty<T>, IQueryMergeExecute<T> {
}
