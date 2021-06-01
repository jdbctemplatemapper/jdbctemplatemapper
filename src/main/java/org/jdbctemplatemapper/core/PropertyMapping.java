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
  private Class<?> propertyType;
  private String columnName;
  private int columnDataType; // see java.sql.Types

  public PropertyMapping(String propertyName, Class<?> propertyType, String columnName, int columnDataType) {
    if (propertyName == null || propertyType == null || columnName == null) {
      throw new IllegalArgumentException("propertyName, propertyType, columnName must not be null");
    }
    this.propertyName = propertyName;
    this.propertyType = propertyType;
    this.columnName = columnName;
    this.columnDataType = columnDataType;
  }
}
