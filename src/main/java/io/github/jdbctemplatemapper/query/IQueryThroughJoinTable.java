package io.github.jdbctemplatemapper.query;

public interface IQueryThroughJoinTable<T> {
    IQueryThroughJoinColumns<T> throughJoinColumns(String ownerJoinColumn, String relatedJoinColumn);
}
