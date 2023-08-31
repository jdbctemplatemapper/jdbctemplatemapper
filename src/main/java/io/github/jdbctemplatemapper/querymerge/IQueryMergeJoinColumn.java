package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeJoinColumn<T> {
    IQueryMergePopulateProperty<T> populateProperty(String propertyName);
}
