package io.github.jdbctemplatemapper.query;

public interface IQueryThroughJoinTable<T> {
    IQueryThroughJoinColumns<T> throughJoinColumns(String mainJoinColumn, String relatedJoinColumn);
}
