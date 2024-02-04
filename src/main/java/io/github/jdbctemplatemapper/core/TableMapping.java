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

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.exception.MapperException;

/**
 * The database table mapping details on an object
 *
 * @author ajoseph
 */
class TableMapping {
  private String tableClassName;
  private String tableName;
  private String schemaName;
  private String catalogName;
  private String commonDatabaseName;

  private String idPropertyName;
  private boolean idAutoIncrement = false;
  private String versionPropertyName = null;
  private String createdOnPropertyName = null;
  private String createdByPropertyName = null;
  private String updatedOnPropertyName = null;
  private String updatedByPropertyName = null;

  private boolean autoAssignProperties = false;

  private String identifierQuoteString = null;

  // model property to database column mapping.
  private List<PropertyMapping> propertyMappings;

  // these maps used for performance
  private Map<String, PropertyMapping> columnNameMap;
  private Map<String, PropertyMapping> propertyNameMap;
  private Map<String, PropertyMapping> columnAliasMap;

  public TableMapping(Class<?> tableClass, String tableName, String schemaName, String catalogName,
      String commonDatabaseName, IdPropertyInfo idPropertyInfo,
      List<PropertyMapping> propertyMappings, String identifierQuoteString) {
    Assert.notNull(tableClass, "tableClass must not be null");
    Assert.notNull(tableName, "tableName must not be null");
    Assert.notNull(idPropertyInfo, "idPropertyInfo must not be null");
    if (MapperUtils.isEmpty(propertyMappings)) {
      throw new IllegalArgumentException("propertyMappings cannot be null or empty");
    }

    this.tableClassName = tableClass.getName();
    this.tableName = tableName;
    this.schemaName = MapperUtils.isEmpty(schemaName) ? null : schemaName;
    this.catalogName = MapperUtils.isEmpty(catalogName) ? null : catalogName;
    this.commonDatabaseName = commonDatabaseName;
    this.idPropertyName = idPropertyInfo.getPropertyName();
    this.idAutoIncrement = idPropertyInfo.isIdAutoIncrement();
    this.propertyMappings = propertyMappings;
    this.identifierQuoteString = identifierQuoteString;

    // initialize the maps
    int size = propertyMappings.size();
    columnNameMap = new HashMap<>(size);
    propertyNameMap = new HashMap<>(size);
    columnAliasMap = new HashMap<>(2 * size);

    int cnt = 1;
    for (PropertyMapping propMapping : propertyMappings) {
      if (propMapping.isVersionAnnotation()) {
        versionPropertyName = propMapping.getPropertyName();
        autoAssignProperties = true;
      }
      if (propMapping.isCreatedOnAnnotation()) {
        createdOnPropertyName = propMapping.getPropertyName();
        autoAssignProperties = true;
      }
      if (propMapping.isCreatedByAnnotation()) {
        createdByPropertyName = propMapping.getPropertyName();
        autoAssignProperties = true;
      }
      if (propMapping.isUpdatedOnAnnotation()) {
        updatedOnPropertyName = propMapping.getPropertyName();
        autoAssignProperties = true;
      }
      if (propMapping.isUpdatedByAnnotation()) {
        updatedByPropertyName = propMapping.getPropertyName();
        autoAssignProperties = true;
      }
      // these maps used for performance. Using intern() to save some memory since aliases are
      // similar for other table mappings.
      String colAliasSuffix = ("c" + cnt).intern();
      propMapping.setColumnAliasSuffix(colAliasSuffix);
      // creating alias lookups for queries generated through Query and QueryMerge
      // aliases tc1, tc2, tc3 ...
      columnAliasMap.put((MapperUtils.TYPE_TABLE_COL_ALIAS_PREFIX + colAliasSuffix).intern(),
          propMapping);
      // aliases rc1, rc2, rc3
      columnAliasMap.put((MapperUtils.RELATED_TABLE_COL_ALIAS_PREFIX + colAliasSuffix).intern(),
          propMapping);

      columnNameMap.put(propMapping.getColumnName(), propMapping);

      propMapping.setIdentifierQuoteString(identifierQuoteString);


      propertyNameMap.put(propMapping.getPropertyName(), propMapping);

      cnt++;
    }
  }

  public String getColumnName(String propertyName) {
    PropertyMapping propMapping = propertyNameMap.get(propertyName);
    return propMapping == null ? null : propMapping.getColumnName();
  }

