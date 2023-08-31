package io.github.jdbctemplatemapper.core;

import java.util.List;

public interface IQueryExecute<T> {
     List<T> execute(JdbcTemplateMapper jtm);
}
