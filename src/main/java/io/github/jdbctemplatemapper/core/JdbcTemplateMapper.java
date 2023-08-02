package io.github.jdbctemplatemapper.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.util.Assert;

/**
 * <pre>
 * Spring's JdbcTemplate gives full control of data access using SQL. It is a better option for complex
 * enterprise applications than an ORM (ORM magic/nuances get in the way for large and complex
 * applications). Even though JdbcTemplate abstracts away a lot of the boiler plate code needed by JDBC, it
 * still remains verbose.
 *
 * JdbcTemplateMapper makes CRUD with JdbcTemplate simpler. (Its constructor take JdbcTemplate as an argument).
 * Use it for one line CRUD operations and for other query stuff use JdbcTemplate as you normally would.
 * 
 * Features:
 * 1) One liners for CRUD. To keep the library as simple possible it only has 2 annotations.
 * 2) Can be configured for:
 *     *) auto assign created on, updated on.
 *     *) auto assign created by, updated by using an implementation of IRecordOperatorResolver.
 *     *) optimistic locking functionality for updates by configuring a version property.
 * 3) Thread safe so just needs a single instance (similar to JdbcTemplate)
 * 4) To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section further below.
 * 5) Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases).
 *    Should work with other relational databases. 
 *
 * JdbcTemplateMapper is opinionated.
 * Projects have to meet the following 2 criteria to use it:
 * 1) Camel case object property names are mapped to snake case table column names.
 *    Properties of a model like 'firstName', 'lastName' will be mapped to corresponding columns
 *    'first_name' and 'last_name' in the database table (If for a model property 
 *    a column match is not found, those properties are ignored during CRUD operations) 
 * 2) The table columns map to object properties and have no concept of relationships. So foreign keys in your
 *    table will need a corresponding extra field in the model.
 *    For example if an 'Order" is tied to a 'Customer' to match the 'customer_id' column in the 'order' table you 
 *    will need to have the 'customerId' property in your 'Order' model.
 *
 * Examples of CRUD:
 *
 * // Product class below maps to 'product' table by default.
 * // Use annotation {@literal @}Table(name="some_tablename") to override the default
 *
 * public class Product {
 *    // {@literal @}Id annotation is required.
 *    // For a auto increment database id use @Id(type=IdType.AUTO_INCREMENT)
 *    // For a non auto increment id use @Id. In this case you will have to manually set id value before insert.
 *
 *    {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *    private Integer id;
 *    private String productName;
 *    private Double price;
 *    private LocalDateTime availableDate;
 *
 *    // insert/update/find.. methods will ignore properties which do not
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
 * List{@literal <Product>} products = jdbcTemplateMapper.findAll(Product.class);
 *
 * jdbcTemplateMapper.delete(product);
 *
 * Installation:
 * Requires Java8 or above and dependencies are the same as that for Spring's JdbcTemplate
 *
 * pom.xml dependencies
 * 
 *{@code
 *  <dependency>
 *   <groupId>io.github.jdbctemplatemapper</groupId>
 *   <artifactId>jdbctemplatemapper</artifactId>
 *   <version>0.5.1-SNAPSHOT</version>
 * </dependency>
 * }
 * 
 * Make sure to include the following Spring entry for JdbcTemplate
 * {@code
 *  <dependency>
 *    <groupId>org.springframework.boot</groupId>
 *    <artifactId>spring-boot-starter-jdbc</artifactId>
 * </dependency>
 * }
 *
 *
 * Spring bean configuration for JdbcTemplateMapper:
 * 1) Configure JdbcTemplate bean as per Spring documentation
 * 
 * 2) Configure the JdbcTemplateMapper bean:
 * {@literal @}Bean
 * public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
 *
 *   return new JdbcTemplateMapper(jdbcTemplate);
 *
 *   // JdbcTemplateMapper needs to get database metadata to generate the sql statements.
 *   // Databases may differ on what criteria is needed to retrieve the information. JdbcTemplateMapper
 *   // has multiple constructors so use the appropriate one. In most cases 'new JdbcTemplateMapper(jdbcTemplate);'
 *   // will do the job.
 * }
 *
 * </pre>
 * 
 * 
 * Logging:
 * # log the sql
 * logging.level.org.springframework.jdbc.core.JdbcTemplate=TRACE
 * 
 * # need this for insert statements
 * logging.level.org.springframework.jdbc.core.simple.SimpleJdbcInsert=TRACE
 *
 * # log the parameters of sql statement
 * logging.level.org.springframework.jdbc.core.StatementCreatorUtils=TRACE
 *
 * @author ajoseph
 */
