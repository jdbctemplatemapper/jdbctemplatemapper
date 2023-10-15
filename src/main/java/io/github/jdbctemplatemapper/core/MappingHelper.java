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

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedBy;
import io.github.jdbctemplatemapper.annotation.CreatedOn;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedBy;
import io.github.jdbctemplatemapper.annotation.UpdatedOn;
import io.github.jdbctemplatemapper.annotation.Version;
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.exception.MapperException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.metadata.TableMetaDataProvider;
import org.springframework.jdbc.core.metadata.TableParameterMetaData;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Mapping helper.
 *
 * @author ajoseph
 */
class MappingHelper {
  // Map key - object class
  // value - the table mapping
  private Map<Class<?>, TableMapping> objectToTableMappingCache = new ConcurrentHashMap<>();

  // workaround for postgres driver bug for ResultSetMetaData
  private boolean forcePostgresTimestampWithTimezone = false;

  private String databaseProductName;

  private final JdbcTemplate jdbcTemplate;
  private final String schemaName;
  private final String catalogName;

  // For most jdbc drivers when getting column metadata using jdbc, the
  // columnPattern argument null
  // returns all the columns (which is the default for JdbcTemplateMapper). Some
  // jdbc drivers may require to pass something like '%'.
  private final String metaDataColumnNamePattern;

  private boolean includeSynonyms = false;

