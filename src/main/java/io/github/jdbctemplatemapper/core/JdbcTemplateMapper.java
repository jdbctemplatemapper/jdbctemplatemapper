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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcInsertOperations;
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.exception.OptimisticLockingException;

/**
 * CRUD methods and configuration for JdbcTemplateMapper.
 *
 * Should be prepared in a Spring application context and given to services as bean reference.
 * JdbcTemplateMapper caches Table meta-data and SQL.
 * 
 * <b> Note: An instance of JdbcTemplateMapper is thread safe once instantiated.</b>
 * 
 * <pre>
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#jdbctemplatemapper">JdbcTemplateMapper documentation</a> 
 * for more info
 * </pre>
 *
 * @author ajoseph
 */
public final class JdbcTemplateMapper {

  private static final int CACHEABLE_UPDATE_PROPERTIES_COUNT = 3;

  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate npJdbcTemplate;

  private final MappingHelper mappingHelper;
  private IRecordOperatorResolver recordOperatorResolver;

  // insert cache. Note that Spring SimpleJdbcInsert is thread safe.
  // Map key - class name
  // value - SimpleJdbcInsert
  private SimpleCache<String, SimpleJdbcInsert> insertCache = new SimpleCache<>();

  // update sql cache
  // Map key - class name
  // value - the update sql and params
  private SimpleCache<String, SqlAndParams> updateCache = new SimpleCache<>();

  // update specified properties sql cache
  // Map key - class name and properties
  // value - the update sql and params
  private SimpleCache<String, SqlAndParams> updatePropertiesCache = new SimpleCache<>(1000);

  // the column sql string with bean friendly column aliases for mapped properties of model.
  // Map key - class name
  // value - the column sql string
  private SimpleCache<String, String> beanColumnsSqlCache = new SimpleCache<>();

  // Query sql cache
  // Map key - see Query.getCacheKey()
  // value - the partial sql.
  private SimpleCache<String, String> querySqlCache = new SimpleCache<>(1000);

  // QueryMerge sql cache
  // Map key - see QueryMerge.getCacheKey()
  // value - the partial sql.
  private SimpleCache<String, String> queryMergeSqlCache = new SimpleCache<>(1000);

  // QueryCount sql cache
  // Map key - see QueryCount.getCacheKey()
  // value - the partial sql.
  private SimpleCache<String, String> queryCountSqlCache = new SimpleCache<>(1000);

  // Spring BeanPropertyRowMapper uses this as its converter so use the same
  private DefaultConversionService conversionService =
      (DefaultConversionService) DefaultConversionService.getSharedInstance();

  private boolean includeSynonyms = false;

