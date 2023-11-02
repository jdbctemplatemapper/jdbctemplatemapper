package io.github.jdbctemplatemapper.query;

public interface IQueryJoinColumnTypeSide<T> {
  IQueryPopulateProperty<T> populateProperty(String propertyName);
}