  /**
   * Constructor.
   *
   * @param jdbcTemplate The jdbcTemplate
   * @param schemaName database schema name.
   * @param catalogName database catalog name.
   * @param metaDataColumnNamePattern For most jdbc drivers getting column metadata from database
   *        the metaDataColumnNamePattern argument of null returns all the columns (which is the
   *        default for JdbcTemplateMapper). Some jdbc drivers may require to pass something like
   *        '%'.
   */
  public MappingHelper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName,
      String metaDataColumnNamePattern) {
    Assert.notNull(jdbcTemplate, "jdbcTemplate must not be null");

    this.jdbcTemplate = jdbcTemplate;
    this.schemaName = schemaName;
    this.catalogName = catalogName;
    this.metaDataColumnNamePattern = metaDataColumnNamePattern;

  }

  public void forcePostgresTimestampWithTimezone(boolean val) {
    this.forcePostgresTimestampWithTimezone = val;
  }

  public void includeSynonyms() {
    this.includeSynonyms = true;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getCatalogName() {
    return catalogName;
  }

  public String getMetaDataColumnNamePattern() {
    return metaDataColumnNamePattern;
  }

  /**
   * Gets the table mapping for the Object. The table mapping has the table name and and object
   * property to database column mapping.
   *
   * <p>
   * Table name is either from the @Tabel annotation or the underscore case conversion of the Object
   * name.
   *
   * @param clazz The object class
   * @return The table mapping.
   */
  public TableMapping getTableMapping(Class<?> clazz) {
    Assert.notNull(clazz, "clazz must not be null");

    TableMapping tableMapping = objectToTableMappingCache.get(clazz);
    if (tableMapping == null) {
      TableColumnInfo tableColumnInfo = getTableColumnInfo(clazz);
      String tableName = tableColumnInfo.getTableName();

      IdPropertyInfo idPropertyInfo = getIdPropertyInfo(clazz);

      // key:column name, value: ColumnInfo
      Map<String, ColumnInfo> columnNameToColumnInfo = tableColumnInfo.getColumnInfos().stream()
          .collect(Collectors.toMap(o -> o.getColumnName(), o -> o));

      // key:propertyName, value:PropertyMapping. LinkedHashMap to maintain order of
      // properties
      Map<String, PropertyMapping> propNameToPropertyMapping = new LinkedHashMap<>();
      for (Field field : clazz.getDeclaredFields()) {
        String propertyName = field.getName();

        Column colAnnotation = AnnotationUtils.findAnnotation(field, Column.class);
        if (colAnnotation != null) {
          String colName = colAnnotation.name();
          if ("[DEFAULT]".equals(colName)) {
            colName = MapperUtils.toUnderscoreName(propertyName);
          }
          colName = MapperUtils.toLowerCase(colName);
          if (!columnNameToColumnInfo.containsKey(colName)) {
            throw new AnnotationException(colName + " column not found in table " + tableName
                + " for model property " + clazz.getSimpleName() + "." + propertyName);
          }
          propNameToPropertyMapping.put(propertyName,
              new PropertyMapping(propertyName, field.getType(), colName,
                  columnNameToColumnInfo.get(colName).getColumnSqlDataType()));
        }

        processAnnotation(Id.class, field, tableName, propNameToPropertyMapping,
            columnNameToColumnInfo);
        processAnnotation(Version.class, field, tableName, propNameToPropertyMapping,
            columnNameToColumnInfo);
        processAnnotation(CreatedOn.class, field, tableName, propNameToPropertyMapping,
            columnNameToColumnInfo);
        processAnnotation(UpdatedOn.class, field, tableName, propNameToPropertyMapping,
            columnNameToColumnInfo);
        processAnnotation(CreatedBy.class, field, tableName, propNameToPropertyMapping,
            columnNameToColumnInfo);
        processAnnotation(UpdatedBy.class, field, tableName, propNameToPropertyMapping,
            columnNameToColumnInfo);

        // postgres driver bug where the database metadata returns TIMESTAMP instead of
        // TIMESTAMP_WITH_TIMEZONE for columns timestamptz.
        if (forcePostgresTimestampWithTimezone) {
          PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
          if (propMapping != null) {
            if (OffsetDateTime.class == propMapping.getPropertyType()
                && propMapping.getColumnSqlDataType() == Types.TIMESTAMP) {
              propMapping.setColumnSqlDataType(Types.TIMESTAMP_WITH_TIMEZONE);
            }
          }
        }
      }

      List<PropertyMapping> propertyMappings = new ArrayList<>(propNameToPropertyMapping.values());
      validateAnnotations(propertyMappings, clazz);

      tableMapping = new TableMapping(clazz, tableName, tableColumnInfo.getSchemaName(),
          tableColumnInfo.getCatalogName(), JdbcUtils.commonDatabaseName(getDatabaseProductName()),
          idPropertyInfo.getPropertyName(), propertyMappings);
      tableMapping.setIdAutoIncrement(idPropertyInfo.isIdAutoIncrement());

      objectToTableMappingCache.put(clazz, tableMapping);
    }
    return tableMapping;
  }

  private IdPropertyInfo getIdPropertyInfo(Class<?> clazz) {
    Id idAnnotation = null;
    String idPropertyName = null;
    boolean isIdAutoIncrement = false;
    for (Field field : clazz.getDeclaredFields()) {
      idAnnotation = AnnotationUtils.findAnnotation(field, Id.class);
      if (idAnnotation != null) {
        idPropertyName = field.getName();
        if (idAnnotation.type() == IdType.AUTO_INCREMENT) {
          if (Number.class.isAssignableFrom(field.getType())) {
            isIdAutoIncrement = true;
          } else {
            throw new AnnotationException(clazz.getSimpleName() + "." + idPropertyName
                + " is auto increment id so has to be a non-primitive Number object.");
          }
        }
        break;
      }
    }
    if (idAnnotation == null) {
      throw new AnnotationException(
          "@Id annotation not found in class " + clazz.getSimpleName() + " . It is required");
    }

    return new IdPropertyInfo(clazz, idPropertyName, isIdAutoIncrement);
  }

  private TableColumnInfo getTableColumnInfo(Class<?> clazz) {
    Table tableAnnotation = AnnotationUtils.findAnnotation(clazz, Table.class);
    validateTableAnnotation(tableAnnotation, clazz);

    String catalog = getCatalogForTable(tableAnnotation);
    String schema = getSchemaForTable(tableAnnotation);

    validateMetaDataConfig(catalog, schema);

    String tableName = tableAnnotation.name();
    List<ColumnInfo> columnInfoList = getColumnInfoFromDatabaseMetadata(tableName, schema, catalog);
    if (MapperUtils.isEmpty(columnInfoList)) {
      throw new AnnotationException(
          getTableMetaDataNotFoundErrMsg(clazz, tableName, schema, catalog));
    }

    return new TableColumnInfo(tableName, schema, catalog, columnInfoList);
  }

  private List<ColumnInfo> getColumnInfoFromDatabaseMetadata(String tableName, String schema,
      String catalog) {
    Assert.hasLength(tableName, "tableName must not be empty");

    TableMetaDataProvider provider = JtmTableMetaDataProviderFactory.createMetaDataProvider(
        jdbcTemplate.getDataSource(), catalog, schema, tableName, includeSynonyms);

    List<ColumnInfo> columnInfoList = new ArrayList<>();

    List<TableParameterMetaData> list = provider.getTableParameterMetaData();
    for (TableParameterMetaData metaData : list) {
      ColumnInfo columnInfo = new ColumnInfo(metaData.getParameterName(), metaData.getSqlType());
      columnInfoList.add(columnInfo);
    }

    return columnInfoList;
  }

  private String getDatabaseProductName() {
    if (this.databaseProductName != null) {
      return this.databaseProductName;
    } else {
      try {
        synchronized (this) {
          this.databaseProductName = JdbcUtils.extractDatabaseMetaData(jdbcTemplate.getDataSource(),
              new DatabaseMetaDataCallback<String>() {
                public String processMetaData(DatabaseMetaData dbMetaData)
                    throws SQLException, MetaDataAccessException {
                  return dbMetaData.getDatabaseProductName();
                }
              });
        }
      } catch (Exception e) {
        throw new MapperException(e);
      }
      return this.databaseProductName;
    }

  }

  boolean getForcePostgresTimestampWithTimezone() {
    return forcePostgresTimestampWithTimezone;
  }

  private <T extends Annotation> void processAnnotation(Class<T> annotationClazz, Field field,
      String tableName, Map<String, PropertyMapping> propNameToPropertyMapping,
      Map<String, ColumnInfo> columnNameToColumnInfo) {

    Annotation annotation = AnnotationUtils.findAnnotation(field, annotationClazz);
    if (annotation != null) {
      String propertyName = field.getName();
      PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
      if (propMapping == null) { // it means there is no @Column annotation for the property
        String colName = MapperUtils.toUnderscoreName(propertyName); // the default column name
        if (!columnNameToColumnInfo.containsKey(colName)) {
          throw new AnnotationException(
              colName + " column not found in table " + tableName + " for model property "
                  + field.getDeclaringClass().getSimpleName() + "." + field.getName());
        }
        propMapping = new PropertyMapping(propertyName, field.getType(), colName,
            columnNameToColumnInfo.get(colName).getColumnSqlDataType());
        propNameToPropertyMapping.put(propertyName, propMapping);
      }

      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(propMapping);
      bw.setPropertyValue(StringUtils.uncapitalize(annotationClazz.getSimpleName()) + "Annotation",
          true);
    }
  }

  private void validateTableAnnotation(Table tableAnnotation, Class<?> clazz) {
    if (tableAnnotation == null) {
      throw new AnnotationException(
          clazz.getSimpleName() + " does not have the @Table annotation. It is required");
    }

    if (MapperUtils.isEmpty(tableAnnotation.name().trim())) {
      throw new AnnotationException(
          "For " + clazz.getSimpleName() + " the @Table annotation has a blank name");
    }
  }

  private void validateAnnotations(List<PropertyMapping> propertyMappings, Class<?> clazz) {
    int idCnt = 0;
    int versionCnt = 0;
    int createdByCnt = 0;
    int createdOnCnt = 0;
    int updatedOnCnt = 0;
    int updatedByCnt = 0;

    for (PropertyMapping propMapping : propertyMappings) {
      int conflictCnt = 0;

      if (propMapping.isIdAnnotation()) {
        idCnt++;
        conflictCnt++;
      }
      if (propMapping.isVersionAnnotation()) {
        versionCnt++;
        conflictCnt++;
      }
      if (propMapping.isCreatedOnAnnotation()) {
        createdOnCnt++;
        conflictCnt++;
      }
      if (propMapping.isCreatedByAnnotation()) {
        createdByCnt++;
        conflictCnt++;
      }
      if (propMapping.isUpdatedOnAnnotation()) {
        updatedOnCnt++;
        conflictCnt++;
      }
      if (propMapping.isUpdatedByAnnotation()) {
        updatedByCnt++;
        conflictCnt++;
      }

      if (propMapping.isVersionAnnotation() && Integer.class != propMapping.getPropertyType()) {
        throw new AnnotationException("@Version requires the type of property "
            + clazz.getSimpleName() + "." + propMapping.getPropertyName() + " to be Integer");
      }

      if (propMapping.isCreatedOnAnnotation()
          && LocalDateTime.class != propMapping.getPropertyType()) {
        throw new AnnotationException("@CreatedOn requires the type of property "
            + clazz.getSimpleName() + "." + propMapping.getPropertyName() + " to be LocalDateTime");
      }

      if (propMapping.isUpdatedOnAnnotation()
          && LocalDateTime.class != propMapping.getPropertyType()) {
        throw new AnnotationException("@UpdatedOn requires the type of property "
            + clazz.getSimpleName() + "." + propMapping.getPropertyName() + " to be LocalDateTime");
      }

      if (conflictCnt > 1) {
        throw new AnnotationException(clazz.getSimpleName() + "." + propMapping.getPropertyName()
            + " has multiple annotations that conflict");
      }
    }

    if (idCnt > 1) {
      throw new AnnotationException(
          " model " + clazz.getSimpleName() + " has multiple @Id annotations");
    }
    if (versionCnt > 1) {
      throw new AnnotationException(
          " model " + clazz.getSimpleName() + " has multiple @Version annotations");
    }
    if (createdOnCnt > 1) {
      throw new AnnotationException(
          " model " + clazz.getSimpleName() + " has multiple @CreatedOn annotations");
    }
    if (createdByCnt > 1) {
      throw new AnnotationException(
          " model " + clazz.getSimpleName() + " has multiple @CreatedBy annotations");
    }
    if (updatedOnCnt > 1) {
      throw new AnnotationException(
          " model " + clazz.getSimpleName() + " has multiple @UpdatedOn annotations");
    }
    if (updatedByCnt > 1) {
      throw new AnnotationException(
          " model " + clazz.getSimpleName() + " has multiple @UpdatedBy annotations");
    }
  }

  private void validateMetaDataConfig(String catalogName, String schemaName) {
    String commonDatabaseName = JdbcUtils.commonDatabaseName(getDatabaseProductName());
    if ("mysql".equalsIgnoreCase(commonDatabaseName)) {
      if (MapperUtils.isNotEmpty(schemaName)) {
        throw new MapperException(getDatabaseProductName() + " does not support schema.");
      }
    }

    if ("oracle".equalsIgnoreCase(commonDatabaseName)) {
      if (MapperUtils.isNotEmpty(catalogName)) {
        throw new MapperException(getDatabaseProductName() + " does not support catalog.");
      }
    }
  }

  private String getCatalogForTable(Table tableAnnotation) {
    return MapperUtils.isEmpty(tableAnnotation.catalog()) ? this.catalogName
        : tableAnnotation.catalog();
  }

  private String getSchemaForTable(Table tableAnnotation) {
    return MapperUtils.isEmpty(tableAnnotation.schema()) ? this.schemaName
        : tableAnnotation.schema();
  }

  private String getTableMetaDataNotFoundErrMsg(Class<?> clazz, String tableName, String schema,
      String catalog) {
    String errMsg = "Unable to locate meta-data for table '" + tableName + "'";

    if (schema != null && catalog != null) {
      errMsg += " in schema " + schema + " and catalog " + catalog;
    } else {
      if (schema != null) {
        errMsg += " in schema " + schema;
      }
      if (catalog != null) {
        errMsg += " in catalog/database " + catalog;
      }
    }
    errMsg += " for class " + clazz.getSimpleName();
    return errMsg;
  }

}
