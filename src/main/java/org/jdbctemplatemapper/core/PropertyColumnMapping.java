package org.jdbctemplatemapper.core;

import lombok.Data;

@Data
/**
 * object property to database column mapping.
 *
 * @author ajoseph
 */
public class PropertyColumnMapping {
  private String propertyName;
  private String columnName;

  public PropertyColumnMapping(String propertyName, String columnName) {
    if (propertyName == null || columnName == null) {
      throw new IllegalArgumentException("propertyName and columnName must not be null");
    }
    this.propertyName = propertyName;
    this.columnName = columnName;
  }
}
