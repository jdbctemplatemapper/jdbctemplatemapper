package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeThroughJoinColumns<T> {
    IQueryMergePopulateProperty<T> populateProperty(String propertyName);
}
