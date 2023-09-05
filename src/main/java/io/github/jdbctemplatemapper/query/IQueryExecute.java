package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
/**
 * The execute interface
 * 
 * @author ajoseph
 *
 * @param <T>
 */
public interface IQueryExecute<T> {
     List<T> execute(JdbcTemplateMapper jdbcTemplateMapper);
}
