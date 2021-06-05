package org.jdbctemplatemapper.core;

import java.util.Set;

import org.springframework.util.ObjectUtils;

import lombok.Data;

/**
 * This holds the sql and the sql params needed to issue and update.
 * 
 * @author ajoseph
 */
@Data
public class UpdateSqlAndParams {
  private String sql; // the sql string
  private Set<String> params; // the parameters for the sql

  public UpdateSqlAndParams(String sql, Set<String> params) {
    if (ObjectUtils.isEmpty(sql) || params == null) {
      throw new IllegalArgumentException("sql and params cannot be null");
    }
    this.sql = sql;
    this.params = params;
  }
}
