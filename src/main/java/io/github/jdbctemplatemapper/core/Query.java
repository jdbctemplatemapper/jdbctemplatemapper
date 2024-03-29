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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.query.IQueryFluent;
import io.github.jdbctemplatemapper.query.IQueryHasMany;
import io.github.jdbctemplatemapper.query.IQueryHasOne;
import io.github.jdbctemplatemapper.query.IQueryJoinColumnManySide;
import io.github.jdbctemplatemapper.query.IQueryJoinColumnTypeSide;
import io.github.jdbctemplatemapper.query.IQueryLimitOffsetClause;
import io.github.jdbctemplatemapper.query.IQueryOrderBy;
import io.github.jdbctemplatemapper.query.IQueryPopulateProperty;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinColumns;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinTable;
import io.github.jdbctemplatemapper.query.IQueryType;
import io.github.jdbctemplatemapper.query.IQueryWhere;

/**
 * Fluent style queries for relationships hasOne, hasMany, hasMany through (many to many).
 *
 * <pre>
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#querying-relationships">Querying
 * relationships</a> for more info
 * </pre>
 *
 * @author ajoseph
 */
public class Query<T> implements IQueryFluent<T> {
  private Class<T> type;
  private String typeTableAlias;
  private String whereClause;
  private Object[] whereParams;
  private String orderBy;
  private String limitOffsetClause;

  private String relationshipType;
  private Class<?> relatedType;
  private String relatedTableAlias;
  private String propertyName; // propertyName on main class that needs to be populated
  private String joinColumnTypeSide;
  private String joinColumnManySide;

  private String throughJoinTable;
  private String throughTypeJoinColumn;
  private String throughRelatedTypeJoinColumn;

  private Query(Class<T> type) {
    this.type = type;
  }

  private Query(Class<T> type, String tableAlias) {
    this.type = type;
    this.typeTableAlias = tableAlias;
  }

  /**
   * The type being queried. The execute() method will return a list of this type.
   *
   * @param <T> The type
   * @param type the type
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryType<T> type(Class<T> type) {
    Assert.notNull(type, "type cannot be null");
    return new Query<T>(type);
  }

  /**
   * The type being queried. The execute() method will return a list of this type.
   *
   * @param <T> The type
   * @param type the type
   * @param tableAlias the table alias. The alias can be used in the where and orderBy clauses
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryType<T> type(Class<T> type, String tableAlias) {
    Assert.notNull(type, "type cannot be null");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new IllegalArgumentException("tableAlias for type cannot be null or blank");
    }
    return new Query<T>(type, tableAlias);
  }

  /**
   * The hasOne relationship.
   *
   * @param relatedType the related type
   * @return interface with the next methods in the chain
   */
  public IQueryHasOne<T> hasOne(Class<?> relatedType) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    this.relationshipType = RelationshipType.HAS_ONE;
    this.relatedType = relatedType;
    return this;
  }

  /**
   * The hasOne relationship.
   *
   * @param relatedType the related type
   * @param tableAlias the table alias which can be used in where and orderBy clauses
   * @return interface with the next methods in the chain
   */
  public IQueryHasOne<T> hasOne(Class<?> relatedType, String tableAlias) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new IllegalArgumentException("tableAlias for type cannot be null or blank");
    }
    this.relationshipType = RelationshipType.HAS_ONE;
    this.relatedType = relatedType;
    this.relatedTableAlias = tableAlias;
    return this;
  }

  /**
   * The hasMany relationship. The 'populateProperty' for hasMany relationship should be a
   * collection and has to be initialized.
   *
   * @param relatedType the related type
   * @return interface with the next methods in the chain
   */
  public IQueryHasMany<T> hasMany(Class<?> relatedType) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    this.relationshipType = RelationshipType.HAS_MANY;
    this.relatedType = relatedType;
    return this;
  }

  /**
   * The hasMany relationship. The 'populateProperty' for hasMany relationship should be a
   * collection and has to be initialized.
   *
   * @param relatedType the related type
   * @param tableAlias the table alias which can be used in where and orderBy clauses
   * @return interface with the next methods in the chain
   */
  public IQueryHasMany<T> hasMany(Class<?> relatedType, String tableAlias) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new IllegalArgumentException("tableAlias for type cannot be null or blank");
    }
    this.relationshipType = RelationshipType.HAS_MANY;
    this.relatedType = relatedType;
    this.relatedTableAlias = tableAlias;
    return this;
  }

  /**
   * Join column for hasOne relationship: The join column (the foreign key) is on the table of the
   * type model. Example: Order hasOne Customer. The join column(foreign key) will be on the table
   * order (of the type model). The join column should not have a table prefix.
   *
   * @param joinColumnTypeSide the join column on the type side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryJoinColumnTypeSide<T> joinColumnTypeSide(String joinColumnTypeSide) {
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
  public IQueryJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide) {
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
  public IQueryThroughJoinTable<T> throughJoinTable(String tableName) {
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
  public IQueryThroughJoinColumns<T> throughJoinColumns(String typeJoinColumn,
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
   * The relationship property that needs to be populated on the type type. For hasMany() this
   * property has to be an initialized collection (cannot be null) and the generic type should match
   * the hasMany related type. For hasOne this property has to be the same type as hasOne related
   * type
   *
   * @param propertyName name of property that needs to be populated
   * @return interface with the next methods in the chain
   */
  public IQueryPopulateProperty<T> populateProperty(String propertyName) {
    if (MapperUtils.isBlank(propertyName)) {
      throw new IllegalArgumentException("propertyName cannot be null or blank");
    }
    this.propertyName = propertyName;
    return this;
  }

  /**
   * The SQL where clause. When querying relationships the where clause can include columns from
   * both type and related tables. The parameters can be positional or named parameters.
   * 
   * <pre>
   * Examples:
   * 1) where("orders.status = ? and orders.customer_id = ?", "COMPLETE", 1)
   * 2) where("orders.status = :status and orders.customer_id = :customerId", 
   *       new MapSqlParameterSource().addValue("status", "COMPLETE").addValue("customerId", 1))
   * </pre>
   * 
   * @param whereClause the whereClause.
   * @param params varArgs for the whereClause.
   * @return interface with the next methods in the chain
   */
  public IQueryWhere<T> where(String whereClause, Object... params) {
    if (MapperUtils.isBlank(whereClause)) {
      throw new IllegalArgumentException("whereClause cannot be null or blank");
    }
    this.whereClause = whereClause;
    this.whereParams = params;
    return this;
  }

  /**
   * The SQL orderBy clause for the query. When querying relationships (hasOne, hasMany, hasMany
   * through) the orderBy can include columns from both type and related tables.
   *
   * @param orderBy the orderBy clause.
   * @return interface with the next methods in the chain
   */
  public IQueryOrderBy<T> orderBy(String orderBy) {
    if (MapperUtils.isBlank(orderBy)) {
      throw new IllegalArgumentException("orderBy cannot be null or blank");
    }
    this.orderBy = orderBy;
    return this;
  }

  /**
   * The SQL limit Offset clause for the query specific to the database being used. The limit offset
   * clause is not supported for hasMany/hasMany through relationships.
   * 
   * <pre>
   * See <a href=
   * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#paginated-queries">Paginated Queries
   * </a> for more info
   * </pre>
   *
   * @param limitOffsetClause the limit clause.
   * @return interface with the next methods in the chain
   */
  public IQueryLimitOffsetClause<T> limitOffsetClause(String limitOffsetClause) {
    if (MapperUtils.isBlank(limitOffsetClause)) {
      throw new IllegalArgumentException("limitOffsetClause cannot be null or blank");
    }
    this.limitOffsetClause = limitOffsetClause;
    return this;
  }

  /**
   * Execute the query using the jdbcTemplateMapper.
   *
   * @param jdbcTemplateMapper the jdbcTemplateMapper
   * @return List a list of type. If no records found returns empty list.
   */
  public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
    Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

    TableMapping typeTableMapping = jdbcTemplateMapper.getTableMapping(type);
    String typeColumnPrefix =
        MapperUtils.columnPrefix(typeTableAlias, typeTableMapping.getTableName());

    SelectMapper<?> typeSelectMapper = jdbcTemplateMapper.getSelectMapperInternal(type,
        typeColumnPrefix, MapperUtils.TYPE_TABLE_COL_ALIAS_PREFIX);

    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : jdbcTemplateMapper.getTableMapping(relatedType);

    // making it effectively final to be used in inner class ResultSetExtractor
    SelectMapper<?> relatedTypeSelectMapper = relatedType == null ? null
        : jdbcTemplateMapper.getSelectMapperInternal(relatedType,
            MapperUtils.columnPrefix(relatedTableAlias, relatedTypeTableMapping.getTableName()),
            MapperUtils.RELATED_TABLE_COL_ALIAS_PREFIX);

    boolean foundInCache = false;
    String cacheKey = getCacheKey();
    String sql = jdbcTemplateMapper.getQuerySqlCache().get(cacheKey);
    if (sql == null) {
      QueryValidator.validate(jdbcTemplateMapper, type, relationshipType, relatedType,
          joinColumnTypeSide, joinColumnManySide, propertyName, throughJoinTable,
          throughTypeJoinColumn, throughRelatedTypeJoinColumn);

      // does not include where,orderBy,offsetLimit
      sql = generatePartialQuerySql(jdbcTemplateMapper);
    } else {
      foundInCache = true;
    }

    // sql stored in cache does not include where,orderBy,offsetLimit
    String partialSqlForCache = sql;

    if (MapperUtils.isNotBlank(whereClause)) {
      sql += " WHERE " + whereClause;
    }

    if (MapperUtils.isNotBlank(orderBy)) {
      sql += " ORDER BY " + orderBy;
    }

    if (MapperUtils.isNotBlank(limitOffsetClause)) {
      QueryValidator.validateQueryLimitOffsetClause(relationshipType, limitOffsetClause);
      sql += " " + limitOffsetClause;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
      public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        // LinkedHashMap to retain record order
        Map<Object, BeanWrapper> idToBeanWrapperTypeModelMap = new LinkedHashMap<>();
        Map<Object, BeanWrapper> idToBeanWrapperRelatedModelMap = new HashMap<>();
        while (rs.next()) {
          BeanWrapper bwTypeModel =
              getBeanWrapperModel(rs, typeSelectMapper, idToBeanWrapperTypeModelMap, true);
          if (relatedType != null && bwTypeModel != null) {
            BeanWrapper bwRelatedModel = getBeanWrapperModel(rs, relatedTypeSelectMapper,
                idToBeanWrapperRelatedModelMap, false);
            Object relatedModel =
                bwRelatedModel == null ? null : bwRelatedModel.getWrappedInstance();
            if (RelationshipType.HAS_ONE.equals(relationshipType)) {
              bwTypeModel.setPropertyValue(propertyName, relatedModel);
            } else if (RelationshipType.HAS_MANY.equals(relationshipType)
                || RelationshipType.HAS_MANY_THROUGH.equals(relationshipType)) {
              if (relatedModel != null) {
                // the property has already been validated so we know it is a
                // collection that has been initialized
                Collection collection = (Collection) bwTypeModel.getPropertyValue(propertyName);
                collection.add(relatedModel);
              }
            }
          }
        }
        return idToBeanWrapperTypeModelMap.values()
                                          .stream()
                                          .map(bw -> (T) bw.getWrappedInstance())
                                          .collect(Collectors.toList());
      }
    };

    List<T> resultList = null;
    if (whereParams == null) {
      resultList = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor);
    } else {
      if (whereParams[0] instanceof MapSqlParameterSource) {
        resultList =
            jdbcTemplateMapper.getNamedParameterJdbcTemplate()
                              .query(sql, (MapSqlParameterSource) whereParams[0], rsExtractor);
      } else {
        resultList = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor, whereParams);
      }
    }

    // code reaches here query success, handle caching
    if (!foundInCache) {
      jdbcTemplateMapper.getQuerySqlCache().put(cacheKey, partialSqlForCache);
    }
    return resultList;
  }

  @SuppressWarnings("rawtypes")
  private BeanWrapper getBeanWrapperModel(ResultSet rs, SelectMapper<?> selectMapper,
      Map<Object, BeanWrapper> idToBeanWrapperModelMap, boolean isTypeModel) throws SQLException {
    BeanWrapper bwModel = null;
    Object id = rs.getObject(selectMapper.getResultSetModelIdColumnLabel());
    id = rs.wasNull() ? null : id; // some drivers are goofy
    if (id != null) {
      bwModel = idToBeanWrapperModelMap.get(id);
      if (bwModel == null) {
        bwModel = selectMapper.buildBeanWrapperModel(rs); // builds the model from resultSet
        if (isTypeModel && (RelationshipType.HAS_MANY.equals(relationshipType)
            || RelationshipType.HAS_MANY_THROUGH.equals(relationshipType))) {
          // first time seeing the type model. Make sure collection is clear.
          Collection collection = (Collection) bwModel.getPropertyValue(propertyName);
          if (collection.size() > 0) {
            collection.clear();
          }
        }
        idToBeanWrapperModelMap.put(id, bwModel);
      }
    }
    return bwModel;
  }

  // The sql generated does not include where, orderBy, offsetLimit
  private String generatePartialQuerySql(JdbcTemplateMapper jtm) {

    TableMapping typeTableMapping = jtm.getTableMapping(type);

    String typeColumnPrefix =
        MapperUtils.columnPrefix(typeTableAlias, typeTableMapping.getTableName());

    SelectMapper<?> typeSelectMapper = jtm.getSelectMapperInternal(type, typeColumnPrefix,
        MapperUtils.TYPE_TABLE_COL_ALIAS_PREFIX);

    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : jtm.getTableMapping(relatedType);

    String relatedColumnPrefix = null;
    if (relatedTypeTableMapping != null) {
      relatedColumnPrefix =
          MapperUtils.columnPrefix(relatedTableAlias, relatedTypeTableMapping.getTableName());
    }

    SelectMapper<?> relatedTypeSelectMapper = relatedType == null ? null
        : jtm.getSelectMapperInternal(relatedType, relatedColumnPrefix,
            MapperUtils.RELATED_TABLE_COL_ALIAS_PREFIX);

    String sql = "SELECT " + typeSelectMapper.getColumnsSql();
    if (relatedType != null) {
      sql += "," + relatedTypeSelectMapper.getColumnsSql();
    }

    if (RelationshipType.HAS_ONE.equals(relationshipType)) {
      sql += hasOneFromClause(typeTableMapping, relatedTypeTableMapping);
    } else if (RelationshipType.HAS_MANY.equals(relationshipType)) {
      // joinColumn is on related table
      sql += hasManyFromClause(typeTableMapping, relatedTypeTableMapping);
    } else if (RelationshipType.HAS_MANY_THROUGH.equals(relationshipType)) {
      sql += hasManyThroughFromClause(typeTableMapping, relatedTypeTableMapping);
    } else {
      String typeTableStr = typeTableAlias == null ? typeTableMapping.fullyQualifiedTableName()
          : typeTableMapping.fullyQualifiedTableName() + " " + typeTableAlias;

      sql += " FROM " + typeTableStr;
    }

    return sql;
  }

  String hasOneFromClause(TableMapping typeTableMapping, TableMapping relatedTableMapping) {
    // joinColumn is on type table
    String typeTableStr =
        MapperUtils.tableStrForFrom(typeTableAlias, typeTableMapping.fullyQualifiedTableName());

    String str = " FROM " + typeTableStr;

    if (relatedType != null) {
      String relatedTableStr = MapperUtils.tableStrForFrom(relatedTableAlias,
          relatedTableMapping.fullyQualifiedTableName());

      String onTypePrefix =
          MapperUtils.columnPrefix(typeTableAlias, typeTableMapping.getTableName());

      String onRelatedPrefix =
          MapperUtils.columnPrefix(relatedTableAlias, relatedTableMapping.getTableName());

      str += " LEFT JOIN " + relatedTableStr + " on " + onTypePrefix + "." + joinColumnTypeSide
          + " = " + onRelatedPrefix + "." + relatedTableMapping.getIdColumnName();
    }
    return str;
  }

  String hasManyFromClause(TableMapping typeTableMapping, TableMapping relatedTableMapping) {
    String typeTableStr =
        MapperUtils.tableStrForFrom(typeTableAlias, typeTableMapping.fullyQualifiedTableName());

    String str = " FROM " + typeTableStr;
    if (relatedType != null) {
      String relatedTableStr = MapperUtils.tableStrForFrom(relatedTableAlias,
          relatedTableMapping.fullyQualifiedTableName());

      String onTypePrefix =
          MapperUtils.columnPrefix(typeTableAlias, typeTableMapping.getTableName());

      String onRelatedPrefix =
          MapperUtils.columnPrefix(relatedTableAlias, relatedTableMapping.getTableName());

      str += " LEFT JOIN " + relatedTableStr + " on " + onTypePrefix + "."
          + typeTableMapping.getIdColumnName() + " = " + onRelatedPrefix + "." + joinColumnManySide;
    }

    return str;
  }

  String hasManyThroughFromClause(TableMapping typeTableMapping, TableMapping relatedTableMapping) {

    String typeTableStr =
        MapperUtils.tableStrForFrom(typeTableAlias, typeTableMapping.fullyQualifiedTableName());

    String str = " FROM " + typeTableStr;

    if (relatedType != null) {
      String relatedTableStr = MapperUtils.tableStrForFrom(relatedTableAlias,
          relatedTableMapping.fullyQualifiedTableName());

      String onTypePrefix =
          MapperUtils.columnPrefix(typeTableAlias, typeTableMapping.getTableName());

      String onRelatedPrefix =
          MapperUtils.columnPrefix(relatedTableAlias, relatedTableMapping.getTableName());

      str += " LEFT JOIN "
          + MapperUtils.getFullyQualifiedTableNameForThroughJoinTable(throughJoinTable,
              typeTableMapping)
          + " on " + onTypePrefix + "." + typeTableMapping.getIdColumnName() + " = "
          + MapperUtils.getTableNameOnly(throughJoinTable) + "." + throughTypeJoinColumn
          + " LEFT JOIN " + relatedTableStr + " on "
          + MapperUtils.getTableNameOnly(throughJoinTable) + "." + throughRelatedTypeJoinColumn
          + " = " + onRelatedPrefix + "." + relatedTableMapping.getIdColumnName();
    }
    return str;
  }

  String getCacheKey() {
    // @formatter:off
    return String.join("-", 
        type.getName(),
        typeTableAlias,
        relatedType == null ? null : relatedType.getName(),
        relatedTableAlias,
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
