package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

/**
 * interface with the next methods in the chain
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryPopulateProperty<T> {
  IQueryWhere<T> where(String whereClause, Object... params);

  IQueryOrderBy<T> orderBy(String orderBy);
  
  IQueryLimitClause<T> limitClause(String orderBy);

  List<T> execute(JdbcTemplateMapper jdbcTemplateMapper);
}
