package io.github.jdbctemplatemapper.core;

public interface IQueryThroughJoinTable<T> {
    IQueryThroughJoinColumns<T> throughJoinColumns(String mainJoinColumn, String relatedJoinColumn);
}
