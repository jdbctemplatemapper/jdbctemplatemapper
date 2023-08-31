package io.github.jdbctemplatemapper.query;

public interface IQueryHasOne<T>{
    IQueryJoinColumn<T> joinColumn(String joinColumn);
}
