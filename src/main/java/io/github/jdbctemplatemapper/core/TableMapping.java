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
import java.util.ArrayList;
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
  private Class<?> tableClass;
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

  // model property to database column mapping.
  private List<PropertyMapping> propertyMappings = new ArrayList<>();

  // these maps used for performance
  private Map<String, PropertyMapping> columnNameMap = new HashMap<>();
  private Map<String, PropertyMapping> propertyNameMap = new HashMap<>();

  public TableMapping(Class<?> tableClass, String tableName, String schemaName, String catalogName,
      String commonDatabaseName, IdPropertyInfo idPropertyInfo,
      List<PropertyMapping> propertyMappings) {
    Assert.notNull(tableClass, "tableClass must not be null");
    Assert.notNull(tableName, "tableName must not be null");
    Assert.notNull(idPropertyInfo, "idPropertyInfo must not be null");

    this.tableClass = tableClass;
    this.tableName = tableName;
    this.schemaName = MapperUtils.isEmpty(schemaName) ? null : schemaName;
    this.catalogName = MapperUtils.isEmpty(catalogName) ? null : catalogName;
    this.commonDatabaseName = commonDatabaseName;

    this.idPropertyName = idPropertyInfo.getPropertyName();
    this.idAutoIncrement = idPropertyInfo.isIdAutoIncrement();

    this.propertyMappings = propertyMappings;

    if (propertyMappings != null) {
      for (PropertyMapping propMapping : propertyMappings) {
        if (propMapping.isVersionAnnotation()) {
          versionPropertyName = propMapping.getPropertyName();
        }
        if (propMapping.isCreatedOnAnnotation()) {
          createdOnPropertyName = propMapping.getPropertyName();
        }
        if (propMapping.isCreatedByAnnotation()) {
          createdByPropertyName = propMapping.getPropertyName();
        }
        if (propMapping.isUpdatedOnAnnotation()) {
          updatedOnPropertyName = propMapping.getPropertyName();
        }
        if (propMapping.isUpdatedByAnnotation()) {
          updatedByPropertyName = propMapping.getPropertyName();
        }
        // these maps used for performance
        columnNameMap.put(propMapping.getColumnName(), propMapping);
        propertyNameMap.put(propMapping.getPropertyName(), propMapping);
      }
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

  public Class<?> getTableClass() {
    return tableClass;
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

  public PropertyMapping getPropertyMappingByColumnName(String columnName) {
    return columnNameMap.get(columnName);
  }

  public PropertyMapping getPropertyMappingByPropertyName(String propertyName) {
    return propertyNameMap.get(propertyName);
  }

  public String fullyQualifiedTableName() {
    if (MapperUtils.isNotEmpty(schemaName)) {
      return schemaName + "." + tableName;
    }

    if (MapperUtils.isNotEmpty(catalogName) && isMySql()) {
      return catalogName + "." + tableName;
    }

    return tableName;
  }

  // if there is a schema or catalog it tacks a "." to them.
  public String fullyQualifiedTablePrefix() {
    if (MapperUtils.isNotEmpty(schemaName)) {
      return schemaName + ".";
    }

    if (MapperUtils.isNotEmpty(catalogName) && isMySql()) {
      return catalogName + ".";
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

}
