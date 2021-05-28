package org.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.ObjectUtils;

import com.microsoft.sqlserver.jdbc.StringUtils;

/**
 * When using ORMs (like Hibernate etc) in a project, during the early stages they seem beneficial.
 * As the project grows to a non trivial size and complexity (most enterprise applications do) their
 * magic ends up getting in the way. As time goes on you start fighting it and their complexity and
 * nuances start bubbling up. The SQL they generate are cryptic which makes troubleshooting
 * challenging. Writing dynamic queries using the non intuitive JPA Criteria API is complex and
 * verbose so projects resort to using some other tools like QueryDSL, which has its own issues.
 * Tools like QueryDSL have their own learning curve and are generally code generates where the
 * generated code needs to be checked in, which is just Project smell. Now you have gone down a
 * rabbit hole which is difficult to back out off. All for the reason because someone alergic to SQL
 * and is religious about the Object/Relational abstraction. I have not talked about the performance
 * issues of ORM and the level of expertise to resolve them.
 *
 * <p>Spring's JdbcTemplate gives full control of data access using SQL. It removes a lot of the
 * boiler plate code which is required by JDBC. Unfortunately it is verbose. JdbcTemplateMapper
 * tries to mitigate the verboseness. It is a utility class which uses JdbcTemplate, allowing for
 * single line CRUD and less verbose ways to query relationships.
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
 *
 * JdbcTemplateMapper is opinionated. Projects have to meet the following 2 criteria to use it:
 * 1) Models should have a property named 'id' which has to be of type Integer or Long.
 * 2) Model property to table column mapping:
 *   Camel case property names are mapped to snake case database column names.
 *   Properties of a model like 'firstName', 'lastName' will be mapped to corresponding database columns
 *   first_name/FIRST_NAME and last_name/LAST_NAME in the database table. If you are using a
 *   case sensitive database setup and have mixed case column names like 'Order_Date' the tool wont work.
 *   (Model to table mapping does not have this restriction. By default a class maps to its snake case table name.
 *   The default class to table mapping can be overridden using the @Table annotation)
 *
 * Examples of simple CRUD:
 *
 * public class Product{ // by default maps to table 'product'
 *    private Integer id;
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
 *   return new JdbcTemplateMapper(jdbcTemplate, "yourSchemaName");
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

  private String schemaName;
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
  private Map<String, SqlPair> updateSqlCache = new ConcurrentHashMap<>();

  // Map key - table name,
  //     value - the list of database column names
  private Map<String, List<String>> tableColumnNamesCache = new ConcurrentHashMap<>();

  // Map key - simple Class name
  //     value - list of property names
  private Map<String, List<String>> objectPropertyNamesCache = new ConcurrentHashMap<>();

  // Map key - snake case string,
  //     value - camel case string
  private Map<String, String> snakeToCamelCache = new ConcurrentHashMap<>();

  // Map key - camel case string,
  //     value - snake case string
  private Map<String, String> camelToSnakeCache = new ConcurrentHashMap<>();

  // Map key - tableName-tableAlias
  //     value - the selectCols string
  private Map<String, String> selectColsCache = new ConcurrentHashMap<>();

  // Map key - object class name
  //     value - the table name
  private Map<String, String> objectToTableCache = new ConcurrentHashMap<>();

  // Map key - object class name
  //     value - the table name
  private Map<String, String> tableToCaseSensitiveIdName = new ConcurrentHashMap<>();

  /**
   * The constructor.
   *
   * @param dataSource The dataSource for the mapper
   * @param schemaName schema name to be used by mapper
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName) {
    if (jdbcTemplate == null) {
      throw new IllegalArgumentException("dataSource cannot be null");
    }
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
    if (!(id instanceof Integer || id instanceof Long)) {
      throw new IllegalArgumentException("id has to be type of Integer or Long");
    }
    String tableName = getTableName(clazz);
    String idColumnName = getTableIdColumnName(tableName);
    String sql =
        "SELECT * FROM " + fullyQualifiedTableName(tableName) + " WHERE " + idColumnName + " = ?";
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
    String tableName = getTableName(clazz);
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
    String tableName = getTableName(clazz);
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
   * @param pojo The object to be saved
   */
  public void insert(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    if (!bw.isReadableProperty("id")) {
      throw new IllegalArgumentException(
          "Object " + pojo.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    Object idValue = bw.getPropertyValue("id");
    if (idValue != null) {
      throw new RuntimeException(
          "For method insert() the objects 'id' property has to be null since this insert is for an object whose id is autoincrement in database.");
    }

    String tableName = getTableName(pojo.getClass());
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

    Map<String, Object> attributes = convertToSnakeCaseAttributes(pojo);

    SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(tableName);
    if (jdbcInsert == null) {
      jdbcInsert =
          new SimpleJdbcInsert(jdbcTemplate)
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
   * @param pojo The object to be saved
   */
  public void insertWithId(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    if (!bw.isReadableProperty("id")) {
      throw new IllegalArgumentException(
          "Object " + pojo.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    Object idValue = bw.getPropertyValue("id");
    if (idValue == null) {
      throw new RuntimeException(
          "For method insertById() the objects 'id' property cannot be null.");
    }

    String tableName = getTableName(pojo.getClass());
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
          new SimpleJdbcInsert(jdbcTemplate).withSchemaName(schemaName).withTableName(tableName);

      simpleJdbcInsertCache.put(tableName, jdbcInsert);
    }

    Map<String, Object> attributes = convertToSnakeCaseAttributes(pojo);
    jdbcInsert.execute(attributes);
  }

  /**
   * Updates object. Assigns updated by, updated on if these properties exist for the object and the
   * jdbcTemplateMapper is configured for these fields. if 'version' property exists for object
   * throws an OptimisticLockingException if object is stale
   *
   * @param pojo object to be updated
   * @return number of records updated
   */
  public Integer update(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    if (!bw.isReadableProperty("id")) {
      throw new IllegalArgumentException(
          "Object " + pojo.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    String tableName = getTableName(pojo.getClass());
    SqlPair sqlPair = updateSqlCache.get(tableName);
    if (sqlPair == null) {
      sqlPair = buildUpdateSql(pojo);
    }

    Set<String> parameters = sqlPair.getParams();
    if (parameters.contains(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, LocalDateTime.now());
    }
    if (parameters.contains(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    Map<String, Object> attributes = convertObjectToMap(pojo);
    // if object has property version throw OptimisticLockingException
    // when update fails. The version gets incremented on update
    if (parameters.contains(versionPropertyName)) {
      Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + pojo.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", versionVal + 1);
      }

      int cnt = npJdbcTemplate.update(sqlPair.getSql(), attributes);
      if (cnt == 0) {
        throw new OptimisticLockingException(
            "Update failed for "
                + pojo.getClass().getSimpleName()
                + " for id:"
                + attributes.get("id")
                + " and "
                + versionPropertyName
                + ":"
                + attributes.get(versionPropertyName));
      }
      return cnt;
    } else {
      return npJdbcTemplate.update(sqlPair.getSql(), attributes);
    }
  }

  /**
   * Updates the propertyNames (passed in as args) of the object. Assigns updated by, updated on if
   * these properties exist for the object and the jdbcTemplateMapper is configured for these
   * fields.
   *
   * @param pojo object to be updated
   * @param propertyNames array of property names that should be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object pojo, String... propertyNames) {
    if (pojo == null || ObjectUtils.isEmpty(propertyNames)) {
      throw new IllegalArgumentException("pojo and propertyNames cannot be null");
    }

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    if (!bw.isReadableProperty("id")) {
      throw new IllegalArgumentException(
          "Object " + pojo.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    String tableName = getTableName(pojo.getClass());
    String idColumnName = getTableIdColumnName(tableName);
    List<String> dbColumnNameList = getDbColumnNames(tableName);

    // cachekey ex: className-propertyName1-propertyName2
    String cacheKey = tableName + "-" + String.join("-", propertyNames);
    SqlPair sqlPair = updateSqlCache.get(cacheKey);
    if (sqlPair == null) {
      Set<String> params = new HashSet<>();
      StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
      sqlBuilder.append(fullyQualifiedTableName(tableName));
      sqlBuilder.append(" SET ");

      Set<String> updateColumnNames = new LinkedHashSet<>();
      for (String propertyName : propertyNames) {
        String tableColumnName = getMatchingCaseSensitiveColumnName(dbColumnNameList, propertyName);
        if (tableColumnName == null) {
          throw new RuntimeException(
              "Property "
                  + propertyName
                  + " does not have a coresponding column in the table "
                  + tableName);
        } else {
          updateColumnNames.add(tableColumnName);
        }
      }

      // add updated info to the column list
      String updatedOnColumnName = null;
      if (updatedOnPropertyName != null && bw.isReadableProperty(updatedOnPropertyName)) {
        updatedOnColumnName =
            getMatchingCaseSensitiveColumnName(dbColumnNameList, updatedOnPropertyName);
        if (updatedOnColumnName != null) {
          updateColumnNames.add(updatedOnColumnName);
        }
      }
      String updatedByColumnName = null;
      if (updatedByPropertyName != null
          && recordOperatorResolver != null
          && bw.isReadableProperty(updatedByPropertyName)) {
        updatedByColumnName =
            getMatchingCaseSensitiveColumnName(dbColumnNameList, updatedByPropertyName);
        if (updatedByColumnName != null) {
          updateColumnNames.add(updatedByColumnName);
        }
      }

      String versionColumnName = null;
      if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
        versionColumnName =
            getMatchingCaseSensitiveColumnName(dbColumnNameList, versionPropertyName);
        if (versionColumnName != null) {
          updateColumnNames.add(versionColumnName);
        }
      }

      boolean first = true;
      for (String columnName : updateColumnNames) {
        if (!first) {
          sqlBuilder.append(", ");
        } else {
          first = false;
        }
        sqlBuilder.append(columnName);
        sqlBuilder.append(" = :");

        if (columnName.equals(versionColumnName)) {
          sqlBuilder.append("incrementedVersion");
          params.add("incrementedVersion");
        } else {
          String propertyName = convertSnakeToCamelCase(columnName);
          sqlBuilder.append(propertyName);
          params.add(propertyName);
        }
      }

      // the where clause
      sqlBuilder.append(" WHERE " + idColumnName + " = :id");
      if (versionColumnName != null) {
        sqlBuilder
            .append(" AND ")
            .append(versionColumnName)
            .append(" = :")
            .append(versionPropertyName);
        params.add(versionPropertyName);
      }

      sqlPair = new SqlPair(sqlBuilder.toString(), params);

      updateSqlCache.put(cacheKey, sqlPair);
    }

    LocalDateTime now = LocalDateTime.now();
    if (sqlPair.getParams().contains(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (sqlPair.getParams().contains(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }

    Map<String, Object> attributes = convertObjectToMap(pojo);
    // if object has property version throw OptimisticLockingException
    // update fails. The version gets incremented
    if (sqlPair.getParams().contains(versionPropertyName)) {
      Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + pojo.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", ++versionVal);
        bw.setPropertyValue(
            versionPropertyName, versionVal); // set value incremented value on object
      }

      int cnt = npJdbcTemplate.update(sqlPair.getSql(), attributes);
      if (cnt == 0) {
        throw new OptimisticLockingException(
            "Update failed for "
                + pojo.getClass().getSimpleName()
                + " id:"
                + attributes.get("id")
                + " "
                + versionPropertyName
                + ":"
                + attributes.get("version"));
      }
      return cnt;
    } else {
      return npJdbcTemplate.update(sqlPair.getSql(), attributes);
    }
  }

  /**
   * Physically Deletes the object from the database
   *
   * @param pojo Object to be deleted
   * @return 0 if no records were deleted
   */
  public Integer delete(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    if (!bw.isReadableProperty("id")) {
      throw new IllegalArgumentException(
          "Object " + pojo.getClass().getSimpleName() + " has to have a property named 'id'.");
    }

    String tableName = getTableName(pojo.getClass());
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
    if (!(id instanceof Integer || id instanceof Long)) {
      throw new IllegalArgumentException("id has to be type of Integer or Long");
    }

    String tableName = getTableName(clazz);
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
    List<T> mainObjList = new ArrayList<>();
    if (mainObj != null) {
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
    String tableName = getTableName(relationshipClazz);
    if (isNotEmpty(mainObjList)) {
      List<Number> allColumnIds = new ArrayList<>();
      for (T mainObj : mainObjList) {
        if (mainObj != null) {
          BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
          Number joinPropertyValue = (Number) bw.getPropertyValue(mainObjJoinPropertyName);
          if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
            allColumnIds.add((Number) bw.getPropertyValue(mainObjJoinPropertyName));
          }
        }
      }
      List<U> relatedObjList = new ArrayList<>();
      String idColumnName = getTableIdColumnName(tableName);
      // to avoid query being issued with large number of ids
      // for the 'IN (:columnIds) clause the list is chunked by IN_CLAUSE_CHUNK_SIZE
      // and multiple queries issued if needed.
      Collection<List<Number>> chunkedColumnIds = chunkList(allColumnIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Number> columnIds : chunkedColumnIds) {
        String sql =
            "SELECT * FROM "
                + fullyQualifiedTableName(tableName)
                + " WHERE "
                + idColumnName
                + " IN (:columnIds)";
        MapSqlParameterSource params = new MapSqlParameterSource("columnIds", columnIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(relationshipClazz);
        relatedObjList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      toOneMerge(
          mainObjList, relatedObjList, mainObjRelationshipPropertyName, mainObjJoinPropertyName);
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

    Map<String, List> resultMap = multipleModelMapper(rs, mainObjMapper, relatedObjMapper);
    List<T> mainObjList = resultMap.get(mainObjMapper.getSqlColumnPrefix());
    List<U> relatedObjList = resultMap.get(relatedObjMapper.getSqlColumnPrefix());

    toOneMerge(
        mainObjList, relatedObjList, mainObjRelationshipPropertyName, mainObjJoinPropertyName);
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

    if (isNotEmpty(mainObjList) && isNotEmpty(relatedObjList)) {
      // Map key: related object id , value: the related object
      Map<Number, U> idToObjectMap =
          relatedObjList
              .stream()
              .filter(e -> e != null)
              .collect(Collectors.toMap(e -> (Number) getSimpleProperty(e, "id"), obj -> obj));

      for (T mainObj : mainObjList) {
        if (mainObj != null) {
          Number joinPropertyValue = (Number) getSimpleProperty(mainObj, mainObjJoinPropertyName);
          if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
            bw.setPropertyValue(
                mainObjRelationshipPropertyName, idToObjectMap.get(joinPropertyValue));
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
    List<T> mainObjList = new ArrayList<>();
    mainObjList.add(mainObj);
    toManyForList(
        mainObjList,
        manySideClazz,
        mainObjCollectionPropertyName,
        manySideJoinPropertyName,
        manySideOrderByClause);
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
    String tableName = getTableName(manySideClazz);

    if (isNotEmpty(mainObjList)) {
      Set<Number> allIds = new LinkedHashSet<>();
      for (T mainObj : mainObjList) {
        if (mainObj != null) {
          BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
          Number idVal = (Number) bw.getPropertyValue("id");
          if (idVal != null && idVal.longValue() > 0) {
            allIds.add((idVal));
          } else {
            throw new RuntimeException("id property in mainObjList cannot be null");
          }
        }
      }
      List<Number> uniqueIds = new ArrayList<>(allIds);
      String joinColumnName = getJoinColumnName(tableName, manySideJoinPropertyName);
      List<U> manySideList = new ArrayList<>();
      // to avoid query being issued with large number of
      // records for the 'IN (:columnIds) clause the list is chunked by IN_CLAUSE_CHUNK_SIZE
      // and multiple queries issued
      Collection<List<Number>> chunkedColumnIds = chunkList(uniqueIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Number> columnIds : chunkedColumnIds) {
        String sql =
            "SELECT * FROM "
                + fullyQualifiedTableName(tableName)
                + " WHERE "
                + joinColumnName
                + " IN (:columnIds)";
        if (isNotEmpty(manySideOrderByClause)) {
          sql += " " + manySideOrderByClause;
        }
        MapSqlParameterSource params = new MapSqlParameterSource("columnIds", columnIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(manySideClazz);
        manySideList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      toManyMerge(
          mainObjList, manySideList, mainObjCollectionPropertyName, manySideJoinPropertyName);
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

    try {
      if (isNotEmpty(manySideList)) {
        Map<Number, List<U>> mapColumnIdToManySide =
            manySideList
                .stream()
                .filter(e -> e != null)
                .collect(
                    Collectors.groupingBy(
                        e -> (Number) getSimpleProperty(e, manySideJoinPropertyName)));

        // assign the manyside list to the mainobj
        for (T mainObj : mainObjList) {
          if (mainObj != null) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
            bw.setConversionService(defaultConversionService);
            Number idValue = (Number) bw.getPropertyValue("id");
            List<U> relatedList = mapColumnIdToManySide.get(idValue);
            bw.setPropertyValue(mainObjCollectionPropertyName, relatedList);
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
    try {
      Map<String, List> resultMap = new HashMap();
      Map<String, List> tempMap = new HashMap<>();
      for (SelectMapper selectMapper : selectMappers) {
        tempMap.put(selectMapper.getSqlColumnPrefix(), new ArrayList());
      }
      List<String> resultSetColumnNames = getResultSetColumnNames(rs);
      while (rs.next()) {
        for (SelectMapper selectMapper : selectMappers) {
          Number id = (Number) rs.getObject(selectMapper.getSqlColumnPrefix() + "id");
          if (id != null && id.longValue() > 0) {
            Object obj =
                newInstance(
                    selectMapper.getClazz(),
                    rs,
                    selectMapper.getSqlColumnPrefix(),
                    resultSetColumnNames);
            tempMap.get(selectMapper.getSqlColumnPrefix()).add(obj);
          }
        }
      }

      // each list should only have elements unique by 'id'
      for (String key : tempMap.keySet()) {
        List<Object> list = tempMap.get(key);
        resultMap.put(key, uniqueByIdList(list));
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
    String str = selectColsCache.get(tableName + "-" + tableAlias);
    if (str == null) {
      List<String> dbColumnNames = getDbColumnNames(tableName);
      if (isEmpty(dbColumnNames)) {
        // try with uppercase table name
        dbColumnNames = getDbColumnNames(tableName.toUpperCase());
        if (isEmpty(dbColumnNames)) {
          throw new RuntimeException("No table named " + tableName);
        }
      }
      StringBuilder sb = new StringBuilder(" ");
      for (String colName : dbColumnNames) {
        sb.append(tableAlias)
            .append(".")
            .append(colName)
            .append(" ")
            .append(tableAlias)
            .append("_")
            .append(colName)
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

  /**
   * Builds sql update statement with named parameters for the object.
   *
   * @param pojo the object that needs to be update.
   * @return The sql update string
   */
  private SqlPair buildUpdateSql(Object pojo) {
    String tableName = getTableName(pojo.getClass());
    String idColumnName = getTableIdColumnName(tableName);

    Set<String> params = new HashSet<>();
    // database columns for the tables
    List<String> dbColumnNameList = getDbColumnNames(tableName);

    // ignore these attributes when generating the sql 'SET' command
    List<String> ignoreAttrs = new ArrayList<>();
    ignoreAttrs.add("id");
    if (createdByPropertyName != null) {
      ignoreAttrs.add(createdByPropertyName);
    }
    if (createdOnPropertyName != null) {
      ignoreAttrs.add(createdOnPropertyName);
    }

    List<String> updateColumnNameList = new ArrayList<>();
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    PropertyDescriptor[] propertyDescriptors = bw.getPropertyDescriptors();
    for (PropertyDescriptor pd : propertyDescriptors) {
      // skips non db columns and ignore fields like 'id' etc for SET
      if (!ignoreAttrs.contains(pd.getName())) {
        String tableColumnName = getMatchingCaseSensitiveColumnName(dbColumnNameList, pd.getName());
        if (tableColumnName != null) {
          updateColumnNameList.add(tableColumnName);
        }
      }
    }

    StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
    sqlBuilder.append(fullyQualifiedTableName(tableName));
    // build set clause
    sqlBuilder.append(" SET ");
    boolean first = true;
    String versionColumnName = null;
    if (versionPropertyName != null) {
      versionColumnName = getMatchingCaseSensitiveColumnName(dbColumnNameList, versionPropertyName);
    }
    for (String columnName : updateColumnNameList) {
      if (!first) {
        sqlBuilder.append(", ");
      } else {
        first = false;
      }
      sqlBuilder.append(columnName);
      sqlBuilder.append(" = :");

      if (columnName.equals(versionColumnName)) {
        sqlBuilder.append("incrementedVersion");
        params.add("incrementedVersion");
      } else {
        String propertyName = convertSnakeToCamelCase(columnName);
        sqlBuilder.append(propertyName);
        params.add(propertyName);
      }
    }
    // build where clause
    sqlBuilder.append(" WHERE " + idColumnName + " = :id");
    if (versionColumnName != null) {
      sqlBuilder
          .append(" AND ")
          .append(versionColumnName)
          .append(" = :")
          .append(versionPropertyName);
      params.add(versionPropertyName);
    }

    String updateSql = sqlBuilder.toString();
    SqlPair sqlPair = new SqlPair(updateSql, params);
    updateSqlCache.put(tableName, sqlPair);

    return sqlPair;
  }

  /**
   * Used by mappers to instantiate object from the result set
   *
   * @param clazz Class of object to be instantiated
   * @param rs Sql result set
   * @param prefix The sql alias in the query
   * @param resultSetColumnNames the column names in the sql statement.
   * @return Object of type T populated from the data in the result set
   */
  private <T> T newInstance(
      Class<T> clazz, ResultSet rs, String prefix, List<String> resultSetColumnNames) {
    try {
      Object obj = clazz.newInstance();
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
      // need below for java.sql.Timestamp to java.time.LocalDateTime conversion etc
      bw.setConversionService(defaultConversionService);
      List<String> propertyNames = getPropertyNames(obj);
      for (String propName : propertyNames) {
        String columnName = convertCamelToSnakeCase(propName);
        if (isNotEmpty(prefix)) {
          columnName = prefix + columnName;
        }
        int index = resultSetColumnNames.indexOf(columnName.toLowerCase());
        Object columnVal = null;
        if (index != -1) {
          // using Springs JdbcUtils to handle oracle.sql.Timestamp ...
          columnVal = JdbcUtils.getResultSetValue(rs, index + 1);
        }
        bw.setPropertyValue(propName, columnVal);
      }
      return clazz.cast(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts an object to a map with key as database column names and assigned corresponding object
   * value. Camel case property names are converted to snake case. For example property name
   * 'userLastName' will get converted to map key 'user_last_name' and assigned the corresponding
   * object value.
   *
   * @param pojo The object to convert
   * @return A map with keys that are in snake case to match database column names and values
   *     corresponding to the object property
   */
  private Map<String, Object> convertToSnakeCaseAttributes(Object pojo) {
    Map<String, Object> camelCaseAttrs = convertObjectToMap(pojo);
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
   * @param pojo The object to be converted.
   * @return Map with key: property name, value: object value
   */
  private Map<String, Object> convertObjectToMap(Object pojo) {
    Map<String, Object> camelCaseAttrs = new HashMap<>();
    List<String> propertyNames = getPropertyNames(pojo);
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    for (String propName : propertyNames) {
      Object propValue = bw.getPropertyValue(propName);
      camelCaseAttrs.put(propName, propValue);
    }
    return camelCaseAttrs;
  }

  /**
   * Gets the table column names from Databases MetaData. The column names are cached
   *
   * @param tableName table name
   * @return the list of columns of the table
   */
  private List<String> getDbColumnNames(String tableName) {
    List<String> columns = tableColumnNamesCache.get(tableName);
    if (isEmpty(columns)) {
      columns = new ArrayList<>();
      try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet resultSet = metadata.getColumns(null, schemaName, tableName, null);
        while (resultSet.next()) {
          columns.add(resultSet.getString("COLUMN_NAME"));
        }
        resultSet.close();
        if (isNotEmpty(columns)) {
          tableColumnNamesCache.put(tableName, columns);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return columns;
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
   * @param pojo the java object
   * @return List of property names.
   */
  private List<String> getPropertyNames(Object pojo) {
    List<String> list = objectPropertyNamesCache.get(pojo.getClass().getSimpleName());
    if (list == null) {
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
      list = new ArrayList<>();
      PropertyDescriptor[] propertyDescriptors = bw.getPropertyDescriptors();
      for (PropertyDescriptor pd : propertyDescriptors) {
        String propName = pd.getName();
        // log.debug("Property name:{}" + propName);
        if ("class".equals(propName)) {
          continue;
        } else {
          list.add(propName);
        }
      }
      objectPropertyNamesCache.put(pojo.getClass().getSimpleName(), list);
    }
    return list;
  }

  /**
   * Gets the property value of an object using Springs BeanWrapper
   *
   * @param obj the java object
   * @param propertyName the property name
   * @return The property value
   */
  private Object getSimpleProperty(Object obj, String propertyName) {
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    return bw.getPropertyValue(propertyName);
  }

  /**
   * Gets the corresponding table name for the class. If @Table annotation is used for the class use
   * its 'name' attribute otherwise returns a snake case name.
   *
   * @param clazz The class
   * @return The table name
   */
  private String getTableName(Class<?> clazz) {
    String tableName = objectToTableCache.get(clazz.getName());
    if (tableName == null) {
      if (clazz.isAnnotationPresent(Table.class)) {
        // @Table annotation is present. Get the table name
        Table table = clazz.getAnnotation(Table.class);
        tableName = table.name();
      } else {
        tableName = convertCamelToSnakeCase(clazz.getSimpleName());
        List<String> columnNames = getDbColumnNames(tableName);
        if (ObjectUtils.isEmpty(columnNames)) {
          // try with uppercase
          tableName = tableName.toUpperCase();
          columnNames = getDbColumnNames(tableName);
          if (ObjectUtils.isEmpty(columnNames)) {
            throw new RuntimeException(
                "Could not find corresponding table for class " + clazz.getName());
          }
        }
        // while we are at it, we get the case sensitive id name
        if (!ObjectUtils.isEmpty(columnNames)) {
          for (String columnName : columnNames) {
            if (columnName.equals("id") || columnName.equals("ID")) {
              tableToCaseSensitiveIdName.put(tableName, columnName);
              break;
            }
          }
        }
      }
    }
    objectToTableCache.put(clazz.getName(), tableName);
    return tableName;
  }

  private String getTableIdColumnName(String tableName) {
    String idName = tableToCaseSensitiveIdName.get(tableName);
    if (StringUtils.isEmpty(idName)) {
      List<String> columnNames = getDbColumnNames(tableName);
      if (ObjectUtils.isEmpty(columnNames)) {
        // try again with uppercase table name.
        columnNames = getDbColumnNames(tableName.toUpperCase());
        if (ObjectUtils.isEmpty(columnNames)) {
          throw new RuntimeException("Could not find table " + tableName);
        }
      }
      if (!ObjectUtils.isEmpty(columnNames)) {
        for (String columnName : columnNames) {
          if (columnName.equals("id") || columnName.equals("ID")) {
            idName = columnName;
            tableToCaseSensitiveIdName.put(tableName, idName);
            break;
          }
        }
      }
    }
    if (StringUtils.isEmpty(idName)) {
      throw new RuntimeException("Could not find id column for table " + tableName);
    }
    return idName;
  }

  private String getJoinColumnName(String tableName, String joinPropertyName) {
    if (tableName == null || joinPropertyName == null) {
      throw new IllegalArgumentException("tableName and joinPropertyName cannot be null");
    }
    List<String> columnNames = getDbColumnNames(tableName);

    String joinColumnName = convertCamelToSnakeCase(joinPropertyName);
    String ucJoinColumnName = joinColumnName.toUpperCase();
    for (String columnName : columnNames) {
      if (columnName.equals(joinColumnName) || columnName.equals(ucJoinColumnName)) {
        return columnName;
      }
    }
    // if code reached here throw exception
    throw new RuntimeException(
        "Could not find corresponding column in table "
            + tableName
            + " for joinPropertyName "
            + joinPropertyName);
  }

  private String getMatchingCaseSensitiveColumnName(
      List<String> dbColumnNameList, String propertyName) {
    String val = null;
    if (isNotEmpty(dbColumnNameList) && isNotEmpty(propertyName)) {
      String columnName = convertCamelToSnakeCase(propertyName);
      if (dbColumnNameList.contains(columnName)) {
        val = columnName;
      } else {
        // try with uppercase
        String ucName = columnName.toUpperCase();
        if (dbColumnNameList.contains(ucName)) {
          val = ucName;
        }
      }
    }
    return val;
  }

  /**
   * Converts camel case to snake case. Ex: userLastName gets converted to user_last_name. The
   * conversion info is cached.
   *
   * @param str camel case String
   * @return the snake case string
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
    String camelCase = snakeToCamelCache.get(str);
    if (camelCase == null) {
      if (str != null) {
        camelCase = JdbcUtils.convertUnderscoreNameToPropertyName(str);
        snakeToCamelCache.put(str, camelCase);
      }
    }
    return camelCase;
  }

  /**
   * Splits the list into multiple lists of chunk size. Used to split the sql IN clauses since some
   * databases have a limitation of 1024. We set the chuck size to IN_CLAUSE_CHUNK_SIZE
   *
   * @param list
   * @param chunkSize
   * @return Collection of lists broken down by chunkSize
   */
  private Collection<List<Number>> chunkList(List<Number> list, Integer chunkSize) {
    AtomicInteger counter = new AtomicInteger();
    Collection<List<Number>> result =
        list.stream()
            .filter(e -> e != null)
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values();
    return result;
  }

  private String fullyQualifiedTableName(String tableName) {
    if (isNotEmpty(schemaName)) {
      return schemaName + "." + tableName;
    }
    return tableName;
  }

  /**
   * Get list with unique objects by object.id
   *
   * @param list
   * @return List of unique objects by id
   */
  private List<Object> uniqueByIdList(List<Object> list) {
    if (isNotEmpty(list)) {
      Map<Number, Object> idToObjectMap = new LinkedHashMap<>();
      for (Object obj : list) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        Number id = (Number) bw.getPropertyValue("id");
        if (!idToObjectMap.containsKey(id)) {
          idToObjectMap.put(id, obj);
        }
      }
      return new ArrayList<Object>(idToObjectMap.values());
    } else {
      return list;
    }
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
