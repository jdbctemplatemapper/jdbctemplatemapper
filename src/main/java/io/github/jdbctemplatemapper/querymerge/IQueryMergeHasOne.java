package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeHasOne<T>{
    IQueryMergeJoinColumn<T> joinColumn(String joinColumn);
}
