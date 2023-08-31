package io.github.jdbctemplatemapper.query;

public interface IQueryThroughJoinColumns<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
