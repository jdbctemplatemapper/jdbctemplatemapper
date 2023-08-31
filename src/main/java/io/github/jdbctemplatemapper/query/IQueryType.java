package io.github.jdbctemplatemapper.query;

import java.util.List;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

public interface IQueryType<T> {
    IQueryWhere<T> where(String wherClause, Object ...params); 
    IQueryOrderBy<T> orderBy(String orderBy);
    IQueryHasMany<T> hasMany(Class<?> clazz);
    IQueryHasOne<T> hasOne(Class<?> clazz);
    List<T> execute(JdbcTemplateMapper jtm);    
}
