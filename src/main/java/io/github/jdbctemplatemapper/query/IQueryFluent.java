package io.github.jdbctemplatemapper.query;

public interface IQueryFluent<T> extends IQueryType<T>, IQueryWhere<T>, IQueryOrderBy<T>, IQueryHasMany<T>, IQueryHasOne<T>,
IQueryJoinColumn<T>, IQueryThroughJoinTable<T>, IQueryThroughJoinColumns<T>, IQueryPopulateProperty<T>,
IQueryExecute<T>{
}

