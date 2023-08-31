package io.github.jdbctemplatemapper.core;

public interface IQueryHasMany <T>{
    IQueryJoinColumn<T> joinColumn(String joinColumn);
    IQueryThroughJoinTable<T> throughJoinTable(String tableName);
}
