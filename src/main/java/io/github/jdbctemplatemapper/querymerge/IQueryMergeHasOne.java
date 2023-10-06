package io.github.jdbctemplatemapper.querymerge;

/**
 * interface with the next methods in the chain.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryMergeHasOne<T> {
  IQueryMergeJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide);
}
