package io.github.jdbctemplatemapper.query;

/**
 * interface with the next methods in the chain
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryJoinColumnManySide<T> {
  IQueryPopulateProperty<T> populateProperty(String propertyName);
}
