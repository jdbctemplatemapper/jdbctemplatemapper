package io.github.jdbctemplatemapper.config;

import io.github.jdbctemplatemapper.core.IRecordOperatorResolver;

public class RecordOperatorResolver implements IRecordOperatorResolver {
  public Object getRecordOperator() {
      return "tester";
  }
}
