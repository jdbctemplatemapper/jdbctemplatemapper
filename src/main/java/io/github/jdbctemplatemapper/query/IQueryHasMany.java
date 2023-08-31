package io.github.jdbctemplatemapper.query;

public interface IQueryHasMany <T>{
    IQueryJoinColumn<T> joinColumn(String joinColumn);
    IQueryThroughJoinTable<T> throughJoinTable(String tableName);
}
