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
 * 1) Simple CRUD one liners using spring's JDBC template.
 *
 * <p>2) Methods to map relationships (toOne, toMany etc)
 *
 * <p>3) Uses an implementation or IRecordOperatorResolver to populate createdBy, updateBy ..
 * fields.
 *
 * <p>Code written so there are no external dependencies other than spring framework
 *
 * @author ajoseph
 */
public class JdbcMapper {
  private NamedParameterJdbcTemplate npJdbcTemplate;
  private JdbcTemplate jdbcTemplate;
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

  // Convert camel case to snake case regex pattern
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

  // Constructor
  public JdbcMapper(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
    this.npJdbcTemplate = namedParameterJdbcTemplate;
    this.jdbcTemplate = namedParameterJdbcTemplate.getJdbcTemplate();
  }

  public JdbcMapper withRecordOperatorResolver(IRecordOperatorResolver recordOperatorResolver) {
    this.recordOperatorResolver = recordOperatorResolver;
    return this;
  }

  public JdbcMapper withSchemaName(String schemaName) {
    this.schemaName = schemaName;
    return this;
  }

  public JdbcMapper withCreatedByPropertyName(String propName) {
    this.createdByPropertyName = propName;
    return this;
  }

  public JdbcMapper withCreatedOnPropertyName(String propName) {
    this.createdOnPropertyName = propName;
    return this;
  }

  public JdbcMapper withUpdatedByPropertyName(String propName) {
    this.updatedByPropertyName = propName;
    return this;
  }

  public JdbcMapper withUpdatedOnPropertyName(String propName) {
    this.updatedOnPropertyName = propName;
    return this;
  }

  public JdbcMapper withVersionPropertyName(String propName) {
    this.versionPropertyName = propName;
    return this;
  }
  
  /**
   * Returns the object by Id. Return null if not found
   *
   * @param id - Id of object
   * @param type - Class of object
   * @return - The object of the specific type
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
   * @param clazz - Type of object
   * @return List of objects
   */
  public <T> List<T> findAll(Class<T> clazz) {
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "select * from " + schemaName + "." + tableName;
    RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
    return jdbcTemplate.query(sql, mapper);
  }

