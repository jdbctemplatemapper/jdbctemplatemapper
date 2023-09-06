package io.github.jdbctemplatemapper.query;
/**
 * interface with the next methods in the chain
 * 
 * @author ajoseph
 *
 * @param <T> the type
 */
public interface IQueryHasMany <T>{
    IQueryJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide);
    IQueryThroughJoinTable<T> throughJoinTable(String tableName);
}
