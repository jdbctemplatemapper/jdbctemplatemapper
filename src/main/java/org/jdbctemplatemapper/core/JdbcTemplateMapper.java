package org.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Spring's JdbcTemplate gives full control of data access using SQL. It removes a lot of the boiler
 * plate code which is required by JDBC. Unfortunately it is still verbose. JdbcTemplateMapper tries
 * to mitigate the verboseness. It is a utility class which uses JdbcTemplate, allowing for single
 * line CRUD and less verbose ways to query relationships.
 *
 * <p>IMPORTANT!!! JdbcTemplateMapper is a helper utility for JdbcTemplate and NOT a replacement for
 * it. Project code will generally be a mix of JdbcTemplate and JdbcTemplateMapper.
 *
 * <p><b>NOTE: An instance of this class is thread-safe once configured.</b>
 *
 * <pre>
 * Features:
 * 1) Simple CRUD one liners
 * 2) Methods to retrieve relationships (toOne..(), toMany..() etc)
 * 3) Can be configured to auto assign properties created on, updated on.
 * 4) Can be configured to auto assign properties created by, updated by using an
 *    implementation of IRecordOperatorResolver.
 * 5) Can be configured to provide optimistic locking functionality for updates using a version property.
 * 6) Tested against PostgreSQL, MySQL, Oracle, SQLServer
 *
 * JdbcTemplateMapper is opinionated. Projects have to meet the following 2 criteria to use it:
 * 1) Models should have a property named 'id' which has to be of type Integer or Long.
 * 2) Model property to table column mapping:
 *   Camel case property names are mapped to snake case database column names.
 *   Properties of a model like 'firstName', 'lastName' will be mapped to corresponding database columns
 *   first_name/FIRST_NAME and last_name/LAST_NAME in the database table. If you are using a
 *   case sensitive database setup and have mixed case column names like 'Order_Date' the tool won't work.
 *   (Model to table mapping does not have this restriction. By default a class maps to its snake case table name.
 *   The default class to table mapping can be overridden using the @Table annotation)
 *
 * Examples of simple CRUD:
 * 
 * // Product class maps to 'product' table by default. Use annotation @Table(name="someothertablename") to override the default
 * public class Product { 
 *    private Integer id; // 'id' property is needed for all models and has to be of type Integer or Long
 *    private String productName;
 *    private Double price;
 *    private LocalDateTime availableDate;
 *
 *    // for insert/update/find.. jdbcTemplateMapper will ignore properties which do not
 *    // have a corresponding snake case columns in database table
 *    private String someNonDatabaseProperty;
 *
 *    // getters and setters ...
 * }
 *
 * Product product = new Product();
 * product.setProductName("some product name");
 * product.setPrice(10.25);
 * product.setAvailableDate(LocalDateTime.now());
 * jdbcTemplateMapper.insert(product);
 *
 * product = jdbcTemplateMapper.findById(1, Product.class);
 * product.setPrice(11.50);
 * jdbcTemplateMapper.update(product);
 *
 * List<Product> products = jdbcTemplateMapper.findAll(Product.class);
 *
 * jdbcTemplateMapper.delete(product);
 *
 * See methods toOne..() and  toMany..() for relationship retrieval.
 *
 * Installation:
 * Requires Java8 or above.
 *
 * pom.xml dependencies
 * For a spring boot application:
 * {@code
 *  <dependency>
 *    <groupId>org.springframework.boot</groupId>
 *    <artifactId>spring-boot-starter-jdbc</artifactId>
 * </dependency>
 * }
 *
 * For a regular spring application:
 * {@code
 *  <dependency>
 *   <groupId>org.springframework</groupId>
 *   <artifactId>spring-jdbc</artifactId>
 *   <version>YourVersionOfSpringJdbc</version>
 *  </dependency>
 * }
 *
 * Spring bean configuration for JdbcTemplateMapper will look something like below:
 * (Assuming that org.springframework.jdbc.core.JdbcTemplate.JdbcTemplate is configured as per Spring instructions)
 *
 * &#64;Bean
 * public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
 *   return new JdbcTemplateMapper(jdbcTemplate, "your_database_schema_name");
 *   //return new JdbcTemplateMapper(jdbcTemplate, null); // for databases like mysql which do not have a schema name
 * }
 *
 * </pre>
 *
 * @author ajoseph
 */
public class JdbcTemplateMapper {
  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate npJdbcTemplate;
  private IRecordOperatorResolver recordOperatorResolver;

  private String catalogName;
  private String schemaName;
  //this is for the call to databaseMetaData.getColumns() just in case a database needs something other than null
  private String metaDataColumnNamePattern;  
  private String createdByPropertyName;
  private String createdOnPropertyName;
  private String updatedByPropertyName;
  private String updatedOnPropertyName;
  private String versionPropertyName;

  // Some old drivers use non compliant JDBC resultSet behavior where
  // resultSetMetaData.getColumnName()
  // retrieves the alias instead of resultSetMetaData.getColumnLabel()
  private boolean useOldAliasMetadataBehavior = false;

  // Need this for type conversions like java.sql.Timestamp to java.time.LocalDateTime etc
  private DefaultConversionService defaultConversionService = new DefaultConversionService();

  // to avoid query being issued with large number of ids
  // for the sql 'IN' clause the id list is chunked by this size
  // and multiple queries issued if needed.
  private static int IN_CLAUSE_CHUNK_SIZE = 100;

  // Convert camel case to snake case regex pattern. Pattern is thread safe
  private static Pattern TO_SNAKE_CASE_PATTERN = Pattern.compile("(.)(\\p{Upper})");

  // Inserts use SimpleJdbcInsert. Since SimpleJdbcInsert is thread safe, cache it
  // Map key - table name,
  //     value - SimpleJdcInsert object for the specific table
  private Map<String, SimpleJdbcInsert> simpleJdbcInsertCache = new ConcurrentHashMap<>();

  // update sql cache
  // Map key   - table name or sometimes tableName-updatePropertyName1-updatePropertyName2...
  //     value - the update sql
  private Map<String, UpdateSqlAndParams> updateSqlAndParamsCache = new ConcurrentHashMap<>();

  // Map key - table name,
  //     value - the list of database column names
  private Map<String, List<ColumnInfo>> tableColumnInfoCache = new ConcurrentHashMap<>();

  // Map key - simple Class name
  //     value - list of property names
  private Map<String, List<PropertyInfo>> objectPropertyInfoCache = new ConcurrentHashMap<>();

  // Map key - camel case string,
  //     value - snake case string
  private Map<String, String> camelToSnakeCache = new ConcurrentHashMap<>();

  // Map key - tableName-tableAlias
  //     value - the selectCols string
  private Map<String, String> selectColsCache = new ConcurrentHashMap<>();

  // Map key - object class name
  //     value - the table name
  private Map<String, TableMapping> objectToTableMappingCache = new ConcurrentHashMap<>();

  // Map key - object class name - propertyName
  //     value - The getter Method
  private Map<String, Method> objectGetMethodCache = new ConcurrentHashMap<>();

  // Map key - object class name - propertyName
  //     value - The setter Method
  private Map<String, Method> objectSetMethodCache = new ConcurrentHashMap<>();

  /**
   * The constructor.
   *
   * @param dataSource The dataSource for the mapper
   * @param schemaName schema name to be used by mapper
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName) {
    Assert.notNull(jdbcTemplate, "jdbcTemplate must not be null");

    this.jdbcTemplate = jdbcTemplate;
    this.schemaName = schemaName;
    this.npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  /**
   * Gets the JdbcTemplate used by the jdbcTemplateMapper
   *
   * @return the JdbcTemplate
   */
  public JdbcTemplate getJdbcTemplate() {
    return this.jdbcTemplate;
  }

