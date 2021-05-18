package org.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
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

import javax.sql.DataSource;

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

/**
 * An utility class which uses Spring's JdbcTemplate for CRUD and relationship mappings.
 * Requires Java 8 and above.
 * Code has no external dependencies other than spring framework
 *
 * <pre>
 * 1) Simple CRUD one liners
 * 2) Methods to map relationships (toOne, toMany etc)
 * 3) Can auto populate fields like created on, updated on if configured.
 * 4) Can auto populate created by, updated by fields if configured using an 
 *    implementation of IRecordOperatorResolver.
 * 5) Can be configured to provide optimistic locking functionality for updates using a version property.
 *
 * JdbcTemplateMapper is  opinionated. It does not use any custom annotations.
 * 1) A model is mapped to its corresponding  table using a naming convention where the 
 *    camel case model name is matched to a snake case table name.
 *    For model named 'Order' the corresponding database table name will be 'order'.
 *    For model named 'OrderLine' the corresponding database table name will be 'order_line'.
 * 2) Properties of a model like firstName, lastName will be matched to corresponding database columns 
 *    first_name and last_name in the table. Uses camel case to snake case mapping.
 * 3) All the models should have a property named 'id' which has to be of type Integer or Long.
 * 4) For data/datetime fields the models should use java 8+ classes LocalDate, LocalDateTime
 * 4) Models can have properties which do not have corresponding database columns or vice versa.
 *    The framework ignores them during inserts/updates/find.. 
 * 
 * Examples of simple CRUD:
 * public class Product{
 *    private Integer id;
 *    private String name;
 *    private Double price;
 *    private String someNonDatabaseProperty; // will not be part of insert/update/find
 *    // getters and setters ...
 * }
 * 
 * Product product = new Product();
 * product.setName("someName");
 * product.setPrice(10.25);
 * jdbcTemplateMapper.insert(product);
 * 
 * product = jdbcTemplateMapper.findById(1, Product.class);
 * product.setPrice(11.50);
 * jdbcTemplateMapper.update(product);
 * 
 * List<Product> products = jdbcTemplateMapper.findAll(Product.class);
 * 
 * See the toOne..() and  toMany..() classes to retrieve classes and their relationships.
 * 
 * Installation:
 * Dependencies in pom.xml
 * If a spring boot application:
 * {@code
 *  <dependency>
 *    <groupId>org.springframework.boot</groupId>
 *    <artifactId>spring-boot-starter-jdbc</artifactId>
 * </dependency>
 * }
 * 
 * If a regular spring application: 
 * {@code
 *  <dependency> 
 *   <groupId>org.springframework</groupId>
 *   <artifactId>spring-jdbc</artifactId> 
 *   <version>YourVersionOfSpringJdbc</version> 
 *  </dependency>
 * }
 * 
 * Spring bean configuration will look something like below (Assuming that the DataSource is already configured):
 *
 * {@literal @}Bean
 * public JdbcTemplateMapper jdbcTemplateMapper(DataSource dataSource) {
 *   return new JdbcTemplateMapper(dataSource, "yourSchemaName");
 * }
 *
 * A Base DAO will look something like below:
 *
 * public class BaseDao {
 *   {@literal @}Autowired protected JdbcTemplateMapper jdbcTemplateMapper;
 *   protected JdbcTemplate jdbcTemplate;
 *   protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;
 *
 *   {@literal @}PostConstruct
 *   public void init() {
 *     this.jdbcTemplate = jdbcTemplateMapper.getJdbcTemplate();
 *     this.namedParameterJdbcTemplate = jdbcTemplateMapper.getNamedParameterJdbcTemplate();
 *   }
 * }
 * </pre>
 *
 * @author ajoseph
 */
public class JdbcTemplateMapper {
  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate npJdbcTemplate;
  private IRecordOperatorResolver recordOperatorResolver;

  private String createdByPropertyName;
  private String createdOnPropertyName;
  private String updatedByPropertyName;
  private String updatedOnPropertyName;
  private String schemaName;
  private String versionPropertyName;

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
  private Map<String, String> updateSqlCache = new ConcurrentHashMap<>();

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

