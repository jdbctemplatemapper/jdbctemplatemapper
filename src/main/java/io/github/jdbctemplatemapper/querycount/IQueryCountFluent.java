package io.github.jdbctemplatemapper.querycount;

/**
 * The fluent style interface for QueryCount.
 *
 * @author ajoseph
 * @param <T> the type
 */
public interface IQueryCountFluent<T> extends IQueryCountType<T>, IQueryCountHasOne<T>,
    IQueryCountJoinColumnOwningSide<T>, IQueryCountWhere<T>, IQueryCountExecute<T> {
}
