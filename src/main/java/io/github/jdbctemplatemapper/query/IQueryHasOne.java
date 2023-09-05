package io.github.jdbctemplatemapper.query;

public interface IQueryHasOne<T>{
    IQueryJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide);
}
