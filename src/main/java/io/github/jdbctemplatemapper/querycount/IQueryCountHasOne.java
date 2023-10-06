package io.github.jdbctemplatemapper.querycount;

/**
 * interface with the next methods in the chain.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryCountHasOne<T> {
  IQueryCountJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide);
}
