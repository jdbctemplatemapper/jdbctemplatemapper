package io.github.jdbctemplatemapper.query;

public interface IQueryJoinColumnOwningSide<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
