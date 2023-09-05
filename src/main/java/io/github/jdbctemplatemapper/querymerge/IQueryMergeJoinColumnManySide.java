package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeJoinColumnManySide<T> {
    IQueryMergePopulateProperty<T> populateProperty(String propertyName);
}
