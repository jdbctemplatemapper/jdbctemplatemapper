package org.jdbctemplatemapper.config;

import org.jdbctemplatemapper.core.IRecordOperatorResolver;

public class RecordOperatorResolver implements IRecordOperatorResolver {
  public Object getRecordOperator() {
      return "tester";
  }
}
