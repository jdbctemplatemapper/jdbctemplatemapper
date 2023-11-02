package io.github.jdbctemplatemapper.query;

public interface IQueryHasOne<T> {
  IQueryJoinColumnOneSide<T> joinColumnOneSide(String joinColumnOneSide);

}
