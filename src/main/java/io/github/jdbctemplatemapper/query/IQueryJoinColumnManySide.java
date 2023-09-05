package io.github.jdbctemplatemapper.query;

public interface IQueryJoinColumnManySide<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
