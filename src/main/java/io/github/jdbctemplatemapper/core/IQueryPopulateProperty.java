package io.github.jdbctemplatemapper.core;

import java.util.List;

public interface IQueryPopulateProperty<T> {
  List<T> execute(JdbcTemplateMapper jtm);
}