public class JdbcTemplateMapper {
  private final JdbcTemplate jdbcTemplate;
  private final NamedParameterJdbcTemplate npJdbcTemplate;

  private final MappingHelper mappingHelper;

  private IRecordOperatorResolver recordOperatorResolver;

  private String createdByPropertyName;
  private String createdOnPropertyName;
  private String updatedByPropertyName;
  private String updatedOnPropertyName;
  private String versionPropertyName;

  // Inserts use SimpleJdbcInsert. Since SimpleJdbcInsert is thread safe, cache it
  // Map key - table name,
  //     value - SimpleJdcInsert object for the specific table
  private Map<String, SimpleJdbcInsert> simpleJdbcInsertCache = new ConcurrentHashMap<>();

  // update sql cache
  // Map key   - table name or sometimes tableName-updatePropertyName1-updatePropertyName2...
  //     value - the update sql
  private Map<String, UpdateSqlAndParams> updateSqlAndParamsCache = new ConcurrentHashMap<>();

  /**
   * @param jdbcTemplate - The jdbcTemplate
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    this(jdbcTemplate, null, null, null);
  }

  /**
   * @param jdbcTemplate - The jdbcTemplate
   * @param schemaName database schema name.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName) {
    this(jdbcTemplate, schemaName, null, null);
  }

  /**
   * @param jdbcTemplate - The jdbcTemplate
   * @param schemaName database schema name.
   * @param catalogName database catalog name.
   */
  public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName) {
    this(jdbcTemplate, schemaName, catalogName, null);
  }

  /**
   * @param jdbcTemplate - The jdbcTemplate
   * @param schemaName - database schema name.
   * @param catalogName - database catalog name.
   * @param metaDataColumnNamePattern - For most jdbc drivers getting column metadata from database
   *     the metaDataColumnNamePattern argument of null returns all the columns (which is the
   *     default for JdbcTemplateMapper). Some jdbc drivers may require to pass something like '%'.
   */
  public JdbcTemplateMapper(
      JdbcTemplate jdbcTemplate,
      String schemaName,
      String catalogName,
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

  public MappingHelper getMappingHelper() {
    return mappingHelper;
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
   * An implementation of IRecordOperatorResolver is used to populate the created by and updated by
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
   * be assigned the value from IRecordOperatorResolver.getRecordOperator() when the object is
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
   * assigned the value from IRecordOperatorResolver.getRecordOperator when the object is updated in
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
   * Returns the object by Id. Return null if not found
   *
   * @param id Id of object
   * @param clazz Class of object
   * @param <T> the type of the object
   * @return the object of the specific type
   */
  public <T> T findById(Object id, Class<T> clazz) {
    Assert.notNull(clazz, "Class must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String sql =
        "SELECT * FROM "
            + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName())
            + " WHERE "
            + tableMapping.getIdColumnName()
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

    String tableName = mappingHelper.getTableMapping(clazz).getTableName();
    String sql = "SELECT * FROM " + mappingHelper.fullyQualifiedTableName(tableName);
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

    String tableName = mappingHelper.getTableMapping(clazz).getTableName();
    String sql =
        "SELECT * FROM " + mappingHelper.fullyQualifiedTableName(tableName) + " " + orderByClause;
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  /**
   * Inserts an object whose id in database is auto increment. Once inserted the object will have
   * the id assigned.
   *
   * <p>Also assigns created by, created on, updated by, updated on, version if these properties
   * exist for the object and the JdbcTemplateMapper is configured for them.
   *
   * @param obj The object to be saved
   */
  public void insert(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);

    Object idValue = bw.getPropertyValue(tableMapping.getIdPropertyName());

    if (tableMapping.isIdAutoIncrement()) {
      if (idValue != null) {
        throw new RuntimeException(
            "For insert() the object's "
                + tableMapping.getIdPropertyName()
                + " property has to be null since this insert is for an object whose id is auto increment.");
      }
    } else {
      if (idValue == null) {
        throw new RuntimeException(
            "For insert() "
                + obj.getClass().getName()
                + "."
                + tableMapping.getIdPropertyName()
                + " property cannot be null");
      }
    }

    LocalDateTime now = LocalDateTime.now();

    if (createdOnPropertyName != null
        && tableMapping.getColumnName(createdOnPropertyName) != null) {
      bw.setPropertyValue(createdOnPropertyName, now);
    }

    if (createdByPropertyName != null
        && recordOperatorResolver != null
        && tableMapping.getColumnName(createdByPropertyName) != null) {
      bw.setPropertyValue(createdByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    if (updatedOnPropertyName != null
        && tableMapping.getColumnName(updatedOnPropertyName) != null) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && tableMapping.getColumnName(updatedByPropertyName) != null) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }
    if (versionPropertyName != null && tableMapping.getColumnName(versionPropertyName) != null) {
      bw.setPropertyValue(versionPropertyName, 1);
    }

    Map<String, Object> attributes = mappingHelper.convertToSnakeCaseAttributes(obj);

    SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(tableMapping.getTableName());
    if (jdbcInsert == null) {
      if (tableMapping.isIdAutoIncrement()) {
        jdbcInsert =
            new SimpleJdbcInsert(jdbcTemplate)
                .withCatalogName(mappingHelper.getCatalogName())
                .withSchemaName(mappingHelper.getSchemaName())
                .withTableName(tableMapping.getTableName())
                .usingGeneratedKeyColumns(tableMapping.getIdColumnName());
      } else {
        jdbcInsert =
            new SimpleJdbcInsert(jdbcTemplate)
                .withCatalogName(mappingHelper.getCatalogName())
                .withSchemaName(mappingHelper.getSchemaName())
                .withTableName(tableMapping.getTableName());
      }

      simpleJdbcInsertCache.put(tableMapping.getTableName(), jdbcInsert);
    }

    if (tableMapping.isIdAutoIncrement()) {
      Number idNumber = jdbcInsert.executeAndReturnKey(attributes);
      bw.setPropertyValue(
          tableMapping.getIdPropertyName(), idNumber); // set the auto increment id value on object
    } else {
      jdbcInsert.execute(attributes);
    }
  }

  /**
   * Updates object. Assigns updated by, updated on if these properties exist for the object and the
   * jdbcTemplateMapper is configured for these fields. if optimistic locking 'version' property
   * exists for object throws an OptimisticLockingException if object is stale
   *
   * @param obj object to be updated
   * @return number of records updated
   */
  public Integer update(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

    UpdateSqlAndParams updateSqlAndParams = updateSqlAndParamsCache.get(obj.getClass().getName());

    if (updateSqlAndParams == null) {
      // ignore these attributes when generating the sql 'SET' command
      List<String> ignoreAttrs = new ArrayList<>();
      ignoreAttrs.add(tableMapping.getIdPropertyName());
      if (createdByPropertyName != null) {
        ignoreAttrs.add(createdByPropertyName);
      }
      if (createdOnPropertyName != null) {
        ignoreAttrs.add(createdOnPropertyName);
      }

      Set<String> updatePropertyNames = new LinkedHashSet<>();

      for (PropertyInfo propertyInfo : mappingHelper.getObjectPropertyInfo(obj)) {
        // if not a ignore property and has a table column mapping add it to the update property
        // list
        if (!ignoreAttrs.contains(propertyInfo.getPropertyName())
            && tableMapping.getColumnName(propertyInfo.getPropertyName()) != null) {
          updatePropertyNames.add(propertyInfo.getPropertyName());
        }
      }
      updateSqlAndParams = buildUpdateSqlAndParams(tableMapping, updatePropertyNames);
      updateSqlAndParamsCache.put(obj.getClass().getName(), updateSqlAndParams);
    }
    return issueUpdate(updateSqlAndParams, obj, tableMapping);
  }

  /**
   * Updates the propertyNames (passed in as args) of the object. Assigns updated by, updated on if
   * these properties exist for the object and the jdbcTemplateMapper is configured for these
   * fields.
   *
   * @param obj object to be updated
   * @param propertyNames array of property names that should be updated
   * @return number of records updated (1 or 0)
   */
  public Integer update(Object obj, String... propertyNames) {
    Assert.notNull(obj, "Object must not be null");
    Assert.notNull(propertyNames, "propertyNames must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

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
                  + tableMapping.getTableName());
        }
      }

      // auto assigned  cannot be updated by user.
      List<String> autoAssignedAttrs = new ArrayList<>();
      autoAssignedAttrs.add(tableMapping.getIdPropertyName());
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

      updateSqlAndParams = buildUpdateSqlAndParams(tableMapping, updatePropertyNames);
      updateSqlAndParamsCache.put(cacheKey, updateSqlAndParams);
    }
    return issueUpdate(updateSqlAndParams, obj, tableMapping);
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

    String sql =
        "delete from "
            + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName())
            + " where "
            + tableMapping.getIdColumnName()
            + "= ?";
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    Object id = bw.getPropertyValue(tableMapping.getIdPropertyName());
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Physically Deletes the object from the database by id
   *
   * @param <T> This describes class type
   * @param id Id of object to be deleted
   * @param clazz Type of object to be deleted.
   * @return number records were deleted (1 or 0)
   */
  public <T> Integer deleteById(Object id, Class<T> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(id, "id must not be null");

    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    String sql =
        "delete from "
            + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName())
            + " where "
            + tableMapping.getIdColumnName()
            + " = ?";
    return jdbcTemplate.update(sql, id);
  }

  private UpdateSqlAndParams buildUpdateSqlAndParams(
      TableMapping tableMapping, Set<String> propertyNames) {
    Assert.notNull(tableMapping, "tableMapping must not be null");
    Assert.notNull(propertyNames, "propertyNames must not be null");

    Set<String> params = new HashSet<>();
    StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
    sqlBuilder.append(mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()));
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
    sqlBuilder.append(
        " WHERE " + tableMapping.getIdColumnName() + " = :" + tableMapping.getIdPropertyName());
    params.add(tableMapping.getIdPropertyName());

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

  private Integer issueUpdate(
      UpdateSqlAndParams updateSqlAndParams, Object obj, TableMapping tableMapping) {
    Assert.notNull(updateSqlAndParams, "updateSqlAndParams must not be null");
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

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
    for (String paramName : parameters) {
      if (paramName.equals("incrementedVersion")) {
        Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
        if (versionVal == null) {
          throw new RuntimeException(
              versionPropertyName
                  + " cannot be null when updating "
                  + obj.getClass().getSimpleName());
        } else {
          mapSqlParameterSource.addValue(
              "incrementedVersion", versionVal + 1, java.sql.Types.INTEGER);
        }
      } else {
        mapSqlParameterSource.addValue(
            paramName, bw.getPropertyValue(paramName), tableMapping.getPropertySqlType(paramName));
      }
    }

    // if object has property version the version gets incremented on update.
    // throws OptimisticLockingException when update fails.
    if (updateSqlAndParams.getParams().contains("incrementedVersion")) {
      int cnt = npJdbcTemplate.update(updateSqlAndParams.getSql(), mapSqlParameterSource);
      if (cnt == 0) {
        throw new OptimisticLockingException(
            "Update failed for "
                + obj.getClass().getSimpleName()
                + " for "
                + tableMapping.getIdPropertyName()
                + ":"
                + bw.getPropertyValue(tableMapping.getIdPropertyName())
                + " and "
                + versionPropertyName
                + ":"
                + bw.getPropertyValue(versionPropertyName));
      }
      // update the version in object with new version
      bw.setPropertyValue(
          versionPropertyName, mapSqlParameterSource.getValue("incrementedVersion"));
      return cnt;
    } else {
      return npJdbcTemplate.update(updateSqlAndParams.getSql(), mapSqlParameterSource);
    }
  }
}