  /**
   * The constructor.
   *
   * @param dataSource - The dataSource for the mapper
   * @param schemaName - schema name to be used by mapper
   */
  public JdbcTemplateMapper(DataSource dataSource, String schemaName) {
    if (dataSource == null) {
      throw new IllegalArgumentException("dataSource cannot be null");
    }
    if (isEmpty(schemaName)) {
      throw new IllegalArgumentException("schemaName cannot be null");
    }

    this.schemaName = schemaName;
    this.npJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    this.jdbcTemplate = npJdbcTemplate.getJdbcTemplate();
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
   * The implementation of IRecordOperatorResolver is used to populate the created by and updated by
   * fields. Assign this while initializing the jdbcTemplateMapper
   *
   * @param recordOperatorResolver - The implement for interface IRecordOperatorResolver
   * @return The jdbcTemplateMapper - The jdbcTemplateMapper
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
   * The property used for optimistic locking. The property has to be of type Integer. If the object
   * has the version property name, on inserts it will be set to 1 and on updates it will
   * incremented by 1. Assign this while initializing jdbcTemplateMapper.
   *
   * @param propName - The version propertyName
   * @return The jdbcTemplateMapper
   */
  public JdbcTemplateMapper withVersionPropertyName(String propName) {
    this.versionPropertyName = propName;
    return this;
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
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + schemaName + "." + tableName + " where id = ?";
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
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + schemaName + "." + tableName;
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
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + schemaName + "." + tableName + " " + orderByClause;
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
   * @param pojo - The object to be saved
   */
  public void insert(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    Object idValue = bw.getPropertyValue("id");
    if (idValue != null) {
      throw new RuntimeException(
          "For method insert() the objects 'id' property has to be null since this insert is for an object whose id is autoincrement in database.");
    }

    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
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
   * @param pojo - The object to be saved
   */
  public void insertWithId(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    Object idValue = bw.getPropertyValue("id");
    if (idValue == null) {
      throw new RuntimeException(
          "For method insertById() the objects 'id' property cannot be null.");
    }

    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
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
   * @param pojo - object to be updated
   * @return number of records updated
   */
  public Integer update(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    String updateSql = updateSqlCache.get(tableName);
    if (updateSql == null) {
      updateSql = buildUpdateSql(pojo);
    }

    List<String> dbColumnNameList = getDbColumnNames(tableName);

    LocalDateTime now = LocalDateTime.now();
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);

    if (updatedOnPropertyName != null
        && bw.isReadableProperty(updatedOnPropertyName)
        && dbColumnNameList.contains(convertCamelToSnakeCase(updatedOnPropertyName))) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(updatedByPropertyName)
        && dbColumnNameList.contains(convertCamelToSnakeCase(updatedByPropertyName))) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }

    Map<String, Object> attributes = convertObjectToMap(pojo);
    // if object has property version throw OptimisticLockingException
    // when update fails. The version gets incremented on update
    if (versionPropertyName != null
        && bw.isReadableProperty(versionPropertyName)
        && dbColumnNameList.contains(convertCamelToSnakeCase(versionPropertyName))) {
      Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + pojo.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", ++versionVal);
        bw.setPropertyValue(versionPropertyName, versionVal);
      }
      int cnt = npJdbcTemplate.update(updateSql, attributes);
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
      return npJdbcTemplate.update(updateSql, attributes);
    }
  }

