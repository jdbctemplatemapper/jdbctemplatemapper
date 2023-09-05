package io.github.jdbctemplatemapper.query;
/**
 * The fluent style interface for Query
 * 
 * @author ajoseph
 *
 * @param <T> the type
 */
public interface IQueryFluent<T> extends IQueryType<T>, IQueryWhere<T>, IQueryOrderBy<T>, IQueryHasMany<T>, IQueryHasOne<T>,
IQueryJoinColumnOwningSide<T>, IQueryJoinColumnManySide<T>,IQueryThroughJoinTable<T>, IQueryThroughJoinColumns<T>, IQueryPopulateProperty<T>,
IQueryExecute<T>{
}