  /**
   * Gets the NamedParameterJdbcTemplate used by the jdbcTemplateMapper
   *
   * @return the NamedParameterJdbcTemplate
   */
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
    return this.npJdbcTemplate;
  }

  /**
   * Assign this to identify the property name of created on field. This property has to be of type
   * LocalDateTime. When an object is inserted into the database the value of this field will be set
   * to current. Assign this while initializing jdbcTemplateMapper.
   *
   * @param propName : the created on property name.
   * @return The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withCreatedOnPropertyName(String propName) {
    this.createdOnPropertyName = propName;
    return this;
  }

  /**
   * The implementation of IRecordOperatorResolver is used to populate the created by and updated by
   * fields. Assign this while initializing the jdbcTemplateMapper
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
   * Assign this to identify the property name of the created by field. The created by property will
   * be assigned the value from recordOperatorResolver.getRecordOperator() when the object is
   * inserted into the database. Assign this while initializing the jdbcTemplateMapper
   *
   * @param propName : the created by property name.
   * @return The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withCreatedByPropertyName(String propName) {
    this.createdByPropertyName = propName;
    return this;
  }

  /**
   * Assign this to identify the property name of updated on field. This property has to be of type
   * LocalDateTime. When an object is updated in the database the value of this field will be set to
   * current. Assign this while initializing jdbcTemplateMapper.
   *
   * @param propName : the updated on property name.
   * @return The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withUpdatedOnPropertyName(String propName) {
    this.updatedOnPropertyName = propName;
    return this;
  }

  /**
   * Assign this to identify the property name of updated by field. The updated by property will be
   * assigned the value from recordOperatorResolver.getRecordOperator when the object is updated in
   * the database. Assign this while initializing the jdbcTemplateMapper
   *
   * @param propName : the update by property name.
   * @return The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withUpdatedByPropertyName(String propName) {
    this.updatedByPropertyName = propName;
    return this;
  }

  /**
   * The property used for optimistic locking. The property has to be of type Integer. If the object
   * has the version property name, on inserts it will be set to 1 and on updates it will
   * incremented by 1. Assign this while initializing jdbcTemplateMapper.
   *
   * @param propName The version propertyName
   * @return The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withVersionPropertyName(String propName) {
    this.versionPropertyName = propName;
    return this;
  }
  
  public void setCatalogName(String catalogName) {
	  this.catalogName = catalogName;
  }
  
  public void setMetaDataColumnNamePattern(String metaDataColumnNamePattern) {
	  this.metaDataColumnNamePattern = metaDataColumnNamePattern;
  }
  
  /**
   * Some old drivers use non compliant JDBC resultSet behavior where
   * resultSetMetaData.getColumnName() retrieves the alias instead of
   * resultSetMetaData.getColumnLabel()
   *
   * <p>For old drivers set this to true.
   *
   * @param val The value
   */
  public void useOldAliasMetadataBehavior(boolean val) {
    this.useOldAliasMetadataBehavior = val;
  }

  /**
   * Returns the object by Id. Return null if not found
   *
   * @param id Id of object
   * @param clazz Class of object
   * @param <T> the type of the object
   * @return the object of the specific type
   */
  public <T> T findById(Object id, Class<T> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    if (!(id instanceof Integer || id instanceof Long)) {
      throw new RuntimeException("id has to be type of Integer or Long");
    }

    TableMapping tableMapping = getTableMapping(clazz);
    String idColumnName = getTableIdColumnName(tableMapping);
    String sql =
        "SELECT * FROM "
            + fullyQualifiedTableName(tableMapping.getTableName())
            + " WHERE "
            + idColumnName
            + " = ?";
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    try {
      Object obj = jdbcTemplate.queryForObject(sql, mapper, id);
      return clazz.cast(obj);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  /**
   * Find all objects
   *
   * @param clazz Type of object
   * @param <T> the type of the objects
   * @return List of objects
   */
  public <T> List<T> findAll(Class<T> clazz) {
    Assert.notNull(clazz, "Class must not be null");

    String tableName = getTableMapping(clazz).getTableName();
    String sql = "SELECT * FROM " + fullyQualifiedTableName(tableName);
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  /**
   * Find all objects and order them using the order by clause passed as argument
   *
   * @param clazz Type of object
   * @param <T> the type of the objects
   * @param orderByClause The order by sql
   * @return List of objects
   */
  public <T> List<T> findAll(Class<T> clazz, String orderByClause) {
    Assert.notNull(clazz, "Class must not be null");

    String tableName = getTableMapping(clazz).getTableName();
    String sql = "SELECT * FROM " + fullyQualifiedTableName(tableName) + " " + orderByClause;
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  /**
   * Inserts an object whose id in database is auto increment. Note the 'id' has to be null for the
   * object to be inserted. Once inserted the object will have the id assigned.
   *
   * <p>Also assigns created by, created on, updated by, updated on, version if these properties
   * exist for the object and the JdbcTemplateMapper is configured for them.
   *
   * @param obj The object to be saved
   */
  public void insert(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    if (!bw.isReadableProperty("id")) {
      throw new RuntimeException(
          "Object " + obj.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    Object idValue = bw.getPropertyValue("id");
    if (idValue != null) {
      throw new RuntimeException(
          "For method insert() the objects 'id' property has to be null since this insert is for an object whose id is autoincrement in database.");
    }

    String tableName = getTableMapping(obj.getClass()).getTableName();
    LocalDateTime now = LocalDateTime.now();

    if (createdOnPropertyName != null && bw.isReadableProperty(createdOnPropertyName)) {
      bw.setPropertyValue(createdOnPropertyName, now);
    }

    if (createdByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(createdByPropertyName)) {
      bw.setPropertyValue(createdByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    if (updatedOnPropertyName != null && bw.isReadableProperty(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
      bw.setPropertyValue(versionPropertyName, 1);
    }

    Map<String, Object> attributes = convertToSnakeCaseAttributes(obj);

    SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(tableName);
    if (jdbcInsert == null) {
      jdbcInsert =
          new SimpleJdbcInsert(jdbcTemplate)
              .withCatalogName(catalogName)
              .withSchemaName(schemaName)
              .withTableName(tableName)
              .usingGeneratedKeyColumns("id");
      simpleJdbcInsertCache.put(tableName, jdbcInsert);
    }

    Number idNumber = jdbcInsert.executeAndReturnKey(attributes);
    bw.setPropertyValue("id", idNumber); // set auto increment id value on object
  }

  /**
   * Inserts an object whose id in database is NOT auto increment. In this case the object's 'id'
   * has to be assigned up front (using a sequence or some other way) and cannot be null.
   *
   * <p>Also assigns created by, created on, updated by, updated on, version if these properties
   * exist for the object and the JdbcTemplateMapper is configured for them.
   *
   * @param obj The object to be saved
   */
  public void insertWithId(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    if (!bw.isReadableProperty("id")) {
      throw new RuntimeException(
          "Object " + obj.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    Object idValue = bw.getPropertyValue("id");
    if (idValue == null) {
      throw new RuntimeException(
          "For method insertById() the objects 'id' property cannot be null.");
    }

    String tableName = getTableMapping(obj.getClass()).getTableName();
    LocalDateTime now = LocalDateTime.now();

    if (createdOnPropertyName != null && bw.isReadableProperty(createdOnPropertyName)) {
      bw.setPropertyValue(createdOnPropertyName, now);
    }
    if (createdByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(createdByPropertyName)) {
      bw.setPropertyValue(createdByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    if (updatedOnPropertyName != null && bw.isReadableProperty(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
      bw.setPropertyValue(versionPropertyName, 1);
    }

    SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(tableName);
    if (jdbcInsert == null) {
      jdbcInsert =
          new SimpleJdbcInsert(jdbcTemplate).withCatalogName(catalogName).withSchemaName(schemaName).withTableName(tableName);

      simpleJdbcInsertCache.put(tableName, jdbcInsert);
    }

    Map<String, Object> attributes = convertToSnakeCaseAttributes(obj);
    jdbcInsert.execute(attributes);
  }

  /**
   * Updates object. Assigns updated by, updated on if these properties exist for the object and the
   * jdbcTemplateMapper is configured for these fields. if 'version' property exists for object
   * throws an OptimisticLockingException if object is stale
   *
   * @param obj object to be updated
   * @return number of records updated
   */
  public Integer update(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = getTableMapping(obj.getClass());
    if (tableMapping.getIdName() == null) {
      throw new RuntimeException(
          "Object " + obj.getClass().getSimpleName() + " has to have a property named 'id'.");
    }
    UpdateSqlAndParams updateSqlAndParams = updateSqlAndParamsCache.get(obj.getClass().getName());

    if (updateSqlAndParams == null) {
      // ignore these attributes when generating the sql 'SET' command
      List<String> ignoreAttrs = new ArrayList<>();
      ignoreAttrs.add("id");
      if (createdByPropertyName != null) {
        ignoreAttrs.add(createdByPropertyName);
      }
      if (createdOnPropertyName != null) {
        ignoreAttrs.add(createdOnPropertyName);
      }

      Set<String> updatePropertyNames = new LinkedHashSet<>();

      for (PropertyInfo propertyInfo : getObjectPropertyInfo(obj)) {
        // if not a ignore property and has a table column mapping add it to the update property
        // list
        if (!ignoreAttrs.contains(propertyInfo.getPropertyName())
            && tableMapping.getColumnName(propertyInfo.getPropertyName()) != null) {
          updatePropertyNames.add(propertyInfo.getPropertyName());
        }
      }
      updateSqlAndParams = buildUpdateSql(tableMapping, updatePropertyNames);
      updateSqlAndParamsCache.put(obj.getClass().getName(), updateSqlAndParams);
    }
    return issueUpdate(updateSqlAndParams, obj);
  }

  /**
   * Updates the propertyNames (passed in as args) of the object. Assigns updated by, updated on if
   * these properties exist for the object and the jdbcTemplateMapper is configured for these
   * fields.
   *
   * @param obj object to be updated
   * @param propertyNames array of property names that should be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object obj, String... propertyNames) {
    Assert.notNull(obj, "Object must not be null");
    Assert.notNull(propertyNames, "propertyNames must not be null");

    TableMapping tableMapping = getTableMapping(obj.getClass());
    String tableName = tableMapping.getTableName();
    // cachekey ex: className-propertyName1-propertyName2
    String cacheKey = obj.getClass().getName() + "-" + String.join("-", propertyNames);
    UpdateSqlAndParams updateSqlAndParams = updateSqlAndParamsCache.get(cacheKey);
    if (updateSqlAndParams == null) {
      // check properties have a corresponding table column
      for (String propertyName : propertyNames) {
        if (tableMapping.getColumnName(propertyName) == null) {
          throw new RuntimeException(
              "property "
                  + propertyName
                  + " is not a property of object "
                  + obj.getClass().getName()
                  + " or does not have a corresponding column in table "
                  + tableName);
        }
      }

      // auto assigned  cannot be updated by user.
      List<String> autoAssignedAttrs = new ArrayList<>();
      autoAssignedAttrs.add("id");
      if (versionPropertyName != null && tableMapping.getColumnName(versionPropertyName) != null) {
        autoAssignedAttrs.add(versionPropertyName);
      }
      if (updatedOnPropertyName != null
          && tableMapping.getColumnName(updatedOnPropertyName) != null) {
        autoAssignedAttrs.add(updatedOnPropertyName);
      }
      if (updatedByPropertyName != null
          && recordOperatorResolver != null
          && tableMapping.getColumnName(updatedByPropertyName) != null) {
        autoAssignedAttrs.add(updatedByPropertyName);
      }

      for (String propertyName : propertyNames) {
        if (autoAssignedAttrs.contains(propertyName)) {
          throw new RuntimeException(
              "property "
                  + propertyName
                  + " is an auto assigned property which cannot be manually set in update statement");
        }
      }

      // add input properties to the update property list
      Set<String> updatePropertyNames = new LinkedHashSet<>();
      for (String propertyName : propertyNames) {
        updatePropertyNames.add(propertyName);
      }

      // add the auto assigned properties if configured and have table column mapping
      if (versionPropertyName != null && tableMapping.getColumnName(versionPropertyName) != null) {
        updatePropertyNames.add(versionPropertyName);
      }
      if (updatedOnPropertyName != null
          && tableMapping.getColumnName(updatedOnPropertyName) != null) {
        updatePropertyNames.add(updatedOnPropertyName);
      }
      if (updatedByPropertyName != null
          && recordOperatorResolver != null
          && tableMapping.getColumnName(updatedByPropertyName) != null) {
        updatePropertyNames.add(updatedByPropertyName);
      }

      updateSqlAndParams = buildUpdateSql(tableMapping, updatePropertyNames);
      updateSqlAndParamsCache.put(cacheKey, updateSqlAndParams);
    }
    return issueUpdate(updateSqlAndParams, obj);
  }

  /**
   * Physically Deletes the object from the database
   *
   * @param obj Object to be deleted
   * @return 0 if no records were deleted
   */
  public Integer delete(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    if (!bw.isReadableProperty("id")) {
      throw new RuntimeException(
          "Object " + obj.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    String tableName = getTableMapping(obj.getClass()).getTableName();

    String sql = "delete from " + fullyQualifiedTableName(tableName) + " where id = ?";
    Object id = bw.getPropertyValue("id");
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Physically Deletes the object from the database by id
   *
   * @param id Id of object to be deleted
   * @param clazz Type of object to be deleted.
   * @return 0 if no records were deleted
   */
  public <T> Integer deleteById(Object id, Class<T> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    if (!(id instanceof Integer || id instanceof Long)) {
      throw new RuntimeException("id has to be type of Integer or Long");
    }
    String tableName = getTableMapping(clazz).getTableName();
    String sql = "delete from " + fullyQualifiedTableName(tableName) + " where id = ?";
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Populates the toOne relationship of the main object. Issues an sql query to get the
   * relationship. Make sure the join property of the argument main object is assigned so it can be
   * matched to its corresponding relationship object (mainObj.mainObjJoinPropertyName =
   * relationshipObj.id)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   * toOne related Object:
   * public class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * 1) Get an Order Object using a query for example:
   * Order order = jdbcTemplateMapper.findById(orderId, Order.class);
   *
   * 2) Populate the Order's toOne customer property. This will issue an sql and populate order.customer
   * jdbcTemplateMapper.toOneForObject(order, Customer.class, "customer", "customerId");
   *
   * </pre>
   *
   * @param mainObj the main object
   * @param relationShipClazz The relationship class
   * @param mainObjRelationshipPropertyName The propertyName of the toOne relationship (on mainOjb)
   *     that needs to be populated.
   * @param mainObjJoinPropertyName the join property on main object.
   */
  public <T, U> void toOneForObject(
      T mainObj,
      Class<U> relationshipClazz,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {

    Assert.notNull(relationshipClazz, "relationshipClazz must not be null");
    Assert.notNull(
        mainObjRelationshipPropertyName, "mainObjRelationshipPropertyName must not be null");
    Assert.notNull(mainObjJoinPropertyName, "mainObjJoinPropertyName must not be null");

    if (mainObj != null) {
      List<T> mainObjList = new ArrayList<>();
      mainObjList.add(mainObj);
      toOneForList(
          mainObjList, relationshipClazz, mainObjRelationshipPropertyName, mainObjJoinPropertyName);
    }
  }

  /**
   * Populates the toOne relationship for all the main objects in the argument list. Issues an sql
   * query using the 'IN' clause to get all the relationship objects corresponding to the main
   * object list. Make sure the join property of the main object is assigned so it can be matched to
   * its corresponding relationship object (mainObj.mainObjJoinPropertyName = relationshipObj.id)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   * toOne related Object:
   * public class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * 1) Get list of orders using a query like below or a complex query using jdbcTemplate.
   * List<Order> orders = jdbcTemplateMapper.findAll(Order.class)
   *
   * 2) Populate each Order's toOne customer property. This will issue an sql to get the corresponding
   *    customers using an IN clause.
   * jdbcTemplateMapper.toOneForList(orders, Customer.class, "customer", "customerId");
   *
   * </pre>
   *
   * @param mainObjList list of main objects
   * @param relationShipClazz The relationship class
   * @param mainObjRelationshipPropertyName The toOne relationship property name on main object
   * @param mainObjJoinPropertyName the join property name on the main object.
   */
  public <T, U> void toOneForList(
      List<T> mainObjList,
      Class<U> relationshipClazz,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {

    Assert.notNull(relationshipClazz, "relationshipClazz must not be null");
    Assert.notNull(
        mainObjRelationshipPropertyName, "mainObjRelationshipPropertyName must not be null");
    Assert.notNull(mainObjJoinPropertyName, "mainObjJoinPropertyName must not be null");

    TableMapping relationshipTableMapping = getTableMapping(relationshipClazz);
    String relationshipTableName = relationshipTableMapping.getTableName();
    String idColumnName = getTableIdColumnName(relationshipTableMapping);

    if (isNotEmpty(mainObjList)) {
      LinkedHashSet<Long> allJoinPropertyIds = new LinkedHashSet<>();

      boolean firstRecord = true;
      for (T mainObj : mainObjList) {
        if (mainObj != null) {
          // Do some validations
          if (firstRecord) {
            firstRecord = false;
            validateToOne(
                mainObj,
                relationshipClazz,
                mainObjRelationshipPropertyName,
                mainObjJoinPropertyName);
          }

          // create the list fo all join property ids
          Number joinPropertyValue = (Number) getPropertyValue(mainObj, mainObjJoinPropertyName);
          if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
            allJoinPropertyIds.add(joinPropertyValue.longValue());
          }
        }
      }
      List<U> relatedObjList = new ArrayList<>();
      // Since some databases have limitations on how many entries can be in an 'IN' clause and to
      // avoid
      // query being issued with large number of ids for the 'IN (:joinPropertyIds), the list
      // is chunked by IN_CLAUSE_CHUNK_SIZE and multiple queries issued if needed.
      Collection<List<Long>> chunkedJoinPropertyIds =
          chunkTheCollection(allJoinPropertyIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Long> joinPropertyIds : chunkedJoinPropertyIds) {
        String sql =
            "SELECT * FROM "
                + fullyQualifiedTableName(relationshipTableName)
                + " WHERE "
                + idColumnName
                + " IN (:joinPropertyIds)";
        MapSqlParameterSource params =
            new MapSqlParameterSource("joinPropertyIds", joinPropertyIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(relationshipClazz);
        relatedObjList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      toOneMerge(
          mainObjList, relatedObjList, mainObjRelationshipPropertyName, mainObjJoinPropertyName);
    }
  }

  private void validateToOne(
      Object mainObj,
      Class<?> relationshipClazz,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {
    List<PropertyInfo> propertyInfoList = getObjectPropertyInfo(mainObj);

    if ("id".equals(mainObjRelationshipPropertyName)) {
      throw new RuntimeException("The argument mainObjRelationshipPropertyName cannot be 'id'.");
    }

    // validate mainObjRelationshipPropertyName
    PropertyInfo propertyInfo =
        propertyInfoList
            .stream()
            .filter(pi -> mainObjRelationshipPropertyName.equals(pi.getPropertyName()))
            .findAny()
            .orElse(null);

    if (propertyInfo == null) {
      throw new RuntimeException(
          "property "
              + mainObjRelationshipPropertyName
              + " does not exist for object "
              + mainObj.getClass().getName());
    } else {
      if (!relationshipClazz.equals(propertyInfo.getPropertyType())) {
        throw new RuntimeException(
            "type mismatch. property "
                + mainObjRelationshipPropertyName
                + " is of type "
                + propertyInfo.getPropertyType()
                + " while the argment relationshipClazz is "
                + relationshipClazz.getName());
      }
    }

    // validate mainObjJoinPropertyName
    if ("id".equals(mainObjJoinPropertyName)) {
      throw new RuntimeException("The argument mainObjJoinPropertyName cannot be 'id'.");
    }

    propertyInfo =
        propertyInfoList
            .stream()
            .filter(pi -> mainObjJoinPropertyName.equals(pi.getPropertyName()))
            .findAny()
            .orElse(null);

    if (propertyInfo == null) {
      throw new RuntimeException(
          "property " + mainObjJoinPropertyName + " not found in object " + mainObj);
    } else {
      if (!Integer.class.equals(propertyInfo.getPropertyType())
          && !Long.class.equals(propertyInfo.getPropertyType())) {
        throw new RuntimeException(
                "type of property " + mainObjJoinPropertyName
                + " which is used as a join property has to be of type Integer or Long ");
      }
    }
  }

  /**
   * Populates a single main object and its toOne related object with the data from the query ResultSet
   * using their respective SqlMappers. The sql for the Resultset object should have the join
   * properties in select statement so that the main object and related object can be tied together
   * (mainObj.mainObjJoinPropertyName = relatedObj.id)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   *
   * toOne related Object:
   * public class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * The sql below uses a column alias naming convention so that the SelectMapper() can use the column prefix
   * to populate the appropriated Objects from the selected columns.
   *
   * Note that the join properties  o.customer_id and the c.id have to be in select clause.
   * <pre>
   * select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id
   *        c.id c_id, c.first_name c_first_name, c.last_name c_last_name, c.address c_address
   * from order o
   * left join customer c on o.customer_id = c.id
   * where o.id = ?
   *
   * See selectCols()} on how to make the select clause less verbose.
   *
   * Example call to get Order and its Customer (toOne relationship) populated from the sql above:
   * Order order =
   *     jdbcTemplate.query(
   *         sql,
   *         new Object[] {theOrderId}, // args
   *         new int[] {java.sql.Types.INTEGER}, // arg types
   *         rs -> {
   *           return jdbcTemplateMapper.toOneMapperForObject(
   *               rs,
   *               new SelectMapper<Order>(Order.class, "o_"), // maps column names with prefix 'o_' to Order
   *               new SelectMapper<Customer>(Customer.class, "c_"), // maps column names with prefix 'c_' to Customer
   *               "customer",
   *               "customerId");
   *         });
   * </pre>
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper The main object mapper.
   * @param relatedObjMapper The related object mapper
   * @param mainObjRelationshipPropertyName The toOne relationship property name on main object
   * @param mainObjJoinPropertyName The join property name on the main object
   * @return The main object with its toOne relationship populated.
   */
  @SuppressWarnings("all")
  public <T, U> T toOneMapperForObject(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      SelectMapper<U> relatedObjMapper,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {

    Assert.notNull(mainObjMapper, "mainObjMapper must not be null");
    Assert.notNull(relatedObjMapper, "relatedObjMapper must not be null");
    Assert.notNull(
        mainObjRelationshipPropertyName, "mainObjRelationshipPropertyName must not be null");
    Assert.notNull(mainObjJoinPropertyName, "mainObjJoinPropertyName must not be null");

    List<T> list =
        toOneMapperForList(
            rs,
            mainObjMapper,
            relatedObjMapper,
            mainObjRelationshipPropertyName,
            mainObjJoinPropertyName);
    return isNotEmpty(list) ? list.get(0) : null;
  }

  /**
   * Populates the main object list with their corresponding toOne related object from the jdbc
   * ResultSet using their respective SqlMappers. The sql for the Resultset object should have the
   * join properties in select statement so that the main object and related object can be tied
   * together (mainObj.mainObjJoinPropertyName = relatedObj.id)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   *
   * toOne related Object:
   * public class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * The sql below uses a column alias naming convention so that the SelectMapper() can use the column prefix
   * to populate the appropriated Objects from the selected columns.
   *
   * Note that the join properties  o.customer_id and the c.id have to be in select clause.
   *
   * select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id
   *        c.id c_id, c.first_name c_first_name, c.last_name c_last_name, c.address c_address
   * from order o
   * left join customer c on o.customer_id = c.id
   *
   * See selectCols() to make the select clause less verbose.
   *
   * Example call to get Orders and its Customer (toOne relationship) populated from the sql above:
   * List<Order> orders =
   *     jdbcTemplate.query(
   *         sql,
   *         rs -> {
   *           return jdbcTemplateMapper.toOneMapperForList(
   *               rs,
   *               new SelectMapper<Order>(Order.class, "o_"), // maps column names with prefix 'o_' to Order
   *               new SelectMapper<Customer>(Customer.class, "c_"), // maps column names with prefix 'c_' to Customer
   *               "customer",
   *               "customerId");
   *         });
   * </pre>
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper The main object mapper.
   * @param relatedObjMapper The related object mapper
   * @param mainObjRelationshipPropertyName The toOne relationship property name on main object
   * @param mainObjJoinPropertyName The join property name on the main object
   * @return List of mainObj with its toOne property assigned
   */
  @SuppressWarnings("all")
  public <T, U> List<T> toOneMapperForList(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      SelectMapper<U> relatedObjMapper,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {

    Assert.notNull(mainObjMapper, "mainObjMapper must not be null");
    Assert.notNull(relatedObjMapper, "relatedObjMapper must not be null");
    Assert.notNull(
        mainObjRelationshipPropertyName, "mainObjRelationshipPropertyName must not be null");
    Assert.notNull(mainObjJoinPropertyName, "mainObjJoinPropertyName must not be null");

    Map<String, LinkedHashMap<Long, Object>> resultMap =
        multipleModelMapperRaw(rs, mainObjMapper, relatedObjMapper);
    List<T> mainObjList = new ArrayList(resultMap.get(mainObjMapper.getSqlColumnPrefix()).values());
    LinkedHashMap<Long, Object> relatedObjMap =
        resultMap.get(relatedObjMapper.getSqlColumnPrefix());
     
    // assign related obj to the main obj relationship property
    for (Object mainObj : mainObjList) {
      if (mainObj != null) {
        Number joinPropertyValue = (Number) getPropertyValue(mainObj, mainObjJoinPropertyName);
        if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {        	
          setPropertyValue(
              mainObj, mainObjRelationshipPropertyName, relatedObjMap.get(joinPropertyValue.longValue()));
        }
      }
    }

    return mainObjList;
  }

  /**
   * Merges relatedObjecList to the mainObj list by assigning
   * mainOjbj.mainObjRelationshipPropertyName with matching related objects ie
   * mainObj.mainObjJoinPropertyName = relatedObj.id
   *
   * @param mainObjList list of main objects
   * @param relatedObjList list of related objects
   * @param mainObjRelationshipPropertyName The toOne relationship property name on main object
   * @param mainObjJoinPropertyName The join property name on the main object
   */
  public <T, U> void toOneMerge(
      List<T> mainObjList,
      List<U> relatedObjList,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {

    Assert.notNull(
        mainObjRelationshipPropertyName, "mainObjRelationshipPropertyName must not be null");
    Assert.notNull(mainObjJoinPropertyName, "mainObjJoinPropertyName must not be null");

    if (isNotEmpty(mainObjList) && isNotEmpty(relatedObjList)) {
      // Map key: related object id , value: the related object
      Map<Long, U> idToObjectMap = new HashMap<>();
      for (U relatedObj : relatedObjList) {
        if (relatedObj != null) {
          Number idVal = (Number) getPropertyValue(relatedObj, "id");
          if (idVal != null && idVal.longValue() > 0) {
            idToObjectMap.put(idVal.longValue(), relatedObj);
          }
        }
      }
      // assign related obj to the main obj relationship property
      for (T mainObj : mainObjList) {
        if (mainObj != null) {
          Number joinPropertyValue = (Number) getPropertyValue(mainObj, mainObjJoinPropertyName);
          if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
            setPropertyValue(
                mainObj, mainObjRelationshipPropertyName, idToObjectMap.get(joinPropertyValue.longValue()));
          }
        }
      }
    }
  }

  /**
   * Populates the toMany relationship. Issues an sql query to get the many side records. Make sure
   * the id property of the argument main object is assigned so that it can be matched to its
   * corresponding many side object (mainObj.id = manySideObj.manySideJoinPropertyName)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * public class OrderLine{
   *   Integer id;
   *   Integer orderId;
   *   Integer productId;
   *   Integer quantity;
   *   Double price;
   * }
   *
   * 1) Get an Order Object using some query for example:
   * Order order = jdbcTemplateMapper.findById(orderId)
   *
   * 2) Populate the order's orderLines. This will issue an sql and populate order.orderLines
   * jdbcTemplateMapper.toManyForObject(order, OrderLine.class, "orderLines", "orderId");
   *
   * </pre>
   *
   * @param mainObjList the main object list
   * @param manySideClass The many side class
   * @param mainObjCollectionPropertyName The collection property name on main object
   * @param manySideJoinPropertyName The join property name on the many side object
   */
  public <T, U> void toManyForObject(
      T mainObj,
      Class<U> manySideClazz,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName) {

    Assert.notNull(manySideClazz, "manySideClazz must not be null");
    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    toManyForObject(
        mainObj, manySideClazz, mainObjCollectionPropertyName, manySideJoinPropertyName, null);
  }
  /**
   * Populates the toMany relationship. Issues an sql query to get the many side records. The many
   * side ordering can be customized using the last argument. Make sure the id property of the
   * argument main object is assigned so that it can be matched to its corresponding many side
   * object (mainObj.id = manySideObj.manySideJoinPropertyName)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * public class OrderLine{
   *   Integer id;
   *   Integer orderId;
   *   Integer productId;
   *   Integer quantity;
   *   Double price;
   * }
   *
   * 1) Get an Order Object using some query for example:
   * Order order = jdbcTemplateMapper.findById(orderId)
   *
   * 2) Populate the order's orderLines. This will issue an sql with the ordering clause and populate order.orderLines
   * jdbcTemplateMapper.toManyForObject(order, OrderLine.class, "orderLines", "orderId", "order by price");
   * </pre>
   *
   * @param mainObjList the main object list
   * @param manySideClass The many side class
   * @param mainObjCollectionPropertyName The collection property name on main object
   * @param manySideJoinPropertyName The join property name on the many side object
   * @param manySideOrderByClause The order by clause for the many side query
   */
  public <T, U> void toManyForObject(
      T mainObj,
      Class<U> manySideClazz,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName,
      String manySideOrderByClause) {

    Assert.notNull(manySideClazz, "manySideClazz must not be null");
    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    if (mainObj != null) {
      List<T> mainObjList = new ArrayList<>();
      mainObjList.add(mainObj);
      toManyForList(
          mainObjList,
          manySideClazz,
          mainObjCollectionPropertyName,
          manySideJoinPropertyName,
          manySideOrderByClause);
    }
  }

  /**
   * Populates the toMany relationship of each the main objects in the list. Issues an sql query to
   * get the many side records. Make sure the id property of the main objects are assigned so that
   * they can be matched to their corresponding many side objects (mainObj.id =
   * manySideObj.manySideJoinPropertyName)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * public class OrderLine{
   *   Integer id;
   *   Integer orderId;
   *   Integer productId;
   *   Integer quantity;
   *   Double price;
   * }
   *
   * 1) Get a list of Order objects using some query for example:
   * List<Order> orders = jdbcTemplateMapper.findAll(Order.class);
   *
   * 2) Populate each order's orderlines . This will issue an sql (with an IN clause) and
   *    populate order.orderLines.
   * jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "orderLines", "orderId");
   *
   * </pre>
   *
   * @param mainObjList the main object list
   * @param manySideClass The many side class
   * @param mainObjCollectionPropertyName The collection property name on mainObj
   * @param manySideJoinPropertyName the join property name on the many side object
   */
  public <T, U> void toManyForList(
      List<T> mainObjList,
      Class<U> manySideClazz,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName) {

    Assert.notNull(manySideClazz, "manySideClazz must not be null");
    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    toManyForList(
        mainObjList, manySideClazz, mainObjCollectionPropertyName, manySideJoinPropertyName, null);
  }

  /**
   * Populates the toMany relations of the list of main objects. Issues an sql query to get the many
   * side records. The many side ordering can be customized using the last argument. Make sure the
   * id property of the main objects are assigned so that they can be matched to their corresponding
   * many side objects (mainObj.id = manySideObj.manySideJoinPropertyName)
   *
   * <pre>
   * Main Object:
   * public class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * public class OrderLine{
   *   Integer id;
   *   Integer orderId;
   *   Integer productId;
   *   Integer quantity;
   *   Double price;
   * }
   *
   * 1) Get a list of Order objects using some query for example:
   * List<Order> orders = jdbcTemplateMapper.findAll(Order.class);
   *
   * 2) Populate each Order's orderLines . This will issue an sql with the ordering clause and populate order.orderLines.
   * jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "orderLines", "orderId", "order by price");
   *
   * </pre>
   *
   * @param mainObjList the main object list
   * @param manySideClass The many side class
   * @param mainObjCollectionPropertyName The collection property name on mainObj
   * @param manySideJoinPropertyName the join property name on the many side object
   * @param manySideOrderByClause The order by clause for the many side query
   */
  public <T, U> void toManyForList(
      List<T> mainObjList,
      Class<U> manySideClazz,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName,
      String manySideOrderByClause) {

    Assert.notNull(manySideClazz, "manySideClazz must not be null");
    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    String manySideTableName = getTableMapping(manySideClazz).getTableName();
    if (isNotEmpty(mainObjList)) {
      LinkedHashSet<Long> allMainObjIds = new LinkedHashSet<>();
      boolean firstRecord = true;
      for (T mainObj : mainObjList) {
        if (mainObj != null) {
          if (firstRecord) {
            validateToManyMainObj(mainObj, mainObjCollectionPropertyName);
            validateToManyManySideObj(manySideClazz, manySideJoinPropertyName);
            firstRecord = false;
          }

          Number idVal = (Number) getPropertyValue(mainObj, "id");
          if (idVal != null && idVal.longValue() > 0) {
            allMainObjIds.add(idVal.longValue());
          } else {
            throw new RuntimeException("id property in mainObjList cannot be null");
          }
        }
      }

      String joinColumnName = getJoinColumnName(manySideTableName, manySideJoinPropertyName);
      List<U> manySideList = new ArrayList<>();
      // Since some databases have limitations on how many entries can be in an 'IN' clause and to
      // avoid
      // query being issued with large number of ids for the 'IN (:mainObjIds), the list
      // is chunked by IN_CLAUSE_CHUNK_SIZE and multiple queries issued if needed.
      Collection<List<Long>> chunkedMainObjIds =
          chunkTheCollection(allMainObjIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Long> mainObjIds : chunkedMainObjIds) {
        String sql =
            "SELECT * FROM "
                + fullyQualifiedTableName(manySideTableName)
                + " WHERE "
                + joinColumnName
                + " IN (:mainObjIds)";
        if (isNotEmpty(manySideOrderByClause)) {
          sql += " " + manySideOrderByClause;
        }
        MapSqlParameterSource params = new MapSqlParameterSource("mainObjIds", mainObjIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(manySideClazz);
        manySideList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      toManyMerge(
          mainObjList, manySideList, mainObjCollectionPropertyName, manySideJoinPropertyName);
    }
  }

  private void validateToManyMainObj(Object mainObj, String mainObjCollectionPropertyName) {

    List<PropertyInfo> propertyInfoList = getObjectPropertyInfo(mainObj);

    if ("id".equals(mainObjCollectionPropertyName)) {
      throw new RuntimeException("The argument mainObjRelationshipPropertyName cannot be 'id'.");
    }

    // validate mainObjRelationshipPropertyName
    PropertyInfo propertyInfo =
        propertyInfoList
            .stream()
            .filter(pi -> mainObjCollectionPropertyName.equals(pi.getPropertyName()))
            .findAny()
            .orElse(null);

    if (propertyInfo == null) {
      throw new RuntimeException(
          "property "
              + mainObjCollectionPropertyName
              + " does not exist for object "
              + mainObj.getClass().getName());
    } else {
      if (!List.class.equals(propertyInfo.getPropertyType())) {
        throw new RuntimeException(
            "property " + mainObjCollectionPropertyName + " should be of type List.");
      }
    }
  }

  private void validateToManyManySideObj(Class<?> manySideClazz, String manySideJoinPropertyName) {
    List<PropertyInfo> propertyInfoList = null;
    try {
      propertyInfoList = getObjectPropertyInfo(manySideClazz.newInstance());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if ("id".equals(manySideJoinPropertyName)) {
      throw new RuntimeException("The argument manySideJoinPropertyName cannot be 'id'.");
    }

    PropertyInfo propertyInfo =
        propertyInfoList
            .stream()
            .filter(pi -> manySideJoinPropertyName.equals(pi.getPropertyName()))
            .findAny()
            .orElse(null);

    if (propertyInfo == null) {
      throw new RuntimeException(
          "property "
              + manySideJoinPropertyName
              + " does not exist for object "
              + manySideClazz.getName());
    } else {
      if (!Integer.class.equals(propertyInfo.getPropertyType())
          && !Long.class.equals(propertyInfo.getPropertyType())) {
          throw new RuntimeException(
                  "type of property " + manySideJoinPropertyName
                  + " which is used as a join property has to be of type Integer or Long ");
      }
    }
  }

  /**
   * Populates a single main object and its many side collection with the data from the ResultSet
   * using their respective SqlMappers.
   *
   * <p>The jdbc ResultSet should have mainObj.id and manySideObj.manySideJoinPropertyName so that
   * the mainObj.mainObjCollectionProperty can be assigned by matching mainObj.id to
   * manySideObj.manySideJoinPropertyName.
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper The main object mapper.
   * @param manySideObjMapper The many side object mapper
   * @param mainObjCollectionPropertyName the collectionPropertyName on the mainObj that needs to be
   *     populated
   * @param manySideJoinPropertyName the join property name on the manySide
   * @return The main object with its toMany relationship assigned.
   */
  @SuppressWarnings("all")
  public <T, U> T toManyMapperForObject(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      SelectMapper<U> manySideObjMapper,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName) {

    Assert.notNull(mainObjMapper, "mainObjMapper must not be null");
    Assert.notNull(manySideObjMapper, "manySideObjMapper must not be null");
    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    List<T> list =
        toManyMapperForList(
            rs,
            mainObjMapper,
            manySideObjMapper,
            mainObjCollectionPropertyName,
            manySideJoinPropertyName);
    return isNotEmpty(list) ? list.get(0) : null;
  }

  /**
   * Populates the main object list and their corresponding many side collections from the jdbc
   * ResultSet using their respective SqlMappers.
   *
   * <p>The jdbc ResultSet should have mainObj.id and manySideObj.manySideJoinPropertyName so that
   * the mainObj.mainObjCollectionProperty can be assigned by matching mainObj.id to
   * manySideObj.manySideJoinPropertyName.
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper The main object mapper
   * @param manySideObjMapper The many side object mapper
   * @param mainObjCollectionPropertyName the collectionPropertyName on the mainObj that needs to be
   *     populated
   * @param manySideJoinPropertyName the join property name on the manySide
   * @return List of mainObj with its collectionPropertyName populated
   */
  @SuppressWarnings("all")
  public <T, U> List<T> toManyMapperForList(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      SelectMapper<U> manySideObjMapper,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName) {

    Assert.notNull(mainObjMapper, "mainObjMapper must not be null");
    Assert.notNull(manySideObjMapper, "manySideObjMapper must not be null");
    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    Map<String, List> resultMap = multipleModelMapper(rs, mainObjMapper, manySideObjMapper);
    List<T> mainObjList = resultMap.get(mainObjMapper.getSqlColumnPrefix());
    List<U> manySideObjList = resultMap.get(manySideObjMapper.getSqlColumnPrefix());

    toManyMerge(
        mainObjList, manySideObjList, mainObjCollectionPropertyName, manySideJoinPropertyName);
    return mainObjList;
  }

  /**
   * Populates each main objects collectionPropertyName with the corresponding manySide objects by
   * matching manySide.joinPropertyName to mainObj.id
   *
   * @param mainObjList the main object list
   * @param manySideList the many side object list
   * @param mainObjCollectionPropertyName the collection property name of the main object that needs
   *     to be populated.
   * @param manySideJoinPropertyName the join property name on the many side
   */
  @SuppressWarnings("all")
  public <T, U> void toManyMerge(
      List<T> mainObjList,
      List<U> manySideList,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName) {

    Assert.notNull(mainObjCollectionPropertyName, "mainObjCollectionPropertyName must not be null");
    Assert.notNull(manySideJoinPropertyName, "manySideJoinPropertyName must not be null");

    try {
      if (isNotEmpty(mainObjList) && isNotEmpty(manySideList)) {
        // many side records are grouped by their join property values
        // Map key - join property value , value - List of records grouped by the join property value
        Map<Long, List<U>> groupedManySide = new HashMap<>();
        for (U manySideObj : manySideList) {
          if (manySideObj != null) {
            Number joinPropertyValue =
                (Number) getPropertyValue(manySideObj, manySideJoinPropertyName);
            if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
              if (groupedManySide.containsKey(joinPropertyValue.longValue())) {
                groupedManySide.get(joinPropertyValue.longValue()).add(manySideObj);
              } else {
                List<U> list = new ArrayList<>();
                list.add(manySideObj);
                groupedManySide.put(joinPropertyValue.longValue(), list);
              }
            }
          }
        }
        // assign the manyside list to the mainobj
        for (T mainObj : mainObjList) {
          if (mainObj != null) {
            Number idVal = (Number) getPropertyValue(mainObj, "id");
            if (idVal != null && idVal.longValue() > 0) {
              setPropertyValue(mainObj, mainObjCollectionPropertyName, groupedManySide.get(idVal.longValue()));
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns lists for each mapper passed in as an argument. The values in the list are UNIQUE and
   * in same order as the ResultSet values.
   *
   * <p>Returns a map. The keys in the map are the SqlMapper column prefixes.
   *
   * @param rs The jdbc result set
   * @param selectMappers array of sql mappers.
   * @return Map key: 'sqlColumnPrefix' of each sqlMapper, value: unique list for each sqlMapper
   */
  @SuppressWarnings("all")
  public Map<String, List> multipleModelMapper(ResultSet rs, SelectMapper... selectMappers) {
    Assert.notNull(selectMappers, "selectMappers must not be null");

    try {
      Map<String, LinkedHashMap<Long, Object>> tempMap =
          multipleModelMapperRaw(rs, selectMappers);
      Map<String, List> resultMap = new HashMap<>();
      for (String key : tempMap.keySet()) {
        resultMap.put(key, new ArrayList<Object>(tempMap.get(key).values()));
      }
      return resultMap;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("all")
  private Map<String, LinkedHashMap<Long, Object>> multipleModelMapperRaw(
      ResultSet rs, SelectMapper... selectMappers) {
    Assert.notNull(selectMappers, "selectMappers must not be null");

    try {
      // LinkedHashMap used to retain the order of insertion of records
      // Map key - sql column prefix
      // LinkedHashMap key - id of object
      // LinkedHashMap value - the object
      Map<String, LinkedHashMap<Long, Object>> resultMap = new HashMap<>();
      for (SelectMapper selectMapper : selectMappers) {
        resultMap.put(selectMapper.getSqlColumnPrefix(), new LinkedHashMap<>());
      }
      List<String> resultSetColumnNames = getResultSetColumnNames(rs);
      while (rs.next()) {
        for (SelectMapper selectMapper : selectMappers) {
          Number idVal = (Number) rs.getObject(selectMapper.getSqlColumnPrefix() + "id");
          if (idVal != null && idVal.longValue() > 0) {
            Object obj =
                newInstance(
                    selectMapper.getClazz(),
                    rs,
                    selectMapper.getSqlColumnPrefix(),
                    resultSetColumnNames);
            resultMap.get(selectMapper.getSqlColumnPrefix()).put(idVal.longValue(), obj);
          }
        }
      }

      return resultMap;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Generates a string which can be used in a sql select statement with the all columns of the
   * table.
   *
   * <pre>
   * Note: the string will have a comma as its last character.
   *
   * selectCols("employee", "emp") where "emp" is the alias will return something like:
   * "emp.id emp_id, emp.last_name emp_last_name, emp.first_name emp_first_name,"
   * </pre>
   *
   * @param tableName the Table name
   * @param tableAlias the alias being used in the sql statement for the table.
   * @return comma separated select column string with a comma at the end of string
   */
  public String selectCols(String tableName, String tableAlias) {
    Assert.hasLength(tableName, "tableName must not be empty");
    Assert.hasLength(tableAlias, "tableAlias must not be empty");
    return selectCols(tableName, tableAlias, true);
  }

  /**
   * Generates a string which can be used in a sql select statement with all the columns of the
   * table.
   *
   * <pre>
   * selectCols("employee", "emp", false) where "emp" is the alias will return something like:
   * "emp.id emp_id, emp.last_name emp_last_name, emp.first_name emp_first_name"
   * </pre>
   *
   * @param tableName the Table name
   * @param tableAlias the alias being used in the sql statement for the table.
   * @param includeLastComma if true last character will be comma; if false the string will have NO
   *     comma at end
   * @return comma separated select column string
   */
  public String selectCols(String tableName, String tableAlias, boolean includeLastComma) {
    Assert.hasLength(tableName, "tableName must not be empty");
    Assert.hasLength(tableAlias, "tableAlias must not be empty");

    String str = selectColsCache.get(tableName + "-" + tableAlias);

    if (str == null) {
      List<ColumnInfo> tableColumnInfo = getTableColumnInfo(tableName);
      if (isEmpty(tableColumnInfo)) {
        // try with uppercase table name
        tableColumnInfo = getTableColumnInfo(tableName.toUpperCase());
        if (isEmpty(tableColumnInfo)) {
          throw new RuntimeException("Could not get column info for table named " + tableName);
        }
      }
      StringBuilder sb = new StringBuilder(" ");
      for (ColumnInfo colInfo : tableColumnInfo) {
        sb.append(tableAlias)
            .append(".")
            .append(colInfo.getColumnName())
            .append(" ")
            .append(tableAlias)
            .append("_")
            .append(colInfo.getColumnName())
            .append(",");
      }
      str = sb.toString();
      selectColsCache.put(tableName + "-" + tableAlias, str);
    }

    if (!includeLastComma) {
      // remove the last comma.
      str = str.substring(0, str.length() - 1) + " ";
    }
    return str;
  }

  private UpdateSqlAndParams buildUpdateSql(TableMapping tableMapping, Set<String> propertyNames) {
    Assert.notNull(tableMapping, "tableMapping must not be null");
    Assert.notNull(propertyNames, "propertyNames must not be null");

    String idColumnName = tableMapping.getIdName();
    if (idColumnName == null) {
      throw new RuntimeException(
          "could not find id column for table " + tableMapping.getTableName());
    }
    Set<String> params = new HashSet<>();
    StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
    sqlBuilder.append(fullyQualifiedTableName(tableMapping.getTableName()));
    sqlBuilder.append(" SET ");

    String versionColumnName = tableMapping.getColumnName(versionPropertyName);
    boolean first = true;
    for (String propertyName : propertyNames) {
      String columnName = tableMapping.getColumnName(propertyName);
      if (columnName != null) {
        if (!first) {
          sqlBuilder.append(", ");
        } else {
          first = false;
        }
        sqlBuilder.append(columnName);
        sqlBuilder.append(" = :");

        if (versionPropertyName != null && columnName.equals(versionColumnName)) {
          sqlBuilder.append("incrementedVersion");
          params.add("incrementedVersion");
        } else {
          sqlBuilder.append(propertyName);
          params.add(propertyName);
        }
      }
    }

    // the where clause
    sqlBuilder.append(" WHERE " + idColumnName + " = :id");
    if (versionPropertyName != null && versionColumnName != null) {
      sqlBuilder
          .append(" AND ")
          .append(versionColumnName)
          .append(" = :")
          .append(versionPropertyName);
      params.add(versionPropertyName);
    }

    String updateSql = sqlBuilder.toString();
    UpdateSqlAndParams updateSqlAndParams = new UpdateSqlAndParams(updateSql, params);

    return updateSqlAndParams;
  }

  private Integer issueUpdate(UpdateSqlAndParams updateSqlAndParams, Object obj) {
    Assert.notNull(updateSqlAndParams, "sqlAndParams must not be null");
    Assert.notNull(obj, "Object must not be null");

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    Set<String> parameters = updateSqlAndParams.getParams();
    if (updatedOnPropertyName != null && parameters.contains(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, LocalDateTime.now());
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && parameters.contains(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }

    Map<String, Object> attributes = convertObjectToMap(obj);
    // if object has property version throw OptimisticLockingException
    // when update fails. The version gets incremented on update
    if (updateSqlAndParams.getParams().contains("incrementedVersion")) {
      Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + obj.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", versionVal + 1);
      }

      int cnt = npJdbcTemplate.update(updateSqlAndParams.getSql(), attributes);
      if (cnt == 0) {
        throw new OptimisticLockingException(
            "Update failed for "
                + obj.getClass().getSimpleName()
                + " for id:"
                + attributes.get("id")
                + " and "
                + versionPropertyName
                + ":"
                + attributes.get(versionPropertyName));
      }
      return cnt;
    } else {
      return npJdbcTemplate.update(updateSqlAndParams.getSql(), attributes);
    }
  }

  /**
   * Used by mappers to instantiate object from the result set
   *
   * @param clazz Class of object to be instantiated
   * @param rs Sql result set
   * @param prefix The sql column alias prefix in the query
   * @param resultSetColumnNames the column names in the sql statement.
   * @return Object of type T populated from the data in the result set
   */
  private <T> T newInstance(
      Class<T> clazz, ResultSet rs, String prefix, List<String> resultSetColumnNames) {

    Assert.notNull(clazz, "clazz must not be null");
    Assert.hasLength(prefix, "prefix must not be empty");
    Assert.notNull(resultSetColumnNames, "resultSetColumnNames must not be null");

    try {
      Object obj = clazz.newInstance();
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
      // need below for java.sql.Timestamp to java.time.LocalDateTime conversion etc
      bw.setConversionService(defaultConversionService);
      for (PropertyInfo propertyInfo : getObjectPropertyInfo(obj)) {
        String columnName = convertCamelToSnakeCase(propertyInfo.getPropertyName());
        if (isNotEmpty(prefix)) {
          columnName = prefix + columnName;
        }
        int index = resultSetColumnNames.indexOf(columnName.toLowerCase());
        if (index != -1) {
          // JDBC index starts at 1. using Springs JdbcUtils to handle oracle.sql.Timestamp ....
          bw.setPropertyValue(
              propertyInfo.getPropertyName(), JdbcUtils.getResultSetValue(rs, index + 1));
        }
      }
      return clazz.cast(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts an object to a map with key as database column names and values the corresponding
   * object value. Camel case property names are converted to snake case. For example property name
   * 'userLastName' will get converted to map key 'user_last_name' and assigned the corresponding
   * object value.
   *
   * @param obj The object to convert
   * @return A map with keys that are in snake case to match database column names and values
   *     corresponding to the object property
   */
  private Map<String, Object> convertToSnakeCaseAttributes(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    Map<String, Object> camelCaseAttrs = convertObjectToMap(obj);
    Map<String, Object> snakeCaseAttrs = new HashMap<>();
    for (String key : camelCaseAttrs.keySet()) {
      // ex: lastName will get converted to last_name
      String snakeCaseKey = convertCamelToSnakeCase(key);
      snakeCaseAttrs.put(snakeCaseKey, camelCaseAttrs.get(key));
    }
    return snakeCaseAttrs;
  }

  /**
   * Converts an object to a Map. The map key will be object property name and value with
   * corresponding object property value.
   *
   * @param obj The object to be converted.
   * @return Map with key: property name, value: object value
   */
  private Map<String, Object> convertObjectToMap(Object obj) {
    Assert.notNull(obj, "Object must not be null");
    Map<String, Object> camelCaseAttrs = new HashMap<>();
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    for (PropertyInfo propertyInfo : getObjectPropertyInfo(obj)) {
      camelCaseAttrs.put(
          propertyInfo.getPropertyName(), bw.getPropertyValue(propertyInfo.getPropertyName()));
    }
    return camelCaseAttrs;
  }

  private List<ColumnInfo> getTableColumnInfo(String tableName) {
    Assert.hasLength(tableName, "tableName must not be empty");
    try {
      List<ColumnInfo> columnInfos = tableColumnInfoCache.get(tableName);
      if (isEmpty(columnInfos)) {
        // Using Spring JdbcUtils.extractDatabaseMetaData() since it has some robust processing for
        // metadata access
        columnInfos =
            JdbcUtils.extractDatabaseMetaData(
                jdbcTemplate.getDataSource(),
                new DatabaseMetaDataCallback<List<ColumnInfo>>() {
                  public List<ColumnInfo> processMetaData(DatabaseMetaData metadata)
                      throws SQLException, MetaDataAccessException {
                    ResultSet rs = null;
                    try {
                      List<ColumnInfo> columnInfoList = new ArrayList<>();
                      rs = metadata.getColumns(catalogName, schemaName, tableName, metaDataColumnNamePattern);
                      while (rs.next()) {
                        columnInfoList.add(
                            new ColumnInfo(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE")));
                      }
                      if (isNotEmpty(columnInfoList)) {
                        tableColumnInfoCache.put(tableName, columnInfoList);
                      }
                      return columnInfoList;
                    } finally {
                      JdbcUtils.closeResultSet(rs);
                    }
                  }
                });
      }
      return columnInfos;
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  /**
   * Gets the resultSet lower case column names ie the column names in the 'select' statement of the
   * sql
   *
   * @param rs The jdbc ResultSet
   * @return List of select column names in lower case
   */
  private List<String> getResultSetColumnNames(ResultSet rs) {
    List<String> rsColNames = new ArrayList<>();
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numberOfColumns = rsmd.getColumnCount();
      // jdbc indexes start at 1
      for (int i = 1; i <= numberOfColumns; i++) {
        if (useOldAliasMetadataBehavior) {
          rsColNames.add(rsmd.getColumnName(i).toLowerCase());
        } else {
          rsColNames.add(rsmd.getColumnLabel(i).toLowerCase());
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return rsColNames;
  }

  /**
   * Get property names of an object. The property names are cached by the object class name
   *
   * @param obj the java object
   * @return List of property names.
   */
  private List<PropertyInfo> getObjectPropertyInfo(Object obj) {
    Assert.notNull(obj, "Object must not be null");
    List<PropertyInfo> propertyInfoList = objectPropertyInfoCache.get(obj.getClass().getName());
    if (propertyInfoList == null) {
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
      propertyInfoList = new ArrayList<>();
      PropertyDescriptor[] propertyDescriptors = bw.getPropertyDescriptors();
      for (PropertyDescriptor pd : propertyDescriptors) {
        String propName = pd.getName();
        // log.debug("Property name:{}" + propName);
        if ("class".equals(propName)) {
          continue;
        } else {
          propertyInfoList.add(new PropertyInfo(propName, pd.getPropertyType()));
        }
      }
      objectPropertyInfoCache.put(obj.getClass().getName(), propertyInfoList);
    }
    return propertyInfoList;
  }

  private Object getPropertyValue(Object obj, String propertyName) {
    Assert.notNull(obj, "Object must not be null");
    Assert.hasLength(propertyName, "propertyName must not be empty");

    Method method = objectGetMethodCache.get(obj.getClass().getName() + "-" + propertyName);
    if (method == null) {
      method =
          ReflectionUtils.findMethod(obj.getClass(), "get" + StringUtils.capitalize(propertyName));
      if (method == null) {
        throw new RuntimeException(
            "method get"
                + StringUtils.capitalize(propertyName)
                + "() not found in object "
                + obj.getClass().getName());
      }
      objectGetMethodCache.put(obj.getClass().getName() + "-" + propertyName, method);
    }
    return ReflectionUtils.invokeMethod(method, obj);
  }

  private Object setPropertyValue(Object obj, String propertyName, Object val) {
    Assert.notNull(obj, "Object must not be null");
    Assert.hasLength(propertyName, "propertyName must not be empty");

    Method method = objectSetMethodCache.get(obj.getClass().getName() + "-" + propertyName);
    if (method == null) {
      Field field = ReflectionUtils.findField(obj.getClass(), propertyName);
      if (field == null) {
        throw new RuntimeException(
            "property " + propertyName + " not found in class " + obj.getClass().getName());
      }
      method =
          ReflectionUtils.findMethod(
              obj.getClass(), "set" + StringUtils.capitalize(propertyName), field.getType());
      if (method == null) {
        throw new RuntimeException(
            "method set"
                + StringUtils.capitalize(propertyName)
                + "("
                + field.getType().getName()
                + ") not found in object "
                + obj.getClass().getName());
      }
      objectSetMethodCache.put(obj.getClass().getName() + "-" + propertyName, method);
    }
    return ReflectionUtils.invokeMethod(method, obj, val);
  }

  private TableMapping getTableMapping(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    TableMapping tableMapping = objectToTableMappingCache.get(clazz.getName());

    if (tableMapping == null) {
      String tableName = null;
      if (clazz.isAnnotationPresent(Table.class)) {
        // @Table annotation is present. Get the table name
        Table table = clazz.getAnnotation(Table.class);
        tableName = table.name();
      } else {
        tableName = convertCamelToSnakeCase(clazz.getSimpleName());
      }

      List<ColumnInfo> columnInfoList = getTableColumnInfo(tableName);
      if (isEmpty(columnInfoList)) {
        // try with uppercase
        tableName = tableName.toUpperCase();
        columnInfoList = getTableColumnInfo(tableName);
        if (isEmpty(columnInfoList)) {
          throw new RuntimeException(
              "Could not find corresponding table for class " + clazz.getSimpleName());
        }
      }

      // if code reaches here table exists
      try {
        List<PropertyInfo> propertyInfoList = getObjectPropertyInfo(clazz.newInstance());
        List<PropertyMapping> propertyMappings = new ArrayList<>();
        // Match  database table columns to the Object properties
        for (ColumnInfo columnInfo : columnInfoList) {
          // property name corresponding to column name
          String propertyName = convertSnakeToCamelCase(columnInfo.getColumnName());
          // check for match
          PropertyInfo propertyInfo =
              propertyInfoList
                  .stream()
                  .filter(pi -> propertyName.equals(pi.getPropertyName()))
                  .findAny()
                  .orElse(null);

          // add matched object property info and table column info to mappings
          if (propertyInfo != null) {
            propertyMappings.add(
                new PropertyMapping(
                    propertyInfo.getPropertyName(),
                    propertyInfo.getPropertyType(),
                    columnInfo.getColumnName(),
                    columnInfo.getColumnDataType()));
          }
        }

        tableMapping = new TableMapping();
        tableMapping.setTableName(tableName);
        tableMapping.setPropertyMappings(propertyMappings);

        // get the case sensitive id name
        for (ColumnInfo columnInfo : columnInfoList) {
          if ("id".equals(columnInfo.getColumnName()) || "ID".equals(columnInfo.getColumnName())) {
            tableMapping.setIdName(columnInfo.getColumnName());
            break;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    objectToTableMappingCache.put(clazz.getName(), tableMapping);
    return tableMapping;
  }

  private String getTableIdColumnName(TableMapping tableMapping) {
    Assert.notNull(tableMapping, "tableMapping must not be null");
    String idName = tableMapping.getIdName();
    if (isEmpty(idName)) {
      throw new RuntimeException(
          "Could not find id column for table" + tableMapping.getTableName());
    }
    return idName;
  }

  private String getJoinColumnName(String tableName, String joinPropertyName) {
    Assert.hasLength(tableName, "tableName must not be empty");
    Assert.hasLength(joinPropertyName, "joinPropertyName must not be empty");

    List<ColumnInfo> columnInfoList = getTableColumnInfo(tableName);

    String joinColumnName = convertCamelToSnakeCase(joinPropertyName);
    String ucJoinColumnName = joinColumnName.toUpperCase();
    for (ColumnInfo columnInfo : columnInfoList) {
      if (joinColumnName.equals(columnInfo.getColumnName())
          || ucJoinColumnName.equals(columnInfo.getColumnName())) {
        return columnInfo.getColumnName();
      }
    }
    // if code reached here throw exception
    throw new RuntimeException(
        "Could not find corresponding column in table "
            + tableName
            + " for joinPropertyName "
            + joinPropertyName);
  }

  /**
   * Converts camel case to snake case. Ex: userLastName gets converted to user_last_name. The
   * conversion info is cached.
   *
   * @param str camel case String
   * @return the snake case string to lower case.
   */
  private String convertCamelToSnakeCase(String str) {
    String snakeCase = camelToSnakeCache.get(str);
    if (snakeCase == null) {
      if (str != null) {
        snakeCase = TO_SNAKE_CASE_PATTERN.matcher(str).replaceAll("$1_$2").toLowerCase();
        camelToSnakeCache.put(str, snakeCase);
      }
    }
    return snakeCase;
  }

  /**
   * Converts snake case to camel case. Ex: user_last_name gets converted to userLastName. The
   * conversion info is cached.
   *
   * @param str snake case string
   * @return the camel case string
   */
  private String convertSnakeToCamelCase(String str) {
    return JdbcUtils.convertUnderscoreNameToPropertyName(str);
  }

  /**
   * Splits the list into multiple lists of chunk size. Used to split the sql IN clauses since some
   * databases have a limitation of 1024. We set the chuck size to IN_CLAUSE_CHUNK_SIZE
   *
   * @param list
   * @param chunkSize
   * @return Collection of lists broken down by chunkSize
   */
  private Collection<List<Long>> chunkTheCollection(
      Collection<Long> collection, Integer chunkSize) {
    Assert.notNull(collection, "collection must not be null");
    AtomicInteger counter = new AtomicInteger();
    Collection<List<Long>> result =
        collection
            .stream()
            .filter(e -> e != null)
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values();
    return result;
  }

  private String fullyQualifiedTableName(String tableName) {
    Assert.hasLength(tableName, "tableName must not be empty");
    if (isNotEmpty(schemaName)) {
      return schemaName + "." + tableName;
    }
    return tableName;
  }

  private boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  private boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  @SuppressWarnings("all")
  private boolean isEmpty(Collection coll) {
    return (coll == null || coll.isEmpty());
  }

  @SuppressWarnings("all")
  private boolean isNotEmpty(Collection coll) {
    return !isEmpty(coll);
  }
}