  /**
   * Find all objects with the order by clause passed as argument
   *
   * @param clazz - Type of object
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
   * <p>Also assigns createdBy, createdOn, updatedBy, updatedOn, version if these properties exist
   * for the object
   *
   * @param pojo - The object to be saved
   */
  public void insert(Object pojo) {
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

    Map<String, Object> attributes = convertToDbColumnAttributes(pojo);

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
   * has to be assigned up front and cannot be null. 
   *
   * <p>Also assigns createdBy, createdOn, updatedBy, updatedOn, version if these properties exist
   * for the object
   *
   * @param pojo - The object to be saved
   */
  public void insertWithId(Object pojo) {
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

    Map<String, Object> attributes = convertToDbColumnAttributes(pojo);
    jdbcInsert.execute(attributes);
  }

  /**
   * Updates object. Will also set updatedBy and updatedOn values if these properties exist for the
   * object. if 'version' property exists for object throws an OptimisticLockingException if fails
   * to update the record
   *
   * @param pojo - object to be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object pojo) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
    String updateSql = updateSqlCache.get(tableName);
    if (updateSql == null) {
      updateSql = buildUpdateSql(pojo);
    }

    LocalDateTime now = LocalDateTime.now();
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);

    if (updatedOnPropertyName != null && bw.isReadableProperty(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }

    Map<String, Object> attributes = convertObjectToMap(pojo);
    // if object has property version throw OptimisticLockingException
    // when update fails. The version gets incremented on update
    if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
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
   * Updates the propertyNames (passed in as args) of the object. Will also set updatedBy and
   * updatedOn values if these properties exist for the object
   *
   * @param pojo - object to be updated
   * @param propertyNames - array of property names that should be updated
   * @return 0 if no records were updated
   */
  public Integer update(Object pojo, String... propertyNames) {
    String tableName = convertCamelToSnakeCase(pojo.getClass().getSimpleName());
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

      List<String> dbColumnNameList = new ArrayList<>();
      for (String propertyName : propertyNames) {
        dbColumnNameList.add(convertCamelToSnakeCase(propertyName));
      }

      // add updated info to the column list
      if (updatedOnPropertyName != null && bw.isReadableProperty(updatedOnPropertyName)) {
        dbColumnNameList.add(convertCamelToSnakeCase(updatedOnPropertyName));
      }
      if (updatedByPropertyName != null
          && recordOperatorResolver != null
          && bw.isReadableProperty(updatedByPropertyName)) {
        dbColumnNameList.add(convertCamelToSnakeCase(updatedByPropertyName));
      }

      boolean first = true;
      for (String columnName : dbColumnNameList) {
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
      if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
        sqlBuilder.append(", ").append(versionPropertyName).append(" = :incrementedVersion");
      }
      // the where clause
      sqlBuilder.append(" where id = :id");
      if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
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
    if (updatedOnPropertyName != null && bw.isReadableProperty(updatedOnPropertyName)) {
      bw.setPropertyValue(updatedOnPropertyName, now);
    }
    if (updatedByPropertyName != null
        && recordOperatorResolver != null
        && bw.isReadableProperty(updatedByPropertyName)) {
      bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
    }

    Map<String, Object> attributes = convertObjectToMap(pojo);
    // if object has property version throw OptimisticLockingException
    // update fails. The version gets incremented
    if (versionPropertyName != null && bw.isReadableProperty(versionPropertyName)) {
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
    String tableName = convertCamelToSnakeCase(clazz.getSimpleName());
    String sql = "delete from " + schemaName + "." + tableName + " where id = ?";
    return jdbcTemplate.update(sql, id);
  }

  /**
   * Get the next sequence number for the sequence name
   *
   * @param sequenceName - The name of the sequence
   * @return the next sequence number
   */
  public Integer getNextSequence(String sequenceName) {
    String sql = "select nextval('" + sequenceName + "')";
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }

  /**
   * Populates the toOne relationship. Issues an sql query to get the relationship.
   *
   * <p>The join property to tie the mainObj to the relatedObject is figured out from the
   * relationshipClazz name For example if the relationShipClass is 'Project' the join property will
   * be 'projectId' of the mainObj. Make sure the join property of the argument mainObj is assigned
   * so it can be tied to its corresponding relationship object.
   *
   * @param mainObj - the main object
   * @param relationshipPropertyName - The propertyName of the toOne relationship (on mainOjb)
   * @param relationShipClazz - The relationship class
   */
  public <T, U> void toOneForObject(
      T mainObj, String relationshipPropertyName, Class<U> relationshipClazz) {
    List<T> mainObjList = new ArrayList<>();
    if (mainObj != null) {
      mainObjList.add(mainObj);
      toOneForList(mainObjList, relationshipPropertyName, relationshipClazz);
    }
  }

  /**
   * Populates the toOne relationship for all the main objects in the argument list. Issues an sql
   * query using the 'IN' clause to get all the relationship objects corresponding to the main
   * object list.
   *
   * <p>The join property to tie the mainObj to the relatedObject is figured out from the
   * relationshipClazz name For example if the relationShipClass is 'Project' the join property will
   * be 'projectId' of the mainObj. Make sure the join property of the argument mainObj is assigned
   * so it can be tied to its corresponding relationship object.
   *
   * @param mainObjList - list of main objects
   * @param relationshipPropertyName - The propertyName of the toOne relationship
   * @param relationShipClazz - The relationship class
   */
  public <T, U> void toOneForList(
      List<T> mainObjList, String relationshipPropertyName, Class<U> relationshipClazz) {
    String tableName = convertCamelToSnakeCase(relationshipClazz.getSimpleName());
    if (isNotEmpty(mainObjList)) {
      String joinColumnName = tableName + "_id";
      String joinPropertyName = convertSnakeToCamelCase(joinColumnName);

      List<Number> allColumnIds = new ArrayList<>();
      for (T mainObj : mainObjList) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
        Number joinPropertyValue = (Number) bw.getPropertyValue(joinPropertyName);
        if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
          allColumnIds.add((Number) bw.getPropertyValue(joinPropertyName));
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

      toOneMerge(mainObjList, relatedObjList, relationshipPropertyName, joinPropertyName);
    }
  }

  /**
   * Populates a single main object and its toOne relationship object with the data from the
   * resultSet using their respective SqlMappers.
   *
   * <p>The jdbc ResultSet argument object should have the join property assigned so the code can
   * tie the the main object and relationship object together.
   *
   * <p>The join property name on the main object is figured out from the mapper class of the
   * related object.
   *
   * @param rs - The jdbc ResultSet
   * @param mainObjMapper - The main object mapper.
   * @param relationshipPropertyName - The toOne relationship name on mainObj
   * @param relatedObjMapper - The related object mapper
   * @return The main object with its toOne relationship populated.
   */
  @SuppressWarnings("all")
  public <T, U> T toOneMapperForObject(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String relationshipPropertyName,
      SelectMapper<U> relatedObjMapper) {
    List<T> list =
        toOneMapperForList(rs, mainObjMapper, relationshipPropertyName, relatedObjMapper);
    return isNotEmpty(list) ? list.get(0) : null;
  }

  /**
   * Populates the main object list with their corresponding toOne relationship object from the jdbc
   * ResultSet using their respective SqlMappers.
   *
   * <p>The jdbc ResultSet argument object should have the join property assigned so the code can
   * tie the the main object and relationship object together.
   *
   * <p>The join property name on the main object is figured out from the mapper class of the
   * related object.
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper
   * @param relationshipPropertyName
   * @param relatedObjMapper
   * @return List of mainObj with its toOne property assigned
   */
  @SuppressWarnings("all")
  public <T, U> List<T> toOneMapperForList(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String relationshipPropertyName,
      SelectMapper<U> relatedObjMapper) {

    Map<String, List> resultMap = multipleModelMapper(rs, mainObjMapper, relatedObjMapper);

    List<T> mainObjList = resultMap.get(mainObjMapper.getSqlColumnPrefix());
    List<U> relatedObjList = resultMap.get(relatedObjMapper.getSqlColumnPrefix());

    String joinPropertyName =
        toLowerCaseFirstChar(relatedObjMapper.getClazz().getSimpleName()) + "Id";

    toOneMerge(mainObjList, relatedObjList, relationshipPropertyName, joinPropertyName);

    return mainObjList;
  }

  /**
   * Merges relatedObjeList to the mainObj list by assigning the relationshipPropertyName on mainObj
   * using the join property name.
   *
   * @param mainObjList
   * @param relatedObjList
   * @param relationshipPropertyName
   * @param joinPropertyName
   */
  public <T, U> void toOneMerge(
      List<T> mainObjList,
      List<U> relatedObjList,
      String relationshipPropertyName,
      String joinPropertyName) {

    if (isNotEmpty(mainObjList) && isNotEmpty(relatedObjList)) {
      Map<Number, U> idToObjectMap =
          relatedObjList
              .stream()
              .collect(Collectors.toMap(e -> (Number) getSimpleProperty(e, "id"), obj -> obj));

      for (T mainObj : mainObjList) {

        Number joinPropertyValue = (Number) getSimpleProperty(mainObj, joinPropertyName);
        if (joinPropertyValue != null && joinPropertyValue.longValue() > 0) {
          BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
          bw.setPropertyValue(relationshipPropertyName, idToObjectMap.get(joinPropertyValue));
        }
      }
    }
  }

  /**
   * Populates the collectionPropertyName of the mainObj.
   *
   * <p>Executes a query with 'IN' clause to get the many side records
   *
   * @param mainObjList - the main object list
   * @param collectionPropertyName - The collection property name on mainObj
   * @param manySideClass - The many side class
   * @param orderByClause - The order by clause for the many side query
   */
  public <T, U> void toManyForObject(
      T mainObj, String collectionPropertyName, Class<U> manySideClazz, String orderByClause) {
    List<T> mainObjList = new ArrayList<>();
    mainObjList.add(mainObj);
    toManyForList(mainObjList, collectionPropertyName, manySideClazz, orderByClause);
  }

  /**
   * When provided a mainObj list populates its collectionPropertyName.
   *
   * <p>Executes a query with 'IN' clause to get the many side records
   *
   * @param mainObjList - the main object list
   * @param collectionPropertyName - The collection property name on mainObj
   * @param manySideClass - The many side class
   * @param orderByClause - The order by clause for the many side query
   */
  public <T, U> void toManyForList(
      List<T> mainObjList,
      String collectionPropertyName,
      Class<U> manySideClazz,
      String orderByClause) {
    String tableName = convertCamelToSnakeCase(manySideClazz.getSimpleName());
    if (isNotEmpty(mainObjList)) {
      Set<Number> allIds = new LinkedHashSet<>();
      for (T mainObj : mainObjList) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
        Number idVal = (Number) bw.getPropertyValue("id");
        if (idVal != null && idVal.longValue() > 0) {
          allIds.add((idVal));
        } else {
          throw new RuntimeException("id property in mainObjList cannot be null");
        }
      }

      List<Number> uniqueIds = new ArrayList<>(allIds);

      String joinColumnName =
          convertCamelToSnakeCase(mainObjList.get(0).getClass().getSimpleName()) + "_id";
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
        if (isNotEmpty(orderByClause)) {
          sql += " " + orderByClause;
        } else {
          sql += " order by id";
        }
        MapSqlParameterSource params = new MapSqlParameterSource("columnIds", columnIds);
        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(manySideClazz);
        manySideList.addAll(npJdbcTemplate.query(sql, params, mapper));
      }

      String joinPropertyName = convertSnakeToCamelCase(joinColumnName);
      toManyMerge(mainObjList, manySideList, collectionPropertyName, joinPropertyName);
    }
  }
  /**
   * Populates a single main object and its collection PropertyName object with the data from the
   * resultSet using their respective SqlMappers.
   *
   * <p>The jdbc ResultSet argument object should have the join property assigned so the code can
   * tie the the main objects and its related objects together.
   *
   * <p>The join property name on the main object is figured out from the mapper class of the
   * related object.
   *
   * @param rs - The jdbc ResultSet
   * @param mainObjMapper - The main object mapper.
   * @param relationshipPropertyName - The toOne relationship name on mainObj
   * @param relatedObjMapper - The related object mapper
   * @return The main object with its toMany relationship assigned.
   */
  @SuppressWarnings("all")
  public <T, U> T toManyMapperForObject(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String collectionPropertyName,
      SelectMapper<U> relatedObjMapper) {
    List<T> list = toManyMapperForList(rs, mainObjMapper, collectionPropertyName, relatedObjMapper);
    return isNotEmpty(list) ? list.get(0) : null;
  }

