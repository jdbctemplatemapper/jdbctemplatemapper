package org.jdbctemplatemapper.core;

/**
 * object property to database column mapping.
 *
 * @author ajoseph
 */
public class PropertyMapping {
  private String propertyName;
  private Class<?> propertyType;
  private String columnName;
  private int columnSqlDataType; // see java.sql.Types
  private boolean autoIncrement = false;

  public PropertyMapping(
      String propertyName, Class<?> propertyType, String columnName, int columnSqlDataType, boolean autoIncrement) {
    if (propertyName == null || propertyType == null || columnName == null) {
      throw new IllegalArgumentException("propertyName, propertyType, columnName must not be null");
    }
    this.propertyName = propertyName;
    this.propertyType = propertyType;
    this.columnName = columnName;
    this.columnSqlDataType = columnSqlDataType;
    this.autoIncrement = autoIncrement;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public Class<?> getPropertyType() {
    return propertyType;
  }

  public String getColumnName() {
    return columnName;
  }

  public int getColumnSqlDataType() {
    return columnSqlDataType;
  }
  
  public boolean isAutoIncrement() {
	    return autoIncrement;
}
}
