package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
/**
 * interface with the next methods in the chain
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryType<T> {
  IQueryHasMany<T> hasMany(Class<?> relatedType);

  IQueryHasOne<T> hasOne(Class<?> relatedType);

  IQueryWhere<T> where(String whereClause, Object... params);

  IQueryOrderBy<T> orderBy(String orderBy);
  
  IQueryLimitClause<T> limitClause(String orderBy);

  List<T> execute(JdbcTemplateMapper jdbcTemplateMapper);
}
