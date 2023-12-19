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

import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.querycount.IQueryCountFluent;
import io.github.jdbctemplatemapper.querycount.IQueryCountHasOne;
import io.github.jdbctemplatemapper.querycount.IQueryCountJoinColumnTypeSide;
import io.github.jdbctemplatemapper.querycount.IQueryCountType;
import io.github.jdbctemplatemapper.querycount.IQueryCountWhere;

/**
 * Gets the count of the records for a query. This is to support pagination counts.
 *
 * <pre>
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#paginated-queries">Paginated Queries
 * </a>
 * </pre>
 *
 * @author ajoseph
 */
public class QueryCount<T> implements IQueryCountFluent<T> {
  private Class<T> type;
  private String typeTableAlias;
  private String whereClause;
  private Object[] whereParams;

  private String relationshipType;
  private Class<?> relatedType;
  private String relatedTableAlias;
  private String joinColumnTypeSide;

  private QueryCount(Class<T> type) {
    this.type = type;
  }

  private QueryCount(Class<T> type, String tableAlias) {
    this.type = type;
    this.typeTableAlias = tableAlias;
  }

  /**
   * The type that needs to be counted.
   *
   * @param <T> The type
   * @param type the type
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryCountType<T> type(Class<T> type) {
    Assert.notNull(type, "type cannot be null");
    return new QueryCount<T>(type);
  }

  /**
   * The type that needs to be counted.
   *
   * @param <T> The type
   * @param type the type
   * @param tableAlias the table alias which can be used in where clause
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryCountType<T> type(Class<T> type, String tableAlias) {
    Assert.notNull(type, "type cannot be null");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new IllegalArgumentException("tableAlias for type cannot be null or blank");
    }
    return new QueryCount<T>(type, tableAlias);
  }

  /**
   * The hasOne relationship.
   *
   * @param relatedType the related type
   * @return interface with the next methods in the chain
   */
  public IQueryCountHasOne<T> hasOne(Class<?> relatedType) {
    Assert.notNull(relatedType, "relatedType cannot be null");
    this.relationshipType = RelationshipType.HAS_ONE;
    this.relatedType = relatedType;
    return this;
  }

  /**
   * The hasOne relationship.
   *
   * @param relatedType the related type
   * @param tableAlias the table alias which can be used in where clause
   * @return interface with the next methods in the chain
   */
  public IQueryCountHasOne<T> hasOne(Class<?> relatedType, String tableAlias) {
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
   * Join column for hasOne relationship: The join column (the foreign key) is on the table of the
   * type model. Example: Order hasOne Customer. The join column(foreign key) will be on the table
   * order (of the type model). The join column should not have a table prefix.
   *
   * @param joinColumnTypeSide the join column on the type side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryCountJoinColumnTypeSide<T> joinColumnTypeSide(String joinColumnTypeSide) {
    if (MapperUtils.isBlank(joinColumnTypeSide)) {
      throw new IllegalArgumentException("joinColumnTypeSide cannot be null or blank");
    }
    this.joinColumnTypeSide = MapperUtils.toLowerCase(joinColumnTypeSide.trim());
    return this;
  }

  /**
   * The where clause.
   *
   * @param whereClause the whereClause for the type.
   * @param params varArgs for the whereClause.
   * @return interface with the next methods in the chain
   */
  public IQueryCountWhere<T> where(String whereClause, Object... params) {
    if (MapperUtils.isBlank(whereClause)) {
      throw new IllegalArgumentException("whereClause cannot be null or blank");
    }
    this.whereClause = whereClause;
    this.whereParams = params;
    return this;
  }

  /**
   * The executes query and returns count.
   *
   * @param jdbcTemplateMapper the jdbcTemplateMapper
   */
  public Integer execute(JdbcTemplateMapper jdbcTemplateMapper) {
    Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

    boolean foundInCache = false;
    String cacheKey = getCacheKey();
    String sql = jdbcTemplateMapper.getQueryCountSqlCache().get(cacheKey);
    if (sql == null) {
      QueryValidator.validateQueryCount(jdbcTemplateMapper, type, relationshipType, relatedType,
          joinColumnTypeSide);
      sql = generatePartialQuerySql(jdbcTemplateMapper);
    } else {
      foundInCache = true;
    }

    String partialSqlForCache = sql;
    if (MapperUtils.isNotBlank(whereClause)) {
      sql += " WHERE " + whereClause;
    }

    Integer count = 0;
    if (whereParams == null) {
      count = jdbcTemplateMapper.getJdbcTemplate().queryForObject(sql, Integer.class);
    } else {
      count = jdbcTemplateMapper.getJdbcTemplate().queryForObject(sql, Integer.class, whereParams);
    }

    // code reaches here query success, handle caching
    if (!foundInCache) {
      jdbcTemplateMapper.getQueryCountSqlCache().put(cacheKey, partialSqlForCache);
    }
    return count;
  }

  private String generatePartialQuerySql(JdbcTemplateMapper jtm) {
    TableMapping typeTableMapping = jtm.getTableMapping(type);
    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : jtm.getTableMapping(relatedType);

    String typeTableStr =
        MapperUtils.tableStrForFrom(typeTableAlias, typeTableMapping.fullyQualifiedTableName());

    String typeColumnPrefix =
        MapperUtils.columnPrefix(typeTableAlias, typeTableMapping.getTableName());

    String sql = "SELECT count(*) as record_count ";
    sql += " FROM " + typeTableStr;
    if (relatedType != null) {
      String relatedTableStr = MapperUtils.tableStrForFrom(relatedTableAlias,
          relatedTypeTableMapping.fullyQualifiedTableName());

      String relatedColumnPrefix =
          MapperUtils.columnPrefix(relatedTableAlias, relatedTypeTableMapping.getTableName());

      if (RelationshipType.HAS_ONE.equals(relationshipType)) {
        // joinColumn is on type table
        sql +=
            " LEFT JOIN " + relatedTableStr + " on " + typeColumnPrefix + "." + joinColumnTypeSide
                + " = " + relatedColumnPrefix + "." + relatedTypeTableMapping.getIdColumnName();
      }
    }
    return sql;
  }

  private String getCacheKey() {
    // @formatter:off
    return String.join("-", 
        type.getName(), 
        typeTableAlias,
        relatedType == null ? null : relatedType.getName(),
        relatedTableAlias,
        relationshipType,
        joinColumnTypeSide);
    // @formatter:on
  }

}
