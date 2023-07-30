package org.jdbctemplatemapper.core;

public class ColumnInfo {
  private String columnName;
  private int columnSqlDataType; // see  java.sql.Types
  private boolean autoIncrement = false;

  public ColumnInfo(String columnName, int columnSqlDataType, boolean autoIncrement) {
    this.columnName = columnName;
    this.columnSqlDataType = columnSqlDataType;
    this.autoIncrement = autoIncrement;
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
