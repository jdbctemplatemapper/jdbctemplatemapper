package io.github.jdbctemplatemapper.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.exception.OptimisticLockingException;

/**
 * CRUD methods and configuration for JdbcTemplateMapper
 *
 * <pre>
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#jdbctemplatemapper">JdbcTemplateMapper documentation</a> for more info
 * </pre>
 *
 * <b> Note: An instance of JdbcTemplateMapper is thread safe.</b>
 *
 * @author ajoseph
 */
public final class JdbcTemplateMapper {
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate npJdbcTemplate;

  private final MappingHelper mappingHelper;
  private IRecordOperatorResolver recordOperatorResolver;

  // update sql cache
  // Map key - object class
  // value - the update sql details
  private Map<Class<?>, SqlAndParams> updateSqlAndParamsCache = new ConcurrentHashMap<>();

  // insert sql cache
  // Map key - object class
  // value - insert sql details
  private Map<Class<?>, SimpleJdbcInsert> simpleJdbcInsertCache = new ConcurrentHashMap<>();

  // the column sql string with column aliases for all properties of model for
  // find methods
  // Map key - object class
  // value - the column sql string
  private Map<Class<?>, String> findColumnsSqlCache = new ConcurrentHashMap<>();

  // JdbcTemplate uses this as its converter so use the same
  private DefaultConversionService conversionService =
      (DefaultConversionService) DefaultConversionService.getSharedInstance();

  // used for an attempt to support for old/non standard jdbc drivers.
  private boolean useColumnLabelForResultSetMetaData = true;