  /**
   * Constructor.
   *
   * @param jdbcTemplate the jdbcTemplate.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    this(jdbcTemplate, null, null);
  }

  /**
   * Constructor.
   *
   * @param jdbcTemplate the jdbcTemplate.
   * @param schemaName database schema name.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName) {
    this(jdbcTemplate, schemaName, null);
  }

  /**
   * Constructor.
   *
   * @param jdbcTemplate the jdbcTemplate
   * @param schemaName database schema name.
   * @param catalogName database catalog name.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName) {
    Assert.notNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.jdbcTemplate = jdbcTemplate;

    npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

    mappingHelper = new MappingHelper(jdbcTemplate, schemaName, catalogName);
  }

  /**
   * Gets the JdbcTemplate of the jdbcTemplateMapper.
   *
   * @return the JdbcTemplate
   */
  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  /**
   * Gets the NamedParameterJdbcTemplate of the jdbcTemplateMapper.
   *
   * @return the NamedParameterJdbcTemplate
   */
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
    return npJdbcTemplate;
  }

  /**
   * An implementation of IRecordOperatorResolver is used to populate the &#64;CreatedBy and
   * &#64;UpdatedBy annotated properties.
   *
   * @param recordOperatorResolver The implement for interface IRecordOperatorResolver
   * @return The jdbcTemplateMapper The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withRecordOperatorResolver(
      IRecordOperatorResolver recordOperatorResolver) {
    this.recordOperatorResolver = recordOperatorResolver;
    return this;
  }

  /**
   * Oracle needs this to get the meta-data of table synonyms. Other databases don't need this.
   */
  public void includeSynonymsForTableColumnMetaData() {
    this.includeSynonyms = true;
    mappingHelper.includeSynonyms();
  }

  /**
   * Get the schema name.
   *
   * @return the schema name.
   */
  public String getSchemaName() {
    return mappingHelper.getSchemaName();
  }

  /**
   * Get the catalog name.
   *
   * @return the catalog name.
   */
  public String getCatalogName() {
    return mappingHelper.getCatalogName();
  }

  /**
   * Exposing the conversion service used so if necessary new converters can be added.
   *
   * @return the default conversion service.
   */
  public DefaultConversionService getConversionService() {
    return (DefaultConversionService) conversionService;
  }

  /**
   * finds the object by Id. Return null if not found
   *
   * @param <T> the type
   * @param clazz Class of object
   * @param id Id of object
   * @return the object of type T
   */
  public <T> T findById(Class<T> clazz, Object id) {
    Assert.notNull(clazz, "Class must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String columnsSql = getBeanColumnsSqlInternal(tableMapping, clazz);
    String sql = "SELECT " + columnsSql + " FROM " + tableMapping.fullyQualifiedTableName()
        + " WHERE " + tableMapping.getIdColumnNameForSql() + " = ?";

    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);

    try {
      Object obj = jdbcTemplate.queryForObject(sql, mapper, id);
      return clazz.cast(obj);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  /**
   * Find all objects.
   *
   * @param <T> the type
   * @param clazz Type of object
   * @return List of objects of type T
   */
  public <T> List<T> findAll(Class<T> clazz) {
    return findAll(clazz, null);
  }

  /**
   * Find all objects ordered by orderByPropertyName ascending.
   *
   * @param <T> the type
   * @param clazz Type of object
   * @param orderByPropertyName the order by property
   * @return List of objects of type T
   */
  public <T> List<T> findAll(Class<T> clazz, String orderByPropertyName) {
    Assert.notNull(clazz, "Class must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String columnsSql = getBeanColumnsSqlInternal(tableMapping, clazz);

    String orderByColumnName = null;
    if (orderByPropertyName != null) {
      orderByColumnName = tableMapping.getColumnName(orderByPropertyName);
      if (orderByColumnName == null) {
        throw new MapperException(
            "orderByPropertyName " + clazz.getSimpleName() + "." + orderByPropertyName
                + " is either invalid or does not have a corresponding column in database.");
      }
    }

    String sql = "SELECT " + columnsSql + " FROM " + tableMapping.fullyQualifiedTableName();

    if (orderByColumnName != null) {
      sql = sql + " ORDER BY " + orderByColumnName + " ASC";
    }

    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  /**
   * Inserts an object. Objects with auto increment id will have the id set to the new id from
   * database. For non auto increment id the id has to be manually set before invoking insert().
   *
   * <pre>
   * Will handle the following annotations:
   * &#64;CreatedOn property will be assigned current date and time
   * &#64;CreatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property 
   *      will be assigned that value
   * &#64;UpdatedOn property will be assigned current date and time
   * &#64;UpdatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property
   *                will be assigned that value
   * &#64;Version property will be set to 1. Used for optimistic locking.
   * </pre>
   *
   * @param obj The object to be saved
   */
  public void insert(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());
    BeanWrapper bw = getBeanWrapper(obj);
    Object idValue = bw.getPropertyValue(tableMapping.getIdPropertyName());
    if (tableMapping.isIdAutoIncrement()) {
      if (idValue != null) {
        throw new MapperException("For insert() the property " + obj.getClass().getSimpleName()
            + "." + tableMapping.getIdPropertyName()
            + " has to be null since this insert is for an object whose id is auto increment.");
      }
    } else {
      if (idValue == null) {
        throw new MapperException("For insert() the property " + obj.getClass().getSimpleName()
            + "." + tableMapping.getIdPropertyName()
            + " cannot be null since it is not an auto increment id");
      }
    }

    if (tableMapping.hasAutoAssignProperties()) {
      LocalDateTime now = LocalDateTime.now();

      PropertyMapping createdOnPropMapping = tableMapping.getCreatedOnPropertyMapping();
      if (createdOnPropMapping != null) {
        bw.setPropertyValue(createdOnPropMapping.getPropertyName(), now);
      }

      PropertyMapping updatedOnPropMapping = tableMapping.getUpdatedOnPropertyMapping();
      if (updatedOnPropMapping != null) {
        bw.setPropertyValue(updatedOnPropMapping.getPropertyName(), now);
      }

      PropertyMapping createdByPropMapping = tableMapping.getCreatedByPropertyMapping();
      if (createdByPropMapping != null && recordOperatorResolver != null) {
        bw.setPropertyValue(createdByPropMapping.getPropertyName(),
            recordOperatorResolver.getRecordOperator());
      }

      PropertyMapping updatedByPropMapping = tableMapping.getUpdatedByPropertyMapping();
      if (updatedByPropMapping != null && recordOperatorResolver != null) {
        bw.setPropertyValue(updatedByPropMapping.getPropertyName(),
            recordOperatorResolver.getRecordOperator());
      }

      PropertyMapping versionPropMapping = tableMapping.getVersionPropertyMapping();
      if (versionPropMapping != null) {
        // version property value defaults to 1 on inserts
        bw.setPropertyValue(versionPropMapping.getPropertyName(), 1);
      }
    }

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
      mapSqlParameterSource.addValue(propMapping.getColumnName(),
          bw.getPropertyValue(propMapping.getPropertyName()));
    }

    boolean foundInCache = false;
    SimpleJdbcInsertOperations jdbcInsert = insertCache.get(obj.getClass().getName());
    if (jdbcInsert == null) {
      jdbcInsert =
          new SimpleJdbcInsert(jdbcTemplate).withCatalogName(tableMapping.getCatalogName())
                                            .withSchemaName(tableMapping.getSchemaName())
                                            .withTableName(
                                                tableNameForSimpleJdbcInsert(tableMapping));

      if (tableMapping.isIdAutoIncrement()) {
        jdbcInsert.usingGeneratedKeyColumns(tableMapping.getIdColumnName());
      }
      // for oracle synonym table metadata
      if (includeSynonyms) {
        jdbcInsert.includeSynonymsForTableColumnMetaData();
      }
    } else {
      foundInCache = true;
    }

    if (tableMapping.isIdAutoIncrement()) {
      Number idNumber = jdbcInsert.executeAndReturnKey(mapSqlParameterSource);
      bw.setPropertyValue(tableMapping.getIdPropertyName(), idNumber); // set object id value
    } else {
      jdbcInsert.execute(mapSqlParameterSource);
    }

    if (!foundInCache) {
      // SimpleJdbcInsert is thread safe.
      insertCache.put(obj.getClass().getName(), (SimpleJdbcInsert) jdbcInsert);
    }
  }

  /**
   * Update the object.
   *
   * <pre>
   * Will handle the following annotations:
   * &#64;UpdatedOn property will be assigned current date and time
   * &#64;UpdatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property 
   *                will be assigned that value
   * &#64;Version property will be incremented on a successful update. An OptimisticLockingException
   *                will be thrown if object is stale.
   * </pre>
   *
   * @param obj object to be updated
   * @return number of records updated
   */
  public Integer update(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

    boolean foundInCache = false;
    SqlAndParams sqlAndParams = updateCache.get(obj.getClass().getName());
    if (sqlAndParams == null) {
      sqlAndParams = buildSqlAndParamsForUpdate(tableMapping);
    } else {
      foundInCache = true;
    }
    Integer cnt = updateInternal(obj, sqlAndParams, tableMapping);

    if (!foundInCache && cnt > 0) {
      updateCache.put(obj.getClass().getName(), sqlAndParams);
    }

    return cnt;
  }

  /**
   * Updates the specified properties passed in as arguments. Use it when you want to update a
   * property or a few properties of the object and not the whole object. Issues an update for only
   * the specific properties and any auto assign properties. Comes in handy for tables with large
   * number of columns and need to update only a few. Also you can instantiate a new Object,
   * populate the id and just the properties needed and invoke updateProperties().
   *
   * <pre>
   * Will handle the following annotations:
   * &#64;UpdatedOn property will be assigned current date and time
   * &#64;UpdatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property 
   *                will be assigned that value
   * &#64;Version property will be incremented on a successful update. An OptimisticLockingException
   *                will be thrown if object is stale.
   * </pre>
   *
   * @param obj object to be updated
   * @param propertyNames the specific property names that need to be updated.
   * @return number of records updated
   */
  public Integer updateProperties(Object obj, String... propertyNames) {
    Assert.notNull(obj, "Object must not be null");
    Assert.notNull(propertyNames, "propertyNames must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

    boolean foundInCache = false;
    SqlAndParams sqlAndParams = null;
    String cacheKey = getUpdatePropertiesCacheKey(obj, propertyNames);
    if (cacheKey != null) {
      sqlAndParams = updatePropertiesCache.get(cacheKey);
    }

    if (sqlAndParams == null) {
      sqlAndParams = buildSqlAndParamsForUpdateProperties(tableMapping, propertyNames);
    } else {
      foundInCache = true;
    }

    Integer cnt = updateInternal(obj, sqlAndParams, tableMapping);

    if (cacheKey != null && !foundInCache && cnt > 0) {
      updatePropertiesCache.put(cacheKey, sqlAndParams);
    }

    return cnt;
  }

  private Integer updateInternal(Object obj, SqlAndParams sqlAndParams, TableMapping tableMapping) {
    Assert.notNull(obj, "Object must not be null");
    Assert.notNull(sqlAndParams, "sqlAndParams must not be null");

    BeanWrapper bw = getBeanWrapper(obj);

    if (bw.getPropertyValue(tableMapping.getIdPropertyName()) == null) {
      throw new IllegalArgumentException("Property " + tableMapping.getTableClassName() + "."
          + tableMapping.getIdPropertyName() + " is the id and cannot be null.");
    }

    Set<String> parameters = sqlAndParams.getParams();

    if (tableMapping.hasAutoAssignProperties()) {
      PropertyMapping updatedByPropMapping = tableMapping.getUpdatedByPropertyMapping();
      if (updatedByPropMapping != null && recordOperatorResolver != null
          && parameters.contains(updatedByPropMapping.getPropertyName())) {
        bw.setPropertyValue(updatedByPropMapping.getPropertyName(),
            recordOperatorResolver.getRecordOperator());
      }

      PropertyMapping updatedOnPropMapping = tableMapping.getUpdatedOnPropertyMapping();
      if (updatedOnPropMapping != null
          && parameters.contains(updatedOnPropMapping.getPropertyName())) {
        bw.setPropertyValue(updatedOnPropMapping.getPropertyName(), LocalDateTime.now());
      }
    }

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    for (String paramName : parameters) {
      if (paramName.equals("incrementedVersion")) {
        Integer versionVal = (Integer) bw.getPropertyValue(
            tableMapping.getVersionPropertyMapping().getPropertyName());
        if (versionVal == null) {
          throw new MapperException(obj.getClass().getSimpleName() + "."
              + tableMapping.getVersionPropertyMapping().getPropertyName()
              + " is configured with annotation @Version. Property "
              + tableMapping.getVersionPropertyMapping().getPropertyName()
              + " cannot be null when updating.");
        } else {
          mapSqlParameterSource.addValue("incrementedVersion", versionVal + 1,
              java.sql.Types.INTEGER);
        }
      } else {
        mapSqlParameterSource.addValue(paramName, bw.getPropertyValue(paramName),
            tableMapping.getPropertySqlType(paramName));
      }
    }

    int cnt = -1;
    // if object has property version the version gets incremented on update.
    // throws OptimisticLockingException when update fails.
    if (sqlAndParams.getParams().contains("incrementedVersion")) {
      cnt = npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
      if (cnt == 0) {
        throw new OptimisticLockingException(obj.getClass().getSimpleName()
            + " update failed due to stale data. Failed for " + tableMapping.getIdColumnName()
            + " = " + bw.getPropertyValue(tableMapping.getIdPropertyName()) + " and "
            + tableMapping.getVersionPropertyMapping().getColumnName() + " = "
            + bw.getPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName()));
      }
      // update the version in object with new version
      bw.setPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName(),
          mapSqlParameterSource.getValue("incrementedVersion"));
    } else {
      cnt = npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
    }

    return cnt;
  }

  /**
   * Deletes the object from the database.
   *
   * @param obj Object to be deleted
   * @return number of records were deleted (1 or 0)
   */
  public Integer delete(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

    String sql = "DELETE FROM " + tableMapping.fullyQualifiedTableName() + " WHERE "
        + tableMapping.getIdColumnNameForSql() + "= ?";
    BeanWrapper bw = getBeanWrapper(obj);
    Object id = bw.getPropertyValue(tableMapping.getIdPropertyName());
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Deletes the object from the database by id.
   *
   * @param clazz Type of object to be deleted.
   * @param id Id of object to be deleted
   * @return number records were deleted (1 or 0)
   */
  public Integer deleteById(Class<?> clazz, Object id) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(id, "id must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String sql = "DELETE FROM " + tableMapping.fullyQualifiedTableName() + " WHERE "
        + tableMapping.getIdColumnNameForSql() + " = ?";
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Gets a SelectMapper for the class and table alias.
   *
   * @param <T> the type for the SelectMapper
   * @param type the class
   * @param tableAlias the table alias used in the query.
   * @return the SelectMapper
   */
  public <T> SelectMapper<T> getSelectMapper(Class<T> type, String tableAlias) {
    return new SelectMapper<T>(type, tableAlias, mappingHelper, conversionService);
  }

  // internal use only
  <T> SelectMapper<T> getSelectMapperInternal(Class<T> type, String tableName, String columnAlias) {
    return new SelectMapper<T>(type, tableName, columnAlias, mappingHelper, conversionService);
  }

  /**
   * Get the column name of a property of the Model. Will return null if there is no corresponding
   * column for the property.
   *
   * @param clazz the class
   * @param propertyName the property name
   * @return the column name
   */
  public String getColumnName(Class<?> clazz, String propertyName) {
    return mappingHelper.getTableMapping(clazz).getColumnName(propertyName);
  }

  /**
   * returns a string which can be used in a sql select statement. The column alias will be the
   * underscore case name of property name, so it works well with JdbcTemplate's
   * BeanPropertyRowMapper
   *
   * <p>
   * Will return something like below if 'name' property is mapped to 'last_name':
   *
   * <pre>
   * "id as id, last_name as name"
   * </pre>
   * 
   * @param clazz the class
   * @return comma separated select column string
   * 
   */
  public String getBeanColumnsSql(Class<?> clazz) {
    return getBeanColumnsSqlInternal(mappingHelper.getTableMapping(clazz), clazz);
  }

  /**
   * Loads the mapping for a class. Model mappings are loaded when they are used for the first time.
   * This method is provided so that the mappings can be loaded during Spring application startup so
   * any mapping issues can be known at startup.
   *
   * @param clazz the class
   */
  public void loadMapping(Class<?> clazz) {
    mappingHelper.getTableMapping(clazz);
  }

  TableMapping getTableMapping(Class<?> clazz) {
    return mappingHelper.getTableMapping(clazz);
  }

  private SqlAndParams buildSqlAndParamsForUpdate(TableMapping tableMapping) {
    Assert.notNull(tableMapping, "tableMapping must not be null");

    // ignore these attributes when generating the sql 'SET' command
    List<String> ignoreAttrs = new ArrayList<>();
    ignoreAttrs.add(tableMapping.getIdPropertyName());
    PropertyMapping createdOnPropMapping = tableMapping.getCreatedOnPropertyMapping();
    if (createdOnPropMapping != null) {
      ignoreAttrs.add(createdOnPropMapping.getPropertyName());
    }
    PropertyMapping createdByPropMapping = tableMapping.getCreatedByPropertyMapping();
    if (createdByPropMapping != null) {
      ignoreAttrs.add(createdByPropMapping.getPropertyName());
    }

    Set<String> params = new HashSet<>();
    StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
    sqlBuilder.append(tableMapping.fullyQualifiedTableName());
    sqlBuilder.append(" SET ");

    PropertyMapping versionPropMapping = tableMapping.getVersionPropertyMapping();
    boolean first = true;
    for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
      if (ignoreAttrs.contains(propMapping.getPropertyName())) {
        continue;
      }
      if (!first) {
        sqlBuilder.append(", ");
      } else {
        first = false;
      }
      sqlBuilder.append(tableMapping.getIdentifierForSql(propMapping.getColumnName()));

      sqlBuilder.append(" = :");

      if (versionPropMapping != null
          && propMapping.getPropertyName().equals(versionPropMapping.getPropertyName())) {
        sqlBuilder.append("incrementedVersion");
        params.add("incrementedVersion");
      } else {
        sqlBuilder.append(propMapping.getPropertyName());
        params.add(propMapping.getPropertyName());
      }
    }

    // the where clause
    sqlBuilder.append(
        " WHERE " + tableMapping.getIdColumnNameForSql() + " = :" + tableMapping.getIdPropertyName());
    params.add(tableMapping.getIdPropertyName());
    if (versionPropMapping != null) {
      sqlBuilder.append(" AND ")
                .append(versionPropMapping.getColumnName())
                .append(" = :")
                .append(versionPropMapping.getPropertyName());
      params.add(versionPropMapping.getPropertyName());
    }

    String updateSql = sqlBuilder.toString();
    SqlAndParams updateSqlAndParams = new SqlAndParams(updateSql, params);

    return updateSqlAndParams;
  }

  private SqlAndParams buildSqlAndParamsForUpdateProperties(TableMapping tableMapping,
      String... propertyNames) {
    Assert.notNull(tableMapping, "tableMapping must not be null");
    Assert.notNull(propertyNames, "propertyNames must not be null");

    // do the validations
    for (String propertyName : propertyNames) {
      PropertyMapping propertyMapping = tableMapping.getPropertyMappingByPropertyName(propertyName);
      if (propertyMapping == null) {
        throw new MapperException("No mapping found for property '" + propertyName + "' in class "
            + tableMapping.getTableClassName());
      }

      // id property cannot be updated
      if (propertyMapping.isIdAnnotation()) {
        throw new MapperException("Id property " + tableMapping.getTableClassName() + "."
            + propertyName + " cannot be updated.");
      }

      // auto assign properties cannot be updated
      if (propertyMapping.isCreatedByAnnotation() || propertyMapping.isCreatedOnAnnotation()
          || propertyMapping.isUpdatedByAnnotation() || propertyMapping.isUpdatedOnAnnotation()
          || propertyMapping.isVersionAnnotation()) {
        throw new MapperException("Auto assign property " + tableMapping.getTableClassName() + "."
            + propertyName + " cannot be updated.");
      }
    }

    Set<String> params = new HashSet<>();
    StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
    sqlBuilder.append(tableMapping.fullyQualifiedTableName());
    sqlBuilder.append(" SET ");

    List<String> propertyList = new ArrayList<>(Arrays.asList(propertyNames));

    // handle auto assign properties for update
    PropertyMapping updatedOnPropMapping = tableMapping.getUpdatedOnPropertyMapping();
    if (updatedOnPropMapping != null) {
      propertyList.add(updatedOnPropMapping.getPropertyName());
    }
    PropertyMapping updatedByPropMapping = tableMapping.getUpdatedByPropertyMapping();
    if (updatedByPropMapping != null) {
      propertyList.add(updatedByPropMapping.getPropertyName());
    }
    PropertyMapping versionPropMapping = tableMapping.getVersionPropertyMapping();
    if (versionPropMapping != null) {
      propertyList.add(versionPropMapping.getPropertyName());
    }

    boolean first = true;
    for (String propertyName : propertyList) {
      PropertyMapping propMapping = tableMapping.getPropertyMappingByPropertyName(propertyName);
      if (!first) {
        sqlBuilder.append(", ");
      } else {
        first = false;
      }

      sqlBuilder.append(propMapping.getColumnNameForSql());
      sqlBuilder.append(" = :");

      if (versionPropMapping != null
          && propMapping.getPropertyName().equals(versionPropMapping.getPropertyName())) {
        sqlBuilder.append("incrementedVersion");
        params.add("incrementedVersion");
      } else {
        sqlBuilder.append(propMapping.getPropertyName());
        params.add(propMapping.getPropertyName());
      }
    }

    // the where clause
    sqlBuilder.append(
        " WHERE " + tableMapping.getIdColumnNameForSql() + " = :" + tableMapping.getIdPropertyName());
    params.add(tableMapping.getIdPropertyName());
    if (versionPropMapping != null) {
      sqlBuilder.append(" AND ")
                .append(versionPropMapping.getColumnName())
                .append(" = :")
                .append(versionPropMapping.getPropertyName());
      params.add(versionPropMapping.getPropertyName());
    }

    String updateSql = sqlBuilder.toString();
    SqlAndParams updateSqlAndParams = new SqlAndParams(updateSql, params);

    return updateSqlAndParams;
  }

  private <T> String getBeanColumnsSqlInternal(TableMapping tableMapping, Class<T> clazz) {
    String columnsSql = beanColumnsSqlCache.get(clazz.getName());
    if (columnsSql == null) {
      StringJoiner sj = new StringJoiner(", ", " ", " ");
      for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
        sj.add(propMapping.getColumnNameForSql() + " as "
            + MapperUtils.toUnderscoreName(propMapping.getPropertyName()));
      }
      columnsSql = sj.toString();
      beanColumnsSqlCache.put(clazz.getName(), columnsSql);
    }
    return columnsSql;
  }

  private BeanWrapper getBeanWrapper(Object obj) {
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    bw.setConversionService(conversionService);
    return bw;
  }

  private String tableNameForSimpleJdbcInsert(TableMapping tableMapping) {
    if (tableMapping.getSchemaName() != null) {
      return tableMapping.getTableName();
    } else {
      return tableMapping.fullyQualifiedTableName();
    }
  }

  // will return null when updateProperties property count is more than
  // CACHEABLE_UPDATE_PROPERTY_COUNT
  private String getUpdatePropertiesCacheKey(Object obj, String[] propertyNames) {
    if (propertyNames.length > CACHEABLE_UPDATE_PROPERTIES_COUNT) {
      return null;
    } else {
      return obj.getClass().getName() + "-" + String.join("-", propertyNames);
    }
  }

  SimpleCache<String, SimpleJdbcInsert> getInsertCache() {
    return insertCache;
  }

  SimpleCache<String, SqlAndParams> getUpdateCache() {
    return updateCache;
  }

  SimpleCache<String, SqlAndParams> getUpdatePropertiesCache() {
    return updatePropertiesCache;
  }

  SimpleCache<String, String> getBeanColumnsSqlCache() {
    return beanColumnsSqlCache;
  }

  SimpleCache<String, String> getQuerySqlCache() {
    return querySqlCache;
  }

  SimpleCache<String, String> getQueryMergeSqlCache() {
    return queryMergeSqlCache;
  }

  SimpleCache<String, String> getQueryCountSqlCache() {
    return queryCountSqlCache;
  }

}
