package io.github.jdbctemplatemapper.core;

public interface IQueryJoinColumn<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
