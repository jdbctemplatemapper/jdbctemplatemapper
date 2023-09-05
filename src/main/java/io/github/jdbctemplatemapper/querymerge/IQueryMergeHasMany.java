package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeHasMany <T>{
    IQueryMergeJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide);
}
