package io.github.jdbctemplatemapper.query;

public interface IQueryJoinColumn<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
