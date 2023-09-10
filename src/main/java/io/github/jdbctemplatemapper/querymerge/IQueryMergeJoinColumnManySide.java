package io.github.jdbctemplatemapper.querymerge;

/**
 * interface with the next methods in the chain
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryMergeJoinColumnManySide<T> {
  IQueryMergePopulateProperty<T> populateProperty(String propertyName);
}