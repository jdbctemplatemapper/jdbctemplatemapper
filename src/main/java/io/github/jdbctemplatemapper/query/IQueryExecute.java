package io.github.jdbctemplatemapper.query;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import java.util.List;

/**
 * The execute interface.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryExecute<T> {
  List<T> execute(JdbcTemplateMapper jdbcTemplateMapper);
}
