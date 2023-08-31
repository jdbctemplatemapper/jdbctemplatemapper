package io.github.jdbctemplatemapper.core;

public interface IQueryHasOne<T>{
    IQueryJoinColumn<T> joinColumn(String joinColumn);
}