  /**
   * Populates the main object list with their corresponding toMany relationship object from the
   * jdbc ResultSet using their respective SqlMappers.
   *
   * <p>The jdbc ResultSet argument object should have the join property assigned so the code can
   * tie the the main object and related object together.
   *
   * <p>The join property name on the main object is figured out from the mapper class of the
   * related object.
   *
   * @param rs The jdbc ResultSet
   * @param mainObjMapper
   * @param relationshipPropertyName
   * @param relatedObjMapper
   * @return List of mainObj with its collectionPropertyName assigned
   */
  @SuppressWarnings("all")
  public <T, U> List<T> toManyMapperForList(
      ResultSet rs,
      SelectMapper<T> mainObjMapper,
      String collectionPropertyName,
      SelectMapper<U> manySideObjMapper) {

    Map<String, List> resultMap = multipleModelMapper(rs, mainObjMapper, manySideObjMapper);

    List<T> mainObjList = resultMap.get(mainObjMapper.getSqlColumnPrefix());
    List<U> manySideObjList = resultMap.get(manySideObjMapper.getSqlColumnPrefix());

    String joinPropertyName = toLowerCaseFirstChar(mainObjMapper.getClazz().getSimpleName()) + "Id";

    toManyMerge(mainObjList, manySideObjList, collectionPropertyName, joinPropertyName);
    return mainObjList;
  }

