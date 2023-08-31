package io.github.jdbctemplatemapper.querymerge;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryMergeExecute<T> {
    void execute(JdbcTemplateMapper jtm, List<T> list);
}
