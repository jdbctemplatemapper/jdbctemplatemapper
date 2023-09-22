package io.github.jdbctemplatemapper.querycount;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

/**
 * interface with the next methods in the chain
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryCountWhere<T> {
  IQueryCountWhere<T> where(String whereClause, Object... params);
  Integer execute(JdbcTemplateMapper jdbcTemplateMapper);
}