  @SuppressWarnings("all")
  public <T, U> void toManyMerge(
      List<T> mainObjList,
      List<U> manySideList,
      String collectionPropertyName,
      String joinPropertyName) {

    try {
      if (isNotEmpty(manySideList)) {
        Map<Number, List<U>> mapColumnIdToManySide =
            manySideList
                .stream()
                .collect(
                    Collectors.groupingBy(e -> (Number) getSimpleProperty(e, joinPropertyName)));

        // assign the manyside list to the mainobj
        for (T mainObj : mainObjList) {
          BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainObj);
          Number idValue = (Number) bw.getPropertyValue("id");
          List<U> relatedList = mapColumnIdToManySide.get(idValue);
          bw.setPropertyValue(collectionPropertyName, relatedList);
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
   * Generates a string which can be used in a sql select statement with all columns of the table.
   *
   * <p>Example: for method call selectCols("employee", "emp") where "emp" is the alias will return:
   * "emp.id emp_id, emp.last_name emp_last_name, emp.first_name emp_first_name" .....
   *
   * @param tableName - the Table name
   * @param tableAlias - the alias being used in the sql statement for the table.
   * @return comma separated select column string
   */
  public String selectCols(String tableName, String tableAlias) {
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
    if (str != null && str.length() > 0) {
      // remove the last comma.
      return str.substring(0, str.length() - 1) + " ";
    } else {
      return null;
    }
  }

  /**
   * Builds sql update statement with named parameters for the object.
   *
   * @param pojo - the object that needs to be update.
   * @return sql update string
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
   * Converts an object to a map with key as database column names. ie the camel case property names
   * are converted to snake case. For example 'userLastName' will get converted to 'user_last_name'
   *
   * @param pojo - The object to convert
   * @return A map with keys that are in snake case to match database column names
   */
  private Map<String, Object> convertToDbColumnAttributes(Object pojo) {
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
   * Converts an object to a Map
   *
   * @param pojo - The object to be converted.
   * @return Map with key: property name, value: object value
   */
  private Map<String, Object> convertObjectToMap(Object pojo) {
    Map<String, Object> camelCaseAttrs = new HashMap<>();
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(pojo);
    List<String> propertyNames = getPropertyNames(pojo);
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
    if (columns == null) {
      columns = new ArrayList<>();
      try {
        DatabaseMetaData metadata = jdbcTemplate.getDataSource().getConnection().getMetaData();
        ResultSet resultSet = metadata.getColumns(null, schemaName, table, null);
        while (resultSet.next()) {
          columns.add(resultSet.getString("COLUMN_NAME"));
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
   * @return List of strings with column name
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
   * @param pojo
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
   * @param obj
   * @param propertyName
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
        // snakeCase = RegExUtils.replaceAll(str, TO_SNAKE_CASE_PATTERN, "$1_$2").toLowerCase();
        camelToSnakeCache.put(str, snakeCase);
      }
    }
    return snakeCase;
  }

  /**
   * Converts sname case to camel case. Ex: user_last_name gets converted to userLastName. The
   * conversion info is cached.
   *
   * @param str - snake case string
   * @return the camel case string
   */
  private String convertSnakeToCamelCase(String str) {
    String camelCase = snakeToCamelCache.get(str);
    if (camelCase == null) {
      camelCase = toCamelCase(str, false, new char[] {'_'});
      snakeToCamelCache.put(str, camelCase);
    }
    return camelCase;
  }

  /**
   * splits the list into multiple lists of chunk size. Used to split the sql IN clauses since some
   * databases have a limitation of 1024. We set the chuck size to IN_CLAUSE_CHUNK_SIZE
   *
   * @param list
   * @param chunkSize
   * @return
   */
  private Collection<List<Number>> chunkList(List<Number> list, Integer chunkSize) {
    AtomicInteger counter = new AtomicInteger();
    Collection<List<Number>> result =
        list.stream()
            .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
            .values();
    return result;
  }

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

  private String toLowerCaseFirstChar(String str) {
    if (str != null) {
      char c[] = str.toCharArray();
      c[0] = Character.toLowerCase(c[0]);
      return new String(c);
    }
    return str;
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
   * Used by toCamelCase() method. Converts an array of delimiters to a hash set of code points.
   * Code point of space(32) is added as the default value. The generated hash set provides O(1)
   * lookup time.
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
