package io.github.jdbctemplatemapper.querymerge;

import java.util.List;
import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

/**
 * interface with the next methods in the chain.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryMergePopulateProperty<T> {
  IQueryMergeOrderBy<T> orderBy(String orderBy);
  void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList);
}