  /**
   * Updates the propertyNames (passed in as args) of the object. Assigns updated by, updated on if
   * these properties exist for the object and the jdbcTemplateMapper is configured for these
   * fields.
   *
   * @param pojo - object to be updated
   * @param propertyNames - array of property names that should be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object pojo, String... propertyNames) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    List<String> dbColumnNameList = getDbColumnNames(tableName);

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);

    // cachekey ex: className-propertyName1-propertyName2
    String cacheKey = tableName + "-" + String.join("-", propertyNames);
    String updateSql = updateSqlCache.get(cacheKey);
    if (updateSql == null) {
      StringBuilder sqlBuilder = new StringBuilder("update ");
      sqlBuilder.append(schemaName);
      sqlBuilder.append(".");
      sqlBuilder.append(tableName);
      sqlBuilder.append(" set ");

      List<String> updateColumnNameList = new ArrayList<>();
      for (String propertyName : propertyNames) {
        updateColumnNameList.add(convertCamelToSnakeCase(propertyName));
      }

      // add updated info to the column list
      if (updatedOnPropertyName != null
          && bw.isReadableProperty(updatedOnPropertyName)
          && dbColumnNameList.contains(convertCamelToSnakeCase(updatedOnPropertyName))) {
        updateColumnNameList.add(convertCamelToSnakeCase(updatedOnPropertyName));
      }
      if (updatedByPropertyName != null
          && recordOperatorResolver != null
          && bw.isReadableProperty(updatedByPropertyName)
          && dbColumnNameList.contains(convertCamelToSnakeCase(updatedByPropertyName))) {
        updateColumnNameList.add(convertCamelToSnakeCase(updatedByPropertyName));
      }

      boolean first = true;
      for (String columnName : updateColumnNameList) {
        if (!first) {
          sqlBuilder.append(", ");
        } else {
          first = false;
        }
        sqlBuilder.append(columnName);
        sqlBuilder.append(" = :");

        sqlBuilder.append(convertSnakeToCamelCase(columnName));
      }
      // the set assignment for the incremented version
      if (versionPropertyName != null
          && bw.isReadableProperty(versionPropertyName)
          && dbColumnNameList.contains(convertCamelToSnakeCase(versionPropertyName))) {
        sqlBuilder.append(", ").append(versionPropertyName).append(" = :incrementedVersion");
      }
      // the where clause
      sqlBuilder.append(" where id = :id");
      if (versionPropertyName != null
          && bw.isReadableProperty(versionPropertyName)
          && dbColumnNameList.contains(convertCamelToSnakeCase(versionPropertyName))) {
        sqlBuilder
            .append(" and ")
            .append(versionPropertyName)
            .append(" = :")
            .append(versionPropertyName);
      }

      updateSql = sqlBuilder.toString();
      updateSqlCache.put(cacheKey, updateSql);
    }

    LocalDateTime now = LocalDateTime.now();
    if (updatedOnPropertyName != null
        && bw.isReadableProperty(updatedOnPropertyName)
        && dbColumnNameList.contains(convertCamelToSnakeCase(updatedOnPropertyName))) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(updatedByPropertyName)
        && dbColumnNameList.contains(convertCamelToSnakeCase(updatedByPropertyName))) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }

    Map<String, Object> attributes = convertObjectToMap(pojo);
    // if object has property version throw OptimisticLockingException
    // update fails. The version gets incremented
    if (versionPropertyName != null
        && bw.isReadableProperty(versionPropertyName)
        && dbColumnNameList.contains(convertCamelToSnakeCase(versionPropertyName))) {
      Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
      if (versionVal == null) {
        throw new RuntimeException(
            versionPropertyName
                + " cannot be null when updating "
                + pojo.getClass().getSimpleName());
      } else {
        attributes.put("incrementedVersion", ++versionVal);
        bw.setPropertyValue(versionPropertyName, versionVal);
      }
      int cnt = npJdbcTemplate.update(updateSql, attributes);
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
      return npJdbcTemplate.update(updateSql, attributes);
    }
  }

  /**
   * Physically Deletes the object from the database
   *
   * @param pojo - Object to be deleted
   * @return 0 if no records were deleted
   */
  public Integer delete(Object pojo) {
    if (pojo == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    String sql = "delete from " + schemaName + "." + tableName + " where id = ?";
    Object id = bw.getPropertyValue("id");
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Physically Deletes the object from the database by id
   *
   * @param id - Id of object to be deleted
   * @param clazz - Type of object to be deleted.
   * @return 0 if no records were deleted
   */
  public <T> Integer deleteById(Object id, Class<T> clazz) {
    if (!(id instanceof Integer || id instanceof Long)) {
      throw new IllegalArgumentException("id has to be type of Integer or Long");
    }
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "delete from " + schemaName + "." + tableName + " where id = ?";
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   * toOne related Object:
   * Class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * 1) Get an Order Object using a query for example:
   * Order order = jdbcTemplateMapper.findById(orderId)
   *
   * 2) Populate the Order's toOne customer property. This will issue an sql and populate order.customer
   * jdbcTemplateMapper.toOneForObject(order, Customer.class, "customer", "customerId");
   *
   * </pre>
   *
   * @param mainObj - the main object
   * @param relationShipClazz - The relationship class
   * @param mainObjRelationshipPropertyName - The propertyName of the toOne relationship (on
   *     mainOjb) that needs to be populated.
   * @param mainObjJoinPropertyName - the join property on main object.
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   * toOne related Object:
   * Class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * 1) Get list of orders using a query for example:
   * List<Order> orders = jdbcTemplateMapper.findAll(Order.class)
   *
   * 2) Populate each Order's toOne customer property. This will issue sql to get the corresponding
   *    customers using an IN clause:
   * jdbcTemplateMapper.toOneForList(orders, Customer.class, "customer", "customerId");
   *
   * </pre>
   *
   * @param mainObjList - list of main objects
   * @param relationShipClazz - The relationship class
   * @param mainObjRelationshipPropertyName - The toOne relationship property name on main object
   * @param mainObjJoinPropertyName - the join property name on the main object.
   */
  public <T, U> void toOneForList(
      List<T> mainObjList,
      Class<U> relationshipClazz,
      String mainObjRelationshipPropertyName,
      String mainObjJoinPropertyName) {
    String tableName = convertCamelToSnakeCase(relationshipClazz.getSimpleName());
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
      // to avoid query being issued with large number of ids
      // for the 'IN (:columnIds) clause the list is chunked by IN_CLAUSE_CHUNK_SIZE
      // and multiple queries issued if needed.
      Collection<List<Number>> chunkedColumnIds = chunkList(allColumnIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Number> columnIds : chunkedColumnIds) {
        String sql = "select * from " + schemaName + "." + tableName + " where id in (:columnIds)";
        MapSqlParameterSource params = new MapSqlParameterSource("columnIds", columnIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(relationshipClazz);
        relatedObjList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      toOneMerge(
          mainObjList, relatedObjList, mainObjRelationshipPropertyName, mainObjJoinPropertyName);
    }
  }

  /**
   * Populates a single main object and its toOne related object with the data from the ResultSet
   * using their respective SqlMappers. The sql for the Resultset object should have the join
   * properties in select statement so that the main object and related object can be tied together
   * (mainObj.mainObjJoinPropertyName = relatedObj.id)
   *
   * <pre>
   * Main Object:
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   *
   * toOne related Object:
   * Class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * The sql below. Uses a column alias naming convention so that the SelectMapper() can use the prefix
   * to populate the appropriated Objects from the selected columns.
   *
   * Note that the join properties  o.customer_id and the c.id have to be in select clause.
   * </pre>
   *
   * See {@link #selectCols()} to make the select clause less verbose.
   *
   * <pre>
   * select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id
   *        c.id c_id, c.first_name c_first_name, c.last_name c_last_name, c.address c_address
   * from order o
   * left join customer c on o.customer_id = c.id
   * where o.id = ?
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
   *               new SelectMapper<Order>(Order.class, "o_"),
   *               new SelectMapper<Customer>(Customer.class, "c_"),
   *               "customer",
   *               "customerId");
   *         });
   * </pre>
   *
   * @param rs - The jdbc ResultSet
   * @param mainObjMapper - The main object mapper.
   * @param relatedObjMapper - The related object mapper
   * @param mainObjRelationshipPropertyName - The toOne relationship property name on main object
   * @param mainObjJoinPropertyName - The join property name on the main object
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   Customer customer; // the toOne relationship
   * }
   *
   * toOne related Object:
   * Class Customer{
   *   Integer id;
   *   String firstName;
   *   String lastName;
   *   String address;
   * }
   *
   * The sql below. Uses a column alias naming convention so that the SelectMapper() can use the prefix
   * to populate the appropriated Objects from the selected columns.
   *
   * Note that the join properties  o.customer_id and the c.id have to be in select clause.
   * See {@link #selectCols()} to make the select clause less verbose.
   *
   * select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id
   *        c.id c_id, c.first_name c_first_name, c.last_name c_last_name, c.address c_address
   * from order o
   * left join customer c on o.customer_id = c.id
   *
   * Example call to get Orders and its Customer (toOne relationship) populated from the sql above:
   * List<Order> orders =
   *     jdbcTemplate.query(
   *         sql,
   *         rs -> {
   *           return jdbcTemplateMapper.toOneMapperForList(
   *               rs,
   *               new SelectMapper<Order>(Order.class, "o_"),
   *               new SelectMapper<Customer>(Customer.class, "c_"),
   *               "customer",
   *               "customerId");
   *         });
   * </pre>
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper - The main object mapper.
   * @param relatedObjMapper - The related object mapper
   * @param mainObjRelationshipPropertyName - The toOne relationship property name on main object
   * @param mainObjJoinPropertyName - The join property name on the main object
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
   * @param mainObjList - list of main objects
   * @param relatedObjList - list of related objects
   * @param mainObjRelationshipPropertyName - The toOne relationship property name on main object
   * @param mainObjJoinPropertyName - The join property name on the main object
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * Class OrderLine{
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
   * @param mainObjList - the main object list
   * @param manySideClass - The many side class
   * @param mainObjCollectionPropertyName - The collection property name on main object
   * @param manySideJoinPropertyName - The join property name on the many side object
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * Class OrderLine{
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
   * @param mainObjList - the main object list
   * @param manySideClass - The many side class
   * @param mainObjCollectionPropertyName - The collection property name on main object
   * @param manySideJoinPropertyName - The join property name on the many side object
   * @param manySideOrderByClause - The order by clause for the many side query
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * Class OrderLine{
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
   * @param mainObjList - the main object list
   * @param manySideClass - The many side class
   * @param mainObjCollectionPropertyName - The collection property name on mainObj
   * @param manySideJoinPropertyName - the join property name on the many side object
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
   * Class Order{
   *   Integer id;
   *   LocalDateTime orderDate;
   *   Integer customerId;
   *   List<OrderLine> orderLines; // toMany relationship
   * }
   * toMany related Object:
   * Class OrderLine{
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
   * @param mainObjList - the main object list
   * @param manySideClass - The many side class
   * @param mainObjCollectionPropertyName - The collection property name on mainObj
   * @param manySideJoinPropertyName - the join property name on the many side object
   * @param manySideOrderByClause - The order by clause for the many side query
   */
  public <T, U> void toManyForList(
      List<T> mainObjList,
      Class<U> manySideClazz,
      String mainObjCollectionPropertyName,
      String manySideJoinPropertyName,
      String manySideOrderByClause) {
    String tableName = convertCamelToSnakeCase(manySideClazz.getSimpleName());
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
      String joinColumnName = convertCamelToSnakeCase(manySideJoinPropertyName);
      List<U> manySideList = new ArrayList<>();
      // to avoid query being issued with large number of
      // records for the 'IN (:columnIds) clause the list is chunked by IN_CLAUSE_CHUNK_SIZE
      // and multiple queries issued
      Collection<List<Number>> chunkedColumnIds = chunkList(uniqueIds, IN_CLAUSE_CHUNK_SIZE);
      for (List<Number> columnIds : chunkedColumnIds) {
        String sql =
            "select * from "
                + schemaName
                + "."
                + tableName
                + " where "
                + joinColumnName
                + " in (:columnIds)";
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
   * @param rs - The jdbc ResultSet
   * @param mainObjMapper - The main object mapper.
   * @param manySideObjMapper - The many side object mapper
   * @param mainObjCollectionPropertyName - the collectionPropertyName on the mainObj that needs to
   *     be populated
   * @param manySideJoinPropertyName - the join property name on the manySide
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
   * @param mainObjMapper - The main object mapper
   * @param manySideObjMapper - The many side object mapper
   * @param mainObjCollectionPropertyName - the collectionPropertyName on the mainObj that needs to
   *     be populated
   * @param manySideJoinPropertyName - the join property name on the manySide
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
   * @param mainObjList - the main object list
   * @param manySideList - the many side object list
   * @param mainObjCollectionPropertyName - the collection property name of the main object that
   *     needs to be populated.
   * @param manySideJoinPropertyName - the join property name on the many side
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
   * @param rs - The result set
   * @param selectMappers - array of sql mappers.
   * @return Map - key: 'sqlColumnPrefix' of each sqlMapper, value - unique list for each sqlMapper
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
   * @param tableName - the Table name
   * @param tableAlias - the alias being used in the sql statement for the table.
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
   * @param tableName - the Table name
   * @param tableAlias - the alias being used in the sql statement for the table.
   * @param includeLastComma - if true last character will be comma; if false the string will have
   *     NO comma at end
   * @return comma separated select column string
   */
  public String selectCols(String tableName, String tableAlias, boolean includeLastComma) {
    List<String> dbColumnNames = getDbColumnNames(tableName);
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
    String str = sb.toString();
    if (!includeLastComma) {
      // remove the last comma.
      str = str.substring(0, str.length() - 1) + " ";
    }
    return str;
  }

  /**
   * Builds sql update statement with named parameters for the object.
   *
   * @param pojo - the object that needs to be update.
   * @return The sql update string
   */
  private String buildUpdateSql(Object pojo) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());

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
      String columnName = convertCamelToSnakeCase(pd.getName());
      // skips non db columns and ignore fields like 'id' etc for SET
      if (!ignoreAttrs.contains(pd.getName()) && dbColumnNameList.contains(columnName)) {
        updateColumnNameList.add(columnName);
      }
    }

    StringBuilder sqlBuilder = new StringBuilder("update ");
    sqlBuilder.append(schemaName);
    sqlBuilder.append(".");
    sqlBuilder.append(tableName);
    sqlBuilder.append(" set ");
    boolean first = true;
    // the dbColumnNameList is the driver because we want the update statement column order to
    // reflect the table column order in database.
    for (String columnName : dbColumnNameList) {
      if (updateColumnNameList.contains(columnName)) {
        if (!first) {
          sqlBuilder.append(", ");
        } else {
          first = false;
        }
        sqlBuilder.append(columnName);
        sqlBuilder.append(" = :");

        if (versionPropertyName != null && versionPropertyName.equals(columnName)) {
          sqlBuilder.append("incrementedVersion");
        } else {
          sqlBuilder.append(convertSnakeToCamelCase(columnName));
        }
      }
    }
    // build where clause
    sqlBuilder.append(" where id = :id");
    if (versionPropertyName != null && updateColumnNameList.contains(versionPropertyName)) {
      sqlBuilder
          .append(" and ")
          .append(versionPropertyName)
          .append(" = :")
          .append(versionPropertyName);
    }

    String updateSql = sqlBuilder.toString();
    updateSqlCache.put(tableName, updateSql);
    return updateSql;
  }

  /**
   * Used by mappers to instantiate object from the result set
   *
   * @param clazz - Class of object to be instantiated
   * @param rs - Sql result set
   * @param prefix - The sql alias in the query (if any)
   * @param resultSetColumnNames - the column names in the sql statement.
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
        if (resultSetColumnNames.contains(columnName)) {
          Object columnVal = rs.getObject(columnName);
          bw.setPropertyValue(propName, columnVal);
        }
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
   * @param pojo - The object to convert
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
   * @param pojo - The object to be converted.
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
   * @param table - table name
   * @return the list of columns of the table
   */
  private List<String> getDbColumnNames(String table) {
    List<String> columns = tableColumnNamesCache.get(table);
    if (isEmpty(columns)) {
      columns = new ArrayList<>();
      try {
        DatabaseMetaData metadata = jdbcTemplate.getDataSource().getConnection().getMetaData();
        ResultSet resultSet = metadata.getColumns(null, schemaName, table, null);
        while (resultSet.next()) {
          columns.add(resultSet.getString("COLUMN_NAME"));
        }
        if (isEmpty(columns)) {
          throw new RuntimeException("Invalid table name: " + table);
        }
        tableColumnNamesCache.put(table, columns);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return columns;
  }

  /**
   * Gets the resultSet column names ie the column names in the 'select' statement of the sql
   *
   * @param rs - ResultSet
   * @return List of select column names
   */
  private List<String> getResultSetColumnNames(ResultSet rs) {
    List<String> rsColNames = new ArrayList<>();
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numberOfColumns = rsmd.getColumnCount();
      // jdbc indexes start at 1
      for (int i = 1; i <= numberOfColumns; i++) {
        rsColNames.add(rsmd.getColumnName(i));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return rsColNames;
  }

  /**
   * Get property names of an object. The property names are cached by the object class name
   *
   * @param pojo - the java object
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
   * @param obj - the Object
   * @param propertyName - the property name
   * @return The property value
   */
  private Object getSimpleProperty(Object obj, String propertyName) {
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    return bw.getPropertyValue(propertyName);
  }

  /**
   * Converts camel case to snake case. Ex: userLastName gets converted to user_last_name. The
   * conversion info is cached.
   *
   * @param str - camel case String
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
   * @param str - snake case string
   * @return the camel case string
   */
  private String convertSnakeToCamelCase(String str) {
    String camelCase = snakeToCamelCache.get(str);
    if (camelCase == null) {
      if (str != null) {
        camelCase = toCamelCase(str, false, new char[] {'_'});
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

  /**
   * Code copied from apache common CaseUtils project. Converts all the delimiter separated words in
   * a String into camelCase, that is each word is made up of a titlecase character and then a
   * series of lowercase characters.
   *
   * <p>The delimiters represent a set of characters understood to separate words. The first
   * non-delimiter character after a delimiter will be capitalized. The first String character may
   * or may not be capitalized and it's determined by the user input for capitalizeFirstLetter
   * variable.
   *
   * <p>A <code>null</code> input String returns <code>null</code>. Capitalization uses the Unicode
   * title case, normally equivalent to upper case and cannot perform locale-sensitive mappings.
   *
   * <pre>
   * CaseUtils.toCamelCase(null, false)                                 = null
   * CaseUtils.toCamelCase("", false, *)                                = ""
   * CaseUtils.toCamelCase(*, false, null)                              = *
   * CaseUtils.toCamelCase(*, true, new char[0])                        = *
   * CaseUtils.toCamelCase("To.Camel.Case", false, new char[]{'.'})     = "toCamelCase"
   * CaseUtils.toCamelCase(" to @ Camel case", true, new char[]{'@'})   = "ToCamelCase"
   * CaseUtils.toCamelCase(" @to @ Camel case", false, new char[]{'@'}) = "toCamelCase"
   * </pre>
   *
   * @param str the String to be converted to camelCase, may be null
   * @param capitalizeFirstLetter boolean that determines if the first character of first word
   *     should be title case.
   * @param delimiters set of characters to determine capitalization, null and/or empty array means
   *     whitespace
   * @return camelCase of String, <code>null</code> if null String input
   */
  private String toCamelCase(
      String str, final boolean capitalizeFirstLetter, final char... delimiters) {
    if (isEmpty(str)) {
      return str;
    }
    str = str.toLowerCase();
    final int strLen = str.length();
    final int[] newCodePoints = new int[strLen];
    int outOffset = 0;
    final Set<Integer> delimiterSet = generateDelimiterSet(delimiters);
    boolean capitalizeNext = false;
    if (capitalizeFirstLetter) {
      capitalizeNext = true;
    }
    for (int index = 0; index < strLen; ) {
      final int codePoint = str.codePointAt(index);

      if (delimiterSet.contains(codePoint)) {
        capitalizeNext = true;
        if (outOffset == 0) {
          capitalizeNext = false;
        }
        index += Character.charCount(codePoint);
      } else if (capitalizeNext || outOffset == 0 && capitalizeFirstLetter) {
        final int titleCaseCodePoint = Character.toTitleCase(codePoint);
        newCodePoints[outOffset++] = titleCaseCodePoint;
        index += Character.charCount(titleCaseCodePoint);
        capitalizeNext = false;
      } else {
        newCodePoints[outOffset++] = codePoint;
        index += Character.charCount(codePoint);
      }
    }
    if (outOffset != 0) {
      return new String(newCodePoints, 0, outOffset);
    }
    return str;
  }

  /**
   * Code copied from apache common CaseUtils project. Used by toCamelCase() method. Converts an
   * array of delimiters to a hash set of code points. Code point of space(32) is added as the
   * default value. The generated hash set provides O(1) lookup time.
   *
   * @param delimiters set of characters to determine capitalization, null means whitespace
   * @return Set<Integer>
   */
  private Set<Integer> generateDelimiterSet(final char[] delimiters) {
    final Set<Integer> delimiterHashSet = new HashSet<>();
    delimiterHashSet.add(Character.codePointAt(new char[] {' '}, 0));
    if (delimiters == null || delimiters.length == 0) {
      return delimiterHashSet;
    }

    for (int index = 0; index < delimiters.length; index++) {
      delimiterHashSet.add(Character.codePointAt(delimiters, index));
    }
    return delimiterHashSet;
  }
}
