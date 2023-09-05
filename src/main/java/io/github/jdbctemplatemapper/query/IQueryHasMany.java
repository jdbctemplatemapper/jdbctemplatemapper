package io.github.jdbctemplatemapper.query;

public interface IQueryHasMany <T>{
    IQueryJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide);
    IQueryThroughJoinTable<T> throughJoinTable(String tableName);
}