  public String getPropertyName(String columnName) {
    PropertyMapping propMapping = columnNameMap.get(columnName);
    return propMapping == null ? null : propMapping.getPropertyName();
  }

  public Class<?> getPropertyType(String propertyName) {
    PropertyMapping propMapping = propertyNameMap.get(propertyName);
    return propMapping == null ? null : propMapping.getPropertyType();
  }

  public int getPropertySqlType(String propertyName) {
    PropertyMapping propMapping = propertyNameMap.get(propertyName);
    return propMapping == null ? Types.NULL : propMapping.getColumnSqlDataType();
  }

  public String getTableClassName() {
    return tableClassName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getCatalogName() {
    return catalogName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getIdentifierQuoteString() {
    return identifierQuoteString;
  }

  public String getIdPropertyName() {
    return getIdPropertyMapping().getPropertyName();
  }

  public String getIdColumnName() {
    return getIdPropertyMapping().getColumnName();
  }

  public boolean isIdAutoIncrement() {
    return idAutoIncrement;
  }

  public PropertyMapping getIdPropertyMapping() {
    PropertyMapping propMapping = propertyNameMap.get(idPropertyName);
    if (propMapping != null) {
      return propMapping;
    } else {
      throw new MapperException("For @Id property " + idPropertyName
          + " could not find corresponding column in database table " + tableName);
    }
  }

  public List<PropertyMapping> getPropertyMappings() {
    return propertyMappings;
  }

  // public PropertyMapping getPropertyMappingByColumnName(String columnName) {
  // return columnNameMap.get(columnName);
  // }

  public PropertyMapping getPropertyMappingByPropertyName(String propertyName) {
    return propertyNameMap.get(propertyName);
  }

  public PropertyMapping getPropertyMappingByColumnAlias(String columnAlias) {
    return columnAliasMap.get(columnAlias);
  }

  public String fullyQualifiedTableName() {
    if (MapperUtils.isNotEmpty(schemaName)) {
      return getIdentifierForSql(schemaName) + "." + getIdentifierForSql(tableName);
    }

    if (MapperUtils.isNotEmpty(catalogName) && isMySql()) {
      return getIdentifierForSql(catalogName) + "." + getIdentifierForSql(tableName);
    }

    return getIdentifierForSql(tableName);
  }

  // if there is a schema or catalog it tacks a "." to them.
  public String fullyQualifiedTablePrefix() {
    if (MapperUtils.isNotEmpty(schemaName)) {
      return getIdentifierForSql(schemaName) + ".";
    }

    if (MapperUtils.isNotEmpty(catalogName) && isMySql()) {
      return getIdentifierForSql(catalogName) + ".";
    }

    return "";
  }

  public PropertyMapping getVersionPropertyMapping() {
    return versionPropertyName != null ? propertyNameMap.get(versionPropertyName) : null;
  }

  public PropertyMapping getCreatedOnPropertyMapping() {
    return createdOnPropertyName != null ? propertyNameMap.get(createdOnPropertyName) : null;
  }

  public PropertyMapping getCreatedByPropertyMapping() {
    return createdByPropertyName != null ? propertyNameMap.get(createdByPropertyName) : null;
  }

  public PropertyMapping getUpdatedOnPropertyMapping() {
    return updatedOnPropertyName != null ? propertyNameMap.get(updatedOnPropertyName) : null;
  }

  public PropertyMapping getUpdatedByPropertyMapping() {
    return updatedByPropertyName != null ? propertyNameMap.get(updatedByPropertyName) : null;
  }

  public boolean isMySql() {
    return "mysql".equalsIgnoreCase(commonDatabaseName);
  }

  public boolean hasAutoAssignProperties() {
    return autoAssignProperties;
  }

  public boolean isQuoteIdentifier() {
    return identifierQuoteString == null ? false : true;
  }

  public String getTableNameForSql() {
    return getIdentifierForSql(tableName);
  }
  
  public String getIdColumnNameForSql() {
    return getIdentifierForSql(getIdPropertyMapping().getColumnName());
  }
  
  public String getColumnNameForSql(String columnName) {
    return getIdentifierForSql(columnName);
  }
  
  public String getIdentifierForSql(String name) {
    if (identifierQuoteString == null) {
      return name;
    } else {
      return identifierQuoteString + name + identifierQuoteString;
    }
  }

}
