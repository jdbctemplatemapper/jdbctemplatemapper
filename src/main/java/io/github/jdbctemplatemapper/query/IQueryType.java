package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
/**
 * interface with the next methods in the chain
 * 
 * @author ajoseph
 *
 * @param <T>
 */
public interface IQueryType<T> {
    IQueryWhere<T> where(String wherClause, Object ...params); 
    IQueryOrderBy<T> orderBy(String orderBy);
    IQueryHasMany<T> hasMany(Class<?> relatedType);
    IQueryHasOne<T> hasOne(Class<?> relatedType);
    List<T> execute(JdbcTemplateMapper jdbcTemplateMapper);    
}
