package io.github.jdbctemplatemapper.querycount;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

/**
 * The execute interface.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryCountExecute<T> {
  Integer execute(JdbcTemplateMapper jdbcTemplateMapper);
}
