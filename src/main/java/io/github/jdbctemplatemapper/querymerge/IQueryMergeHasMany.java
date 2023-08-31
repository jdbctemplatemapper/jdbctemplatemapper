package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeHasMany <T>{
    IQueryMergeJoinColumn<T> joinColumn(String joinColumn);
}
