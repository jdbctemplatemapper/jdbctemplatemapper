package io.github.jdbctemplatemapper.query;
/**
 * interface with the next methods in the chain
 * 
 * @author ajoseph
 *
 * @param <T>
 */
public interface IQueryJoinColumnManySide<T> {
    IQueryPopulateProperty<T> populateProperty(String propertyName);
}