  /** @param jdbcTemplate the jdbcTemplate */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    this(jdbcTemplate, null, null, null);
  }

  /**
   * @param jdbcTemplate the jdbcTemplate
   * @param schemaName database schema name.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName) {
    this(jdbcTemplate, schemaName, null, null);
  }

  /**
   * @param jdbcTemplate the jdbcTemplate
   * @param schemaName database schema name.
   * @param catalogName database catalog name.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName) {
    this(jdbcTemplate, schemaName, catalogName, null);
  }

  /**
   * @param jdbcTemplate The jdbcTemplate
   * @param schemaName database schema name.
   * @param catalogName database catalog name.
   * @param metaDataColumnNamePattern For most jdbc drivers getting column metadata from database
   *        the metaDataColumnNamePattern argument of null will return all the columns (which is the
   *        default for JdbcTemplateMapper). Some jdbc drivers may require to pass something like
   *        '%'.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName,
      String metaDataColumnNamePattern) {

    Assert.notNull(jdbcTemplate, "jdbcTemplate must not be null");
    this.jdbcTemplate = jdbcTemplate;

    npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

    mappingHelper =
        new MappingHelper(jdbcTemplate, schemaName, catalogName, metaDataColumnNamePattern);
  }

  /**
   * Gets the JdbcTemplate of the jdbcTemplateMapper
   *
   * @return the JdbcTemplate
   */
  public JdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  /**
   * Gets the NamedParameterJdbcTemplate of the jdbcTemplateMapper
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
   * Get the schema name
   * 
   * @return the schema name
   */
  public String getSchemaName() {
    return mappingHelper.getSchemaName();
  }

  /**
   * Get the catalog name
   * 
   * @return the catalog name
   */
  public String getCatalogName() {
    return mappingHelper.getCatalogName();
  }

  /**
   * Exposing the conversion service used so if necessary new converters can be added
   *
   * @return the default conversion service.
   */
  public DefaultConversionService getConversionService() {
    return (DefaultConversionService) conversionService;
  }

  /**
   * Attempted Support for old/non standard jdbc drivers. For these drivers
   * resultSetMetaData,getcolumnLabel(int) info is in resultSetMetaData.getColumnName(int). When
   * this is the case set this value to false. default is true
   *
   * @param val boolean value
   */
  public void useColumnLabelForResultSetMetaData(boolean val) {
    this.useColumnLabelForResultSetMetaData = val;
  }

  /**
   * Some postgres drivers seem to not return the correct meta data for TIMESTAMP WITH TIMEZONE
   * fields. If this value is set to true it will force system to use
   * java.sql.Types.TIMESTAMP_WITH_TIMEZONE for properties of models which are of type
   * OffsetDateTime.
   *
   * @param val boolean value
   */
  public void forcePostgresTimestampWithTimezone(boolean val) {
    mappingHelper.forcePostgresTimestampWithTimezone(val);
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
    String columnsSql = getFindColumnsSql(tableMapping, clazz);
    String sql = "SELECT " + columnsSql + " FROM "
        + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " WHERE "
        + tableMapping.getIdColumnName() + " = ?";

    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);

    try {
      Object obj = jdbcTemplate.queryForObject(sql, mapper, id);
      return clazz.cast(obj);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  /**
   * Returns list of objects which match the property value.
   *
   * <pre>
   * Query is constructed
   * in such a way that if propertyValue is null it will return records in
   * database which have null values for the property
   * </pre>
   *
   * @param <T> the type
   * @param clazz Class of List of objects returned
   * @param propertyName the property name
   * @param propertyValue the value of property to query by
   * @return a List of objects of type T
   */
  public <T> List<T> findByProperty(Class<T> clazz, String propertyName, Object propertyValue) {
    return findByProperty(clazz, propertyName, propertyValue, null);
  }

  /**
   * Returns list of objects which match the propertyValue ordered by the orderedByProperty
   * ascending.
   *
   * <pre>
   * Query is constructed in such a way that if
   * propertyValue is null it will return records in database which have null
   * values for the property
   * </pre>
   *
   * @param <T> the type
   * @param clazz Class of List of objects returned
   * @param propertyName the property name
   * @param propertyValue the value of property to query by
   * @param orderByPropertyName the order by property name
   * @return a List of objects for type T
   */
  public <T> List<T> findByProperty(Class<T> clazz, String propertyName, Object propertyValue,
      String orderByPropertyName) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(propertyName, "propertyName must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String propColumnName = tableMapping.getColumnName(propertyName);
    if (propColumnName == null) {
      throw new MapperException(clazz.getSimpleName() + "." + propertyName
          + " is either invalid or does not have a corresponding column in database.");
    }

    String columnsSql = getFindColumnsSql(tableMapping, clazz);
    String sql = "SELECT " + columnsSql + " FROM "
        + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " WHERE "
        + propColumnName;

    if (propertyValue == null) {
      sql += " IS NULL";
    } else {
      sql += " = ?";
    }

    String orderByColumnName = null;
    if (orderByPropertyName != null) {
      orderByColumnName = tableMapping.getColumnName(orderByPropertyName);
      if (orderByColumnName == null) {
        throw new MapperException(
            "orderByPropertyName " + clazz.getSimpleName() + "." + orderByPropertyName
                + " is either invalid or does not have a corresponding column in database.");
      }
    }

    if (orderByColumnName != null) {
      sql += " ORDER BY " + orderByColumnName + " ASC";
    }

    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);

    if (propertyValue == null) {
      return jdbcTemplate.query(sql, mapper);
    } else {
      return jdbcTemplate.query(sql, mapper, propertyValue);
    }
  }

  /**
   * Find all objects
   *
   * @param <T> the type
   * @param clazz Type of object
   * @return List of objects of type T
   */
  public <T> List<T> findAll(Class<T> clazz) {
    return findAll(clazz, null);
  }

  /**
   * Find all objects ordered by orderByPropertyName ascending
   *
   * @param <T> the type
   * @param clazz Type of object
   * @param orderByPropertyName the order by property
   * @return List of objects of type T
   */
  public <T> List<T> findAll(Class<T> clazz, String orderByPropertyName) {
    Assert.notNull(clazz, "Class must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String columnsSql = getFindColumnsSql(tableMapping, clazz);

    String orderByColumnName = null;
    if (orderByPropertyName != null) {
      orderByColumnName = tableMapping.getColumnName(orderByPropertyName);
      if (orderByColumnName == null) {
        throw new MapperException(
            "orderByPropertyName " + clazz.getSimpleName() + "." + orderByPropertyName
                + " is either invalid or does not have a corresponding column in database.");
      }
    }

    String sql = "SELECT " + columnsSql + " FROM "
        + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName());

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
   * &#64;CreatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property will be assigned that value
   * &#64;UpdatedOn property will be assigned current date and time
   * &#64;UpdatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property will be assigned that value
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
      bw.setPropertyValue(versionPropMapping.getPropertyName(), 1);
    }

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    if (mappingHelper.getForcePostgresTimestampWithTimezone()) {
      // propertySqlTypes are included in this case.
      for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
        mapSqlParameterSource.addValue(propMapping.getColumnName(),
            bw.getPropertyValue(propMapping.getPropertyName()),
            tableMapping.getPropertySqlType(propMapping.getPropertyName()));
      }
    } else {
      for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
        mapSqlParameterSource.addValue(propMapping.getColumnName(),
            bw.getPropertyValue(propMapping.getPropertyName()));
      }
    }

    SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(obj.getClass());
    if (jdbcInsert == null) {
      if (tableMapping.isIdAutoIncrement()) {
        jdbcInsert =
            new SimpleJdbcInsert(jdbcTemplate).withCatalogName(mappingHelper.getCatalogName())
                .withSchemaName(mappingHelper.getSchemaName())
                .withTableName(tableMapping.getTableName())
                .usingGeneratedKeyColumns(tableMapping.getIdColumnName());
      } else {
        jdbcInsert =
            new SimpleJdbcInsert(jdbcTemplate).withCatalogName(mappingHelper.getCatalogName())
                .withSchemaName(mappingHelper.getSchemaName())
                .withTableName(tableMapping.getTableName());
      }
      simpleJdbcInsertCache.put(obj.getClass(), jdbcInsert);
    }

    if (tableMapping.isIdAutoIncrement()) {
      Number idNumber = jdbcInsert.executeAndReturnKey(mapSqlParameterSource);
      bw.setPropertyValue(tableMapping.getIdPropertyName(), idNumber); // set object id value
    } else {
      jdbcInsert.execute(mapSqlParameterSource);
    }
  }

  /**
   * Update the object.
   *
   * <pre>
   * Will handle the following annotations:
   * &#64;UpdatedOn property will be assigned current date and time
   * &#64;UpdatedBy if IRecordOperaterrResolver is configured with JdbcTemplateMapper the property will be assigned that value
   * &#64;Version property will be incremented on a successful update. An OptimisticLockingException will be thrown if object is stale.
   * </pre>
   *
   * @param obj object to be updated
   * @return number of records updated
   */
  public Integer update(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());
    SqlAndParams sqlAndParams = updateSqlAndParamsCache.get(obj.getClass());
    if (sqlAndParams == null) {
      sqlAndParams = buildSqlAndParamsForUpdate(tableMapping);
      updateSqlAndParamsCache.put(obj.getClass(), sqlAndParams);
    }

    BeanWrapper bw = getBeanWrapper(obj);
    Set<String> parameters = sqlAndParams.getParams();
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

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    for (String paramName : parameters) {
      if (paramName.equals("incrementedVersion")) {
        Integer versionVal = (Integer) bw
            .getPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName());
        if (versionVal == null) {
          throw new MapperException("JdbcTemplateMapper is configured for versioning so "
              + tableMapping.getVersionPropertyMapping().getPropertyName()
              + " cannot be null when updating " + obj.getClass().getSimpleName());
        } else {
          mapSqlParameterSource.addValue("incrementedVersion", versionVal + 1,
              java.sql.Types.INTEGER);
        }
      } else {
        mapSqlParameterSource.addValue(paramName, bw.getPropertyValue(paramName),
            tableMapping.getPropertySqlType(paramName));
      }
    }

    // if object has property version the version gets incremented on update.
    // throws OptimisticLockingException when update fails.
    if (sqlAndParams.getParams().contains("incrementedVersion")) {
      int cnt = npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
      if (cnt == 0) {
        throw new OptimisticLockingException("update failed for " + obj.getClass().getSimpleName()
            + " . " + tableMapping.getIdPropertyName() + ": "
            + bw.getPropertyValue(tableMapping.getIdPropertyName()) + " and "
            + tableMapping.getVersionPropertyMapping().getPropertyName() + ": "
            + bw.getPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName()));
      }
      // update the version in object with new version
      bw.setPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName(),
          mapSqlParameterSource.getValue("incrementedVersion"));
      return cnt;
    } else {
      return npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
    }
  }

  /**
   * Physically Deletes the object from the database
   *
   * @param obj Object to be deleted
   * @return number of records were deleted (1 or 0)
   */
  public Integer delete(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

    String sql = "DELETE FROM " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName())
        + " WHERE " + tableMapping.getIdColumnName() + "= ?";
    BeanWrapper bw = getBeanWrapper(obj);
    Object id = bw.getPropertyValue(tableMapping.getIdPropertyName());
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Physically Deletes the object from the database by id
   *
   * @param clazz Type of object to be deleted.
   * @param id Id of object to be deleted
   * @return number records were deleted (1 or 0)
   */
  public Integer deleteById(Class<?> clazz, Object id) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(id, "id must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String sql = "DELETE FROM " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName())
        + " WHERE " + tableMapping.getIdColumnName() + " = ?";
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
    return new SelectMapper<T>(type, tableAlias, mappingHelper, conversionService,
        useColumnLabelForResultSetMetaData);
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
  public String getColumnsSql(Class<?> clazz) {
    return getFindColumnsSql(mappingHelper.getTableMapping(clazz), clazz);
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

  String fullyQualifiedTableName(String tableName) {
    return mappingHelper.fullyQualifiedTableName(tableName);
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
    sqlBuilder.append(mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()));
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
      sqlBuilder.append(propMapping.getColumnName());
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
        " WHERE " + tableMapping.getIdColumnName() + " = :" + tableMapping.getIdPropertyName());
    params.add(tableMapping.getIdPropertyName());
    if (versionPropMapping != null) {
      sqlBuilder.append(" AND ").append(versionPropMapping.getColumnName()).append(" = :")
          .append(versionPropMapping.getPropertyName());
      params.add(versionPropMapping.getPropertyName());
    }

    String updateSql = sqlBuilder.toString();
    SqlAndParams updateSqlAndParams = new SqlAndParams(updateSql, params);

    return updateSqlAndParams;
  }

  private <T> String getFindColumnsSql(TableMapping tableMapping, Class<T> clazz) {
    String columnsSql = findColumnsSqlCache.get(clazz);
    if (columnsSql == null) {
      StringJoiner sj = new StringJoiner(", ", " ", " ");
      for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
        sj.add(propMapping.getColumnName() + " as "
            + MapperUtils.toUnderscoreName(propMapping.getPropertyName()));
      }
      columnsSql = sj.toString();
      findColumnsSqlCache.put(clazz, columnsSql);
    }
    return columnsSql;
  }

  private BeanWrapper getBeanWrapper(Object obj) {
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    bw.setConversionService(conversionService);
    return bw;
  }
}
