package io.github.jdbctemplatemapper.querymerge;

/**
 * interface with the next methods in the chain
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryMergeThroughJoinTable<T> {
  IQueryMergeThroughJoinColumns<T> throughJoinColumns(String ownerJoinColumn, String relatedJoinColumn);
}