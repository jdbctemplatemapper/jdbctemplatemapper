package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeJoinColumnOwningSide<T> {
    IQueryMergePopulateProperty<T> populateProperty(String propertyName);
}
