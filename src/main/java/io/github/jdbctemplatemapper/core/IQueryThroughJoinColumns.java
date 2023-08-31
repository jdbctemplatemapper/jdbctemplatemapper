package io.github.jdbctemplatemapper.core;

public interface IQueryThroughJoinColumns<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
