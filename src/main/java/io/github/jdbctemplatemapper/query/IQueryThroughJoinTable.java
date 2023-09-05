package io.github.jdbctemplatemapper.query;
/**
 * interface with the next methods in the chain
 * 
 * @author ajoseph
 *
 * @param <T>
 */
public interface IQueryThroughJoinTable<T> {
    IQueryThroughJoinColumns<T> throughJoinColumns(String ownerJoinColumn, String relatedJoinColumn);
}
