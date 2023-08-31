package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryExecute<T> {
     List<T> execute(JdbcTemplateMapper jtm);
}
