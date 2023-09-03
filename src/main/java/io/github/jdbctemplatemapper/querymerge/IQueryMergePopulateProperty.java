package io.github.jdbctemplatemapper.querymerge;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryMergePopulateProperty<T> {
    void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList);
}