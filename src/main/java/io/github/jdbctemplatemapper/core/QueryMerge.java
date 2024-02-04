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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeFluent;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasMany;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasOne;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumnManySide;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumnTypeSide;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeOrderBy;
import io.github.jdbctemplatemapper.querymerge.IQueryMergePopulateProperty;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeThroughJoinColumns;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeThroughJoinTable;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeType;

/**
 * QueryMerge allows query results to be merged with results of another query. Generally used when
 * multiple relationships need to be populated.
 *
 * <pre>
 *
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#querying-relationships">Querying relationships</a> for more info
 * </pre>
 *
 * @author ajoseph
 */
public class QueryMerge<T> implements IQueryMergeFluent<T> {
  private static final int IN_CLAUSE_CHUNK_SIZE = 100;
  private Class<T> type;
  private String relationshipType;
  private Class<?> relatedType;
  private String relatedTypeTableAlias;
  private String propertyName; // propertyName on type that needs to be populated
  private String joinColumnTypeSide;
  private String joinColumnManySide;

  private String throughJoinTable;
  private String throughTypeJoinColumn;
  private String throughRelatedTypeJoinColumn;
  private String orderBy;

  private QueryMerge(Class<T> type) {
    this.type = type;
  }

  /**
   * The type records the query results are merged to.
   *
   * @param <T> the type
   * @param type The type
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryMergeType<T> type(Class<T> type) {
    Assert.notNull(type, "type cannot be null");
    return new QueryMerge<T>(type);
  }

  /**
   * The type on which the query results are merged to.
   *
   * @param <T> the type
   * @param type The type
   * @return interface with the next methods in the chain
   */

  /**
   * hasOne relationship.
   *
   * @param relatedType the related type
   * @return interface with the next methods in the chain
   */
  public IQueryMergeHasOne<T> hasOne(Class<?> relatedType) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    this.relationshipType = RelationshipType.HAS_ONE;
    this.relatedType = relatedType;
    return this;
  }

  /**
   * hasOne relationship.
   *
   * @param relatedType the related type
   * @param tableAlias the table alias.
   * @return interface with the next methods in the chain
   */
  public IQueryMergeHasOne<T> hasOne(Class<?> relatedType, String tableAlias) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new IllegalArgumentException("tableAlias for type cannot be null or blank");
    }
    this.relationshipType = RelationshipType.HAS_ONE;
    this.relatedType = relatedType;
    this.relatedTypeTableAlias = tableAlias;
    return this;
  }

  /**
   * hasMany relationship.
   *
   * @param relatedType the related type
   * @return interface with the next methods in the chain
   */
  public IQueryMergeHasMany<T> hasMany(Class<?> relatedType) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    this.relationshipType = RelationshipType.HAS_MANY;
    this.relatedType = relatedType;
    return this;
  }

  /**
   * hasMany relationship.
   *
   * @param relatedType the related type
   * @param tableAlias the table alias which can be used in orderBy clause
   * @return interface with the next methods in the chain
   */
  public IQueryMergeHasMany<T> hasMany(Class<?> relatedType, String tableAlias) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new IllegalArgumentException("tableAlias for type cannot be null or blank");
    }
    this.relationshipType = RelationshipType.HAS_MANY;
    this.relatedType = relatedType;
    this.relatedTypeTableAlias = tableAlias;
    return this;
  }

  /**
   * Join column for hasOne relationship. The join column (the foreign key) is on the table of the
   * type model. Example: Order hasOne Customer. The join column(foreign key) will be on the table
   * order (of the type model). The join column should not have a table prefix.
   *
   * @param joinColumnTypeSide the join column on the type side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryMergeJoinColumnTypeSide<T> joinColumnTypeSide(String joinColumnTypeSide) {
    if (MapperUtils.isBlank(joinColumnTypeSide)) {
      throw new IllegalArgumentException("joinColumnTypeSide cannot be null or blank");
    }
    this.joinColumnTypeSide = MapperUtils.toLowerCase(joinColumnTypeSide.trim());
    return this;
  }

  /**
   * Join column for hasMany relationship: The join column (the foreign key) is on the table of the
   * many side. Example: Order hasMany OrderLine. The join column will be on the table order_line
   * (the many side). The join column should not have a table prefix.
   *
   * @param joinColumnManySide the join column on the many side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryMergeJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide) {
    if (MapperUtils.isBlank(joinColumnManySide)) {
      throw new IllegalArgumentException("joinColumnManySide cannot be null or blank");
    }
    this.joinColumnManySide = MapperUtils.toLowerCase(joinColumnManySide.trim());
    return this;
  }

  /**
   * The join table (associative table) for the hasMany through (many to many) relationship.
   *
   * @param tableName the associative table name
   * @return interface with the next methods in the chain
   */
  public IQueryMergeThroughJoinTable<T> throughJoinTable(String tableName) {
    if (MapperUtils.isBlank(tableName)) {
      throw new IllegalArgumentException("throughJoinTable() tableName cannot be null or blank");
    }
    this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
    this.throughJoinTable = tableName;
    return this;
  }

  /**
   * The join columns for the type side and the related side for hasMany through relationship.
   *
   * @param typeJoinColumn the join column for the type side
   * @param relatedTypeJoinColumn the join column for the related side
   * @return interface with the next methods in the chain
   */
  public IQueryMergeThroughJoinColumns<T> throughJoinColumns(String typeJoinColumn,
      String relatedTypeJoinColumn) {
    if (MapperUtils.isBlank(typeJoinColumn)) {
      throw new IllegalArgumentException(
          "throughJoinColumns() typeJoinColumn cannot be null or blank");
    }
    if (MapperUtils.isBlank(relatedTypeJoinColumn)) {
      throw new IllegalArgumentException(
          "throughJoinColumns() relatedTypeJoinColumn cannot be null or blank");
    }
    this.throughTypeJoinColumn = typeJoinColumn;
    this.throughRelatedTypeJoinColumn = relatedTypeJoinColumn;
    return this;
  }

  /**
   * The relationship property that needs to be populated on the type. For hasMany() this property
   * has to be an initialized collection (cannot be null) and the generic type should match the
   * hasMany related type. For hasOne this property has to be the same type as hasOne related type
   *
   * @param propertyName name of property that needs to be populated
   * @return interface with the next methods in the chain
   */
  public IQueryMergePopulateProperty<T> populateProperty(String propertyName) {
    if (MapperUtils.isBlank(propertyName)) {
      throw new IllegalArgumentException("propertyName cannot be null or blank");
    }
    this.propertyName = propertyName;
    return this;
  }

  /**
   * The orderBy clause for QueryMerge. For QueryMerge orderBy is only supported for hasMany and
   * hasMany through. orderBy columns have to be on the table of the hasMany/hasMany through side.
   * For hasOne orderBy does not makes sense because the order will be dictated by the mergeList
   * argument in the execute method.
   *
   * @param orderBy the orderBy clause.
   * @return interface with the next methods in the chain
   */
  public IQueryMergeOrderBy<T> orderBy(String orderBy) {
    if (MapperUtils.isBlank(orderBy)) {
      throw new IllegalArgumentException("orderBy cannot be null or blank");
    }
    this.orderBy = orderBy;
    return this;
  }

  /**
   * The query executes an sql 'IN' clause to get the related side (hasOne, hasMany, hasMany
   * through) objects and merges those with the objects in the mergeList.
   *
   * <pre>
   * If the the mergeList size is larger than 100, multiple 'IN' queries will be issued with each query
   * having up to 100 IN clause parameters to get the records.
   * </pre>
   *
   * @param jdbcTemplateMapper the jdbcTemplateMapper
   * @param mergeList a list of objects of type.
   */
  public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList) {
    Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

    String cacheKey = getCacheKey();
    if (jdbcTemplateMapper.getQueryMergeSqlCache().get(cacheKey) == null) {
      QueryValidator.validate(jdbcTemplateMapper, type, relationshipType, relatedType,
          joinColumnTypeSide, joinColumnManySide, propertyName, throughJoinTable,
          throughTypeJoinColumn, throughRelatedTypeJoinColumn);
    }

    if (RelationshipType.HAS_ONE.equals(relationshipType)) {
      if (MapperUtils.isNotBlank(orderBy)) {
        throw new IllegalArgumentException(
            "For QueryMerge hasOne relationships orderBy is not supported."
                + " The order is already dictated by the mergeList order");
      }
      processHasOne(jdbcTemplateMapper, mergeList, type, relatedType, cacheKey);
    } else if (RelationshipType.HAS_MANY.equals(relationshipType)) {
      processHasMany(jdbcTemplateMapper, mergeList, type, relatedType, cacheKey);
    } else if (RelationshipType.HAS_MANY_THROUGH.equals(relationshipType)) {
      processHasManyThrough(jdbcTemplateMapper, mergeList, type, relatedType, cacheKey);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void processHasOne(JdbcTemplateMapper jtm, List<T> mergeList, Class<?> type,
      Class<?> relatedType, String cacheKey) {

    if (MapperUtils.isEmpty(mergeList)) {
      return;
    }

    TableMapping typeTableMapping = jtm.getTableMapping(type);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);
    String joinPropertyName = typeTableMapping.getPropertyName(joinColumnTypeSide);

    List<BeanWrapper> bwMergeList = new ArrayList<>(mergeList.size());
    Set params = new HashSet<>(mergeList.size());
    for (T obj : mergeList) {
      if (obj != null) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        bw.setPropertyValue(propertyName, null);
        bwMergeList.add(bw);
        Object joinPropertyValue = bw.getPropertyValue(joinPropertyName);
        if (joinPropertyValue != null) {
          params.add(joinPropertyValue);
        }
      }
    }
    if (MapperUtils.isEmpty(params)) {
      return;
    }

    String relatedColumnPrefix =
        MapperUtils.columnPrefix(relatedTypeTableAlias, relatedTypeTableMapping.getTableName());

    SelectMapper<?> selectMapperRelatedType = jtm.getSelectMapperInternal(relatedType,
        relatedColumnPrefix, MapperUtils.RELATED_TABLE_COL_ALIAS_PREFIX);

    boolean foundInCache = false;
    String sql = jtm.getQueryMergeSqlCache().get(cacheKey);
    if (sql == null) {

      String relatedTableStr = MapperUtils.tableStrForFrom(relatedTypeTableAlias,
          relatedTypeTableMapping.fullyQualifiedTableName());

      // @formatter:off
      sql = "SELECT " + selectMapperRelatedType.getColumnsSql() 
          + " FROM " + relatedTableStr
          + " WHERE " + relatedTypeTableMapping.getIdColumnNameForSql() + " IN (:joinPropertyTypeSideValues)";
      
      // @formatter:on
    } else {
      foundInCache = true;
    }

    String relatedModelIdPropName = relatedTypeTableMapping.getIdPropertyName();
    Map<Object, Object> idToRelatedModelMap = new HashMap<>(mergeList.size());

    ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
      public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
          BeanWrapper bwRelatedModel = selectMapperRelatedType.buildBeanWrapperModel(rs);
          if (bwRelatedModel != null) {
            idToRelatedModelMap.put(bwRelatedModel.getPropertyValue(relatedModelIdPropName),
                bwRelatedModel.getWrappedInstance());
          }
        }
        return null;
      }
    };

    // some databases have limits on number of entries in a 'IN' clause
    // Chunk the list and make multiple calls as needed.
    List<List<?>> chunkedJoinPropertyTypeSideValues =
        MapperUtils.chunkTheList(new ArrayList(params), IN_CLAUSE_CHUNK_SIZE);
    for (List<?> joinPropertyTypeSideValues : chunkedJoinPropertyTypeSideValues) {
      MapSqlParameterSource queryParams =
          new MapSqlParameterSource("joinPropertyTypeSideValues", joinPropertyTypeSideValues);
      jtm.getNamedParameterJdbcTemplate().query(sql, queryParams, rsExtractor);
    }

    for (BeanWrapper bw : bwMergeList) {
      // find the matching related model
      Object relatedModel = idToRelatedModelMap.get(bw.getPropertyValue(joinPropertyName));
      if (relatedModel != null) {
        bw.setPropertyValue(propertyName, relatedModel);
      }
    }
    // code reaches here query success, handle caching
    if (!foundInCache) {
      jtm.getQueryMergeSqlCache().put(cacheKey, sql);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void processHasMany(JdbcTemplateMapper jtm, List<T> mergeList, Class<?> type,
      Class<?> relatedType, String cacheKey) {

    if (MapperUtils.isEmpty(mergeList)) {
      return;
    }
    TableMapping typeTableMapping = jtm.getTableMapping(type);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);
    String joinPropertyName = relatedTypeTableMapping.getPropertyName(joinColumnManySide);
    String typeIdPropName = typeTableMapping.getIdPropertyName();
    Map<Object, BeanWrapper> idToBeanWrapperTypeModelMap = new HashMap<>(mergeList.size());
    Set params = new HashSet<>(mergeList.size());
    for (Object obj : mergeList) {
      if (obj != null) {
        BeanWrapper bwTypeModel = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        Object idValue = bwTypeModel.getPropertyValue(typeIdPropName);
        if (idValue != null) {
          params.add(idValue);
          // clear collection to address edge case where collection is initialized with values
          Collection collection = (Collection) bwTypeModel.getPropertyValue(propertyName);
          if (collection.size() > 0) {
            collection.clear();
          }
          idToBeanWrapperTypeModelMap.put(idValue, bwTypeModel);
        }
      }
    }
    if (MapperUtils.isEmpty(params)) {
      return;
    }

    String relatedColumnPrefix = MapperUtils.columnPrefix(relatedTypeTableAlias,
        relatedTypeTableMapping.getTableNameForSql());

    SelectMapper<?> selectMapper = jtm.getSelectMapperInternal(relatedType, relatedColumnPrefix,
        MapperUtils.RELATED_TABLE_COL_ALIAS_PREFIX);

    boolean foundInCache = false;
    String sql = jtm.getQueryMergeSqlCache().get(cacheKey);
    if (sql == null) {
      String relatedTableStr =
          relatedTypeTableAlias == null ? relatedTypeTableMapping.fullyQualifiedTableName()
              : relatedTypeTableMapping.fullyQualifiedTableName() + " " + relatedTypeTableAlias;

      // @formatter:off
      sql = "SELECT " + selectMapper.getColumnsSql() 
          + " FROM " + relatedTableStr 
          + " WHERE " + joinColumnManySide
          + " IN (:typeIds)";
      // @formatter:on
    } else {
      foundInCache = true;
    }

    String partialSqlForCache = sql;
    if (MapperUtils.isNotBlank(orderBy)) {
      sql += " ORDER BY " + orderBy;
    }

    ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
      public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
          BeanWrapper bwRelatedModel = selectMapper.buildBeanWrapperModel(rs);
          if (bwRelatedModel != null) {
            BeanWrapper bwTypeModel =
                idToBeanWrapperTypeModelMap.get(bwRelatedModel.getPropertyValue(joinPropertyName));
            if (bwTypeModel != null) {
              // already validated so we know collection is initialized
              Collection collection = (Collection) bwTypeModel.getPropertyValue(propertyName);
              collection.add(bwRelatedModel.getWrappedInstance());
            }
          }
        }
        return null;
      }
    };

    // some databases have limits on number of entries in a 'IN' clause
    // Chunk the list and make multiple calls as needed.
    List<List<?>> chunkedTypeIds =
        MapperUtils.chunkTheList(new ArrayList(params), IN_CLAUSE_CHUNK_SIZE);
    for (List typeIds : chunkedTypeIds) {
      MapSqlParameterSource queryParams = new MapSqlParameterSource("typeIds", typeIds);
      jtm.getNamedParameterJdbcTemplate().query(sql, queryParams, rsExtractor);
    }

    // code reaches here query success, handle caching
    if (!foundInCache) {
      jtm.getQueryMergeSqlCache().put(cacheKey, partialSqlForCache);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void processHasManyThrough(JdbcTemplateMapper jtm, List<T> mergeList, Class<?> type,
      Class<?> relatedType, String cacheKey) {

    if (MapperUtils.isEmpty(mergeList)) {
      return;
    }
    TableMapping typeTableMapping = jtm.getTableMapping(type);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);

    String typeIdPropName = typeTableMapping.getIdPropertyName();

    // key - typeId, value - bean wrapped type model from mergeList
    Map<Object, BeanWrapper> idToBeanWrapperTypeModelMap = new HashMap<>(mergeList.size());
    Set params = new HashSet<>(mergeList.size());
    for (Object obj : mergeList) {
      if (obj != null) {
        BeanWrapper bwTypeModel = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        Object idValue = bwTypeModel.getPropertyValue(typeIdPropName);
        if (idValue != null) {
          params.add(idValue);
          // clear collection to address edge case where collection is initialized with values
          Collection collection = (Collection) bwTypeModel.getPropertyValue(propertyName);
          if (collection.size() > 0) {
            collection.clear();
          }
          idToBeanWrapperTypeModelMap.put(idValue, bwTypeModel);
        }
      }
    }
    if (MapperUtils.isEmpty(params)) {
      return;
    }

    // The select statement is build in such a way the buildBeanWrapperModel(rs) returns the
    // type id value. Note: For QueryMerge there is no alias for type table
    SelectMapper<?> selectMapperType = jtm.getSelectMapperInternal(type,
        typeTableMapping.getTableName(), MapperUtils.TYPE_TABLE_COL_ALIAS_PREFIX);

    String relatedColumnPrefix =
        relatedTypeTableAlias == null ? relatedTypeTableMapping.getTableNameForSql()
            : relatedTypeTableAlias;

    SelectMapper<?> selectMapperRelatedType = jtm.getSelectMapperInternal(relatedType,
        relatedColumnPrefix, MapperUtils.RELATED_TABLE_COL_ALIAS_PREFIX);

    boolean foundInCache = false;
    String sql = jtm.getQueryMergeSqlCache().get(cacheKey);
    if (sql == null) {
      String relatedTableStr =
          relatedTypeTableAlias == null ? relatedTypeTableMapping.fullyQualifiedTableName()
              : relatedTypeTableMapping.fullyQualifiedTableName() + " " + relatedTypeTableAlias;

      String onRelatedPrefix =
          relatedTypeTableAlias == null ? relatedTypeTableMapping.getTableNameForSql()
              : relatedTypeTableAlias;

      sql = "SELECT " + MapperUtils.getTableNameOnly(throughJoinTable) + "."
          + typeTableMapping.getIdentifierForSql(throughTypeJoinColumn) + " as "
          + selectMapperType.getResultSetModelIdColumnLabel() + ", "
          + selectMapperRelatedType.getColumnsSql() + " FROM "
          + MapperUtils.getFullyQualifiedTableNameForThroughJoinTable(throughJoinTable,
              typeTableMapping)
          + " LEFT JOIN " + relatedTableStr + " on "
          + MapperUtils.getTableNameOnly(throughJoinTable) + "."
          + relatedTypeTableMapping.getIdentifierForSql(throughRelatedTypeJoinColumn) + " = "
          + onRelatedPrefix + "." + relatedTypeTableMapping.getIdColumnNameForSql() + " WHERE "
          + MapperUtils.getTableNameOnly(throughJoinTable) + "."
          + typeTableMapping.getIdentifierForSql(throughTypeJoinColumn) + " IN (:typeIds)";
    } else {
      foundInCache = true;
    }

    String partialSqlForCache = sql;
    if (MapperUtils.isNotBlank(orderBy)) {
      sql += " ORDER BY " + orderBy;
    }

    ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
      public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        while (rs.next()) {
          BeanWrapper bwResultSetType = selectMapperType.buildBeanWrapperModel(rs);
          if (bwResultSetType != null) {
            BeanWrapper bwRelatedModel = selectMapperRelatedType.buildBeanWrapperModel(rs);
            if (bwRelatedModel != null) {
              Object typeIdValue = bwResultSetType.getPropertyValue(typeIdPropName);
              BeanWrapper bw = idToBeanWrapperTypeModelMap.get(typeIdValue);
              if (bw != null) {
                Collection collection = (Collection) bw.getPropertyValue(propertyName);
                collection.add(bwRelatedModel.getWrappedInstance());
              }
            }
          }
        }
        return null;
      }
    };

    // some databases have limits on number of entries in a 'IN' clause
    // Chunk the list and make multiple calls as needed.
    Collection<List<?>> chunkedTypeIds =
        MapperUtils.chunkTheList(new ArrayList(params), IN_CLAUSE_CHUNK_SIZE);
    for (List typeIds : chunkedTypeIds) {
      MapSqlParameterSource queryParams = new MapSqlParameterSource("typeIds", typeIds);
      jtm.getNamedParameterJdbcTemplate().query(sql, queryParams, rsExtractor);
    }

    // code reaches here query success, handle caching
    if (!foundInCache) {
      jtm.getQueryMergeSqlCache().put(cacheKey, partialSqlForCache);
    }
  }

  private String getCacheKey() {
    // @formatter:off
    return String.join("-", 
        type.getName(),
        relatedType == null ? null : relatedType.getName(),
        relatedTypeTableAlias,
        relationshipType,
        joinColumnTypeSide,
        joinColumnManySide,
        throughJoinTable, 
        throughTypeJoinColumn,
        throughRelatedTypeJoinColumn,
        propertyName);
    // @formatter:on
  }

}
