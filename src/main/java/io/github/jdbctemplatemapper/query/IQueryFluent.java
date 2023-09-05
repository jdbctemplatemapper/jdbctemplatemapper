package io.github.jdbctemplatemapper.query;

public interface IQueryFluent<T> extends IQueryType<T>, IQueryWhere<T>, IQueryOrderBy<T>, IQueryHasMany<T>, IQueryHasOne<T>,
IQueryJoinColumnOwningSide<T>, IQueryJoinColumnManySide<T>,IQueryThroughJoinTable<T>, IQueryThroughJoinColumns<T>, IQueryPopulateProperty<T>,
IQueryExecute<T>{
}

