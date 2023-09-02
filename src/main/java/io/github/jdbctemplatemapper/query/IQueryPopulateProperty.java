package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryPopulateProperty<T> {
  List<T> execute(JdbcTemplateMapper jdbcTemplateMapper);
}
