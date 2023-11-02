package io.github.jdbctemplatemapper.query;

public interface IQueryJoinColumnOneSide<T> {
  IQueryPopulateProperty<T> populateProperty(String propertyName);
}
