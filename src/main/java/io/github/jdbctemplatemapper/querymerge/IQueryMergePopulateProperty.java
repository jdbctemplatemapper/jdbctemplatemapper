package io.github.jdbctemplatemapper.querymerge;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
/**
 * interface with the next methods in the chain
 * 
 * @author ajoseph
 *
 * @param <T>
 */
public interface IQueryMergePopulateProperty<T> {
    void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList);
}
