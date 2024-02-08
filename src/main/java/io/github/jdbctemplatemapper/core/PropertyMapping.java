/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jdbctemplatemapper.core;

/**
 * Object property to database column mapping.
 *
 * @author ajoseph
 */
class PropertyMapping {
  private String propertyName;
  private Class<?> propertyType;
  private String columnName;
  private String identifierQuoteString;
  private int columnSqlDataType; // see java.sql.Types

  private boolean idAnnotation = false;
  private boolean createdOnAnnotation = false;
  private boolean updatedOnAnnotation = false;
  private boolean versionAnnotation = false;
  private boolean createdByAnnotation = false;
  private boolean updatedByAnnotation = false;
  private String columnAliasSuffix;


  public PropertyMapping(String propertyName, Class<?> propertyType, String columnName,
      int columnSqlDataType) {
    if (propertyName == null || propertyType == null || columnName == null) {
      throw new IllegalArgumentException("propertyName, propertyType, columnName must not be null");
    }
    this.propertyName = propertyName;
    this.propertyType = propertyType;
    // column names stored in lower case always
    // No plans to support case sensitive table column names or column names with spaces in them
    // this.columnName = MapperUtils.toLowerCase(columnName);
    this.columnName = columnName;
    this.columnSqlDataType = columnSqlDataType;
  }

  public void setColumnSqlDataType(int columnSqlDataType) {
    this.columnSqlDataType = columnSqlDataType;
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

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public int getColumnSqlDataType() {
    return columnSqlDataType;
  }

  public boolean isIdAnnotation() {
    return idAnnotation;
  }

  public void setIdAnnotation(boolean idAnnotation) {
    this.idAnnotation = idAnnotation;
  }

  public boolean isCreatedOnAnnotation() {
    return createdOnAnnotation;
  }

  public void setCreatedOnAnnotation(boolean createdOnAnnotation) {
    this.createdOnAnnotation = createdOnAnnotation;
  }

  public boolean isUpdatedOnAnnotation() {
    return updatedOnAnnotation;
  }

  public void setUpdatedOnAnnotation(boolean updatedOnAnnotation) {
    this.updatedOnAnnotation = updatedOnAnnotation;
  }

  public boolean isVersionAnnotation() {
    return versionAnnotation;
  }

  public void setVersionAnnotation(boolean versionAnnotation) {
    this.versionAnnotation = versionAnnotation;
  }

  public boolean isCreatedByAnnotation() {
    return createdByAnnotation;
  }

  public void setCreatedByAnnotation(boolean createdByAnnotation) {
    this.createdByAnnotation = createdByAnnotation;
  }

  public boolean isUpdatedByAnnotation() {
    return updatedByAnnotation;
  }

  public void setUpdatedByAnnotation(boolean updatedByAnnotation) {
    this.updatedByAnnotation = updatedByAnnotation;
  }

  public String getColumnAliasSuffix() {
    return columnAliasSuffix;
  }

  public void setColumnAliasSuffix(String colAliasSuffix) {
    this.columnAliasSuffix = colAliasSuffix;
  }

  public String getIdentifierQuoteString() {
    return identifierQuoteString;
  }

  public void setIdentifierQuoteString(String identifierQuoteString) {
    this.identifierQuoteString = identifierQuoteString;
  }

  public String getColumnNameForSql() {
    if (identifierQuoteString == null) {
      return this.columnName;
    } else {
      return identifierQuoteString + this.columnName + identifierQuoteString;
    }
  }

}
