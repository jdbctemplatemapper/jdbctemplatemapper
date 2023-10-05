package io.github.jdbctemplatemapper.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.querycount.IQueryCountFluent;
import io.github.jdbctemplatemapper.querycount.IQueryCountHasOne;
import io.github.jdbctemplatemapper.querycount.IQueryCountJoinColumnOwningSide;
import io.github.jdbctemplatemapper.querycount.IQueryCountType;
import io.github.jdbctemplatemapper.querycount.IQueryCountWhere;

/**
 * Gets the count of the records for a query. This is to mainly support pagination counts
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

  private static final int CACHE_MAX_ENTRIES = 1000;
  // key - cacheKey, value - sql
  private static Map<String, String> successCountSqlCache = new ConcurrentHashMap<>();

  private Class<T> ownerType;
  private String whereClause;
  private Object[] whereParams;

  private RelationshipType relationshipType;
  private Class<?> relatedType;
  private String joinColumnOwningSide;

  private QueryCount(Class<T> type) {
    this.ownerType = type;
  }

  /**
   * The owning type
   *
   * @param <T> The type
   * @param type the type
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryCountType<T> type(Class<T> type) {
    Assert.notNull(type, "Type cannot be null");
    return new QueryCount<T>(type);
  }

  /**
   * The hasOne relationship
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
   * Join column for hasOne relationship: The join column (the foreign key) is on the table of the
   * owning model. Example: Order hasOne Customer. The join column(foreign key) will be on the table
   * order (of the owning model)
   *
   * <p>
   * The join column should not have a table prefix.
   *
   * @param joinColumnOwningSide the join column on the owning side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryCountJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide) {
    if (MapperUtils.isBlank(joinColumnOwningSide)) {
      throw new IllegalArgumentException("joinColumnOwningSide cannot be null or blank");
    }
    this.joinColumnOwningSide = MapperUtils.toLowerCase(joinColumnOwningSide.trim());
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
   * The executes query and returns count
   *
   * @param jdbcTemplateMapper the jdbcTemplateMapper
   */
  public Integer execute(JdbcTemplateMapper jdbcTemplateMapper) {
    Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

    boolean foundInCache = false;
    String cacheKey = getCacheKey(jdbcTemplateMapper);
    String sql = getSqlFromCache(cacheKey);
    if (sql == null) {
      QueryValidator.validateQueryCount(jdbcTemplateMapper, ownerType, relationshipType,
          relatedType, joinColumnOwningSide);
      sql = generateQuerySql(jdbcTemplateMapper);
    }
    else {
      foundInCache = true;
    }

    Integer count = 0;
    if (whereParams == null) {
      count = jdbcTemplateMapper.getJdbcTemplate().queryForObject(sql, Integer.class);
    } else {
      count = jdbcTemplateMapper.getJdbcTemplate().queryForObject(sql, Integer.class, whereParams);
    }

    // code reaches here query success, handle caching
    if (!foundInCache) {
      addToCache(cacheKey, sql);
    }
    return count;
  }

  private String generateQuerySql(JdbcTemplateMapper jtm) {
    TableMapping ownerTypeTableMapping = jtm.getTableMapping(ownerType);
    String ownerTypeTableName = ownerTypeTableMapping.getTableName();
    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : jtm.getTableMapping(relatedType);

    String sql = "SELECT count(*) as record_count ";
    sql += " FROM " + ownerTypeTableMapping.fullyQualifiedTableName();
    if (relatedType != null) {
      String relatedTypeTableName = relatedTypeTableMapping.getTableName();
      if (relationshipType == RelationshipType.HAS_ONE) {
        // joinColumn is on owner table
        sql += " LEFT JOIN " + relatedTypeTableMapping.fullyQualifiedTableName()
            + " on " + ownerTypeTableName + "." + joinColumnOwningSide + " = "
            + relatedTypeTableName + "." + relatedTypeTableMapping.getIdColumnName();
      }
    }
    if (MapperUtils.isNotBlank(whereClause)) {
      sql += " WHERE " + whereClause;
    }

    return sql;
  }

  private String getCacheKey(JdbcTemplateMapper jdbcTemplateMapper) {
    // @formatter:off
    return String.join("-", 
        ownerType.getName(), 
        relatedType == null ? null : relatedType.getName(),
        relationshipType == null ? null : relationshipType.toString(),
        joinColumnOwningSide, 
        whereClause,
        jdbcTemplateMapper.toString());
    // @formatter:on
  }

  private String getSqlFromCache(String cacheKey) {
    return successCountSqlCache.get(cacheKey);
  }

  private void addToCache(String key, String sql) {
    if (successCountSqlCache.size() < CACHE_MAX_ENTRIES) {
      successCountSqlCache.put(key, sql);
    } else {
      // remove a random entry from cache and add new entry
      String k = successCountSqlCache.keySet().iterator().next();
      successCountSqlCache.remove(k);

      successCountSqlCache.put(key, sql);
    }
  }

}
