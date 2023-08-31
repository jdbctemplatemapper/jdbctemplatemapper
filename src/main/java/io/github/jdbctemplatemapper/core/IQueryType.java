package io.github.jdbctemplatemapper.core;

import java.util.List;

public interface IQueryType<T> {
    IQueryWhere<T> where(String wherClause, Object ...params); 
    IQueryOrderBy<T> orderBy(String orderBy);
    IQueryHasMany<T> hasMany(Class<?> clazz);
    IQueryHasOne<T> hasOne(Class<?> clazz);
    List<T> execute(JdbcTemplateMapper jtm);    
}
