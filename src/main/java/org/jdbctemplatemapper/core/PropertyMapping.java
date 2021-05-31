package org.jdbctemplatemapper.core;

import lombok.Data;

@Data
/**
 * object property to database column mapping.
 *
 * @author ajoseph
 */
public class PropertyMapping {
  private String propertyName;
  private String columnName;

  public PropertyMapping(String propertyName, String columnName) {
    if (propertyName == null || columnName == null) {
      throw new IllegalArgumentException("propertyName and columnName must not be null");
    }
    this.propertyName = propertyName;
    this.columnName = columnName;
  }
}
