package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeHasOne<T>{
    IQueryMergeJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide);
}
