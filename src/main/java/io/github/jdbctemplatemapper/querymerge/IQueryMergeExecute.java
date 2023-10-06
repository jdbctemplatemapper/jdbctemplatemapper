package io.github.jdbctemplatemapper.querymerge;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import java.util.List;

/**
 * The execute interface.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryMergeExecute<T> {
  void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList);
}
