package io.github.jdbctemplatemapper.querymerge;

public interface IQueryMergeThroughJoinTable<T> {
    IQueryMergeThroughJoinColumns<T> throughJoinColumns(String mainJoinColumn, String relatedJoinColumn);
}
