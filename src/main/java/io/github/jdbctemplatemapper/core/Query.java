package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.Assert;
import io.github.jdbctemplatemapper.query.IQueryFluent;
import io.github.jdbctemplatemapper.query.IQueryHasMany;
import io.github.jdbctemplatemapper.query.IQueryHasOne;
import io.github.jdbctemplatemapper.query.IQueryJoinColumnManySide;
import io.github.jdbctemplatemapper.query.IQueryJoinColumnOwningSide;
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
  private static final int CACHE_MAX_ENTRIES = 1000;

  // Cached SQL does not include limit clause
  // key - cacheKey, value - sql
  private static Map<String, String> successQuerySqlCache = new ConcurrentHashMap<>();

  private Class<T> ownerType;
  private String whereClause;
  private Object[] whereParams;
  private String orderBy;
  private String limitOffsetClause;

  private RelationshipType relationshipType;
  private Class<?> relatedType;
  private String propertyName; // propertyName on main class that needs to be populated
  private String joinColumnOwningSide;
  private String joinColumnManySide;

  private String throughJoinTable;
  private String throughOwnerTypeJoinColumn;
  private String throughRelatedTypeJoinColumn;

  private Query(Class<T> type) {
    this.ownerType = type;
  }

  /**
   * The type being queried. The execute() method will return a list of this type.
   *
   * @param <T> The type
   * @param type the type
   * @return interface with the next methods in the chain
   */
  public static <T> IQueryType<T> type(Class<T> type) {
    Assert.notNull(type, "Type cannot be null");
    return new Query<T>(type);
  }

  /**
   * The hasOne relationship
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
  public IQueryJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide) {
    if (MapperUtils.isBlank(joinColumnOwningSide)) {
      throw new IllegalArgumentException("joinColumnOwningSide cannot be null or blank");
    }
    this.joinColumnOwningSide = MapperUtils.toLowerCase(joinColumnOwningSide.trim());
    return this;
  }

  /**
   * Join column for hasMany relationship: The join column (the foreign key) is on the table of the
   * many side. Example: Order hasMany OrderLine. The join column will be on the table order_line
   * (the many side)
   *
   * <p>
   * The join column should not have a table prefix.
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
   * The join table (associative table) for the hasMany through (many to many) relationship
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
   * The join columns for the owning side and the related side for hasMany through relationship
   *
   * @param ownerTypeJoinColumn the join column for the owning side
   * @param relatedTypeJoinColumn the join column for the related side
   * @return interface with the next methods in the chain
   */
  public IQueryThroughJoinColumns<T> throughJoinColumns(String ownerTypeJoinColumn,
      String relatedTypeJoinColumn) {
    if (MapperUtils.isBlank(ownerTypeJoinColumn)) {
      throw new IllegalArgumentException(
          "throughJoinColumns() ownerTypeJoinColumn cannot be null or blank");
    }
    if (MapperUtils.isBlank(relatedTypeJoinColumn)) {
      throw new IllegalArgumentException(
          "throughJoinColumns() relatedTypeJoinColumn cannot be null or blank");
    }
    this.throughOwnerTypeJoinColumn = ownerTypeJoinColumn;
    this.throughRelatedTypeJoinColumn = relatedTypeJoinColumn;
    return this;
  }

  /**
   * The relationship property that needs to be populated on the owning type.
   *
   * <p>
   * For hasMany() this property has to be an initialized collection (cannot be null) and the
   * generic type should match the hasMany related type. For hasOne this property has to be the same
   * type as hasOne related type
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
   * The where clause for the type.
   *
   * @param whereClause the whereClause for the type.
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
   * The orderBy clause for the query. When querying relationships (hasOne, hasMany, hasMany
   * through) both the owning side and related side columns can be used in the orderBy clause.
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
   * The limit Offset clause for the query specific to the database being used.
   * 
   * The limit offset clause for hasMany/hasMany through relationship is not supported.
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
   * Execute the query using the jdbcTemplateMapper
   *
   * @param jdbcTemplateMapper the jdbcTemplateMapper
   * @return List a list of the owning type with its relationship property populated
   */
  public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
    Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

    TableMapping ownerTypeTableMapping = jdbcTemplateMapper.getTableMapping(ownerType);
    SelectMapper<?> ownerTypeSelectMapper =
        jdbcTemplateMapper.getSelectMapper(ownerType, ownerTypeTableMapping.getTableName());

    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : jdbcTemplateMapper.getTableMapping(relatedType);
    // making it effectively final to be used in inner class ResultSetExtractor
    SelectMapper<?> relatedTypeSelectMapper = relatedType == null ? null
        : jdbcTemplateMapper.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());

    String cacheKey = getCacheKey(jdbcTemplateMapper);
    String sql = getSqlFromCache(cacheKey);
    if (sql == null) {
      QueryValidator.validate(jdbcTemplateMapper, ownerType, relationshipType, relatedType,
          joinColumnOwningSide, joinColumnManySide, propertyName, throughJoinTable,
          throughOwnerTypeJoinColumn, throughRelatedTypeJoinColumn);

      sql = generateQuerySql(jdbcTemplateMapper);
    }

    // sql stored in cache does not include limit offset clause.
    String sqlForCache = sql;

    if (MapperUtils.isNotBlank(limitOffsetClause)) {
      QueryValidator.validateQueryLimitOffsetClause(relationshipType, limitOffsetClause);
      sql += " " + limitOffsetClause;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
      public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        // LinkedHashMap to retain record order
        Map<Object, BeanWrapper> idToBeanWrapperOwnerModelMap = new LinkedHashMap<>();
        Map<Object, BeanWrapper> idToBeanWrapperRelatedModelMap = new HashMap<>();
        while (rs.next()) {
          BeanWrapper bwOwnerModel =
              getBeanWrapperModel(rs, ownerTypeSelectMapper, idToBeanWrapperOwnerModelMap, true);
          if (relatedType != null && bwOwnerModel != null) {
            BeanWrapper bwRelatedModel = getBeanWrapperModel(rs, relatedTypeSelectMapper,
                idToBeanWrapperRelatedModelMap, false);
            Object relatedModel =
                bwRelatedModel == null ? null : bwRelatedModel.getWrappedInstance();
            if (RelationshipType.HAS_ONE == relationshipType) {
              bwOwnerModel.setPropertyValue(propertyName, relatedModel);
            } else if (RelationshipType.HAS_MANY == relationshipType
                || RelationshipType.HAS_MANY_THROUGH == relationshipType) {
              if (relatedModel != null) {
                // the property has already been validated so we know it is a
                // collection that has been initialized
                Collection collection = (Collection) bwOwnerModel.getPropertyValue(propertyName);
                collection.add(relatedModel);
              }
            }
          }
        }
        return idToBeanWrapperOwnerModelMap.values().stream().map(bw -> (T) bw.getWrappedInstance())
            .collect(Collectors.toList());
      }
    };

    List<T> resultList = null;
    if (whereParams == null) {
      resultList = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor);
    } else {
      resultList = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor, whereParams);
    }

    // code reaches here query success, handle caching
    if (!previouslySuccessfulQuery(cacheKey)) {
      addToCache(cacheKey, sqlForCache);
    }
    return resultList;
  }

  @SuppressWarnings("rawtypes")
  private BeanWrapper getBeanWrapperModel(ResultSet rs, SelectMapper<?> selectMapper,
      Map<Object, BeanWrapper> idToBeanWrapperModelMap, boolean isOwningModel) throws SQLException {
    BeanWrapper bwModel = null;
    Object id = rs.getObject(selectMapper.getResultSetModelIdColumnLabel());
    id = rs.wasNull() ? null : id; // some drivers are goofy
    if (id != null) {
      bwModel = idToBeanWrapperModelMap.get(id);
      if (bwModel == null) {
        bwModel = selectMapper.buildBeanWrapperModel(rs); // builds the model from resultSet
        if (isOwningModel && (RelationshipType.HAS_MANY == relationshipType
            || RelationshipType.HAS_MANY_THROUGH == relationshipType)) {
          // first time seeing the owning model. Make sure collection is clear.
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

  private String generateQuerySql(JdbcTemplateMapper jtm) {

    TableMapping ownerTypeTableMapping = jtm.getTableMapping(ownerType);
    SelectMapper<?> ownerTypeSelectMapper =
        jtm.getSelectMapper(ownerType, ownerTypeTableMapping.getTableName());
    String ownerTypeTableName = ownerTypeTableMapping.getTableName();

    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : jtm.getTableMapping(relatedType);
    SelectMapper<?> relatedTypeSelectMapper = relatedType == null ? null
        : jtm.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());

    String sql = "SELECT " + ownerTypeSelectMapper.getColumnsSql();
    if (relatedType != null) {
      sql += "," + relatedTypeSelectMapper.getColumnsSql();
    }

    sql += " FROM " + jtm.fullyQualifiedTableName(ownerTypeTableName);
    if (relatedType != null) {
      String relatedTypeTableName = relatedTypeTableMapping.getTableName();
      if (relationshipType == RelationshipType.HAS_ONE) {
        // joinColumn is on owner table
        sql += " LEFT JOIN " + jtm.fullyQualifiedTableName(relatedTypeTableMapping.getTableName())
            + " on " + ownerTypeTableName + "." + joinColumnOwningSide + " = "
            + relatedTypeTableName + "." + relatedTypeTableMapping.getIdColumnName();
      } else if (relationshipType == RelationshipType.HAS_MANY) {
        // joinColumn is on related table
        sql += " LEFT JOIN " + jtm.fullyQualifiedTableName(relatedTypeTableName) + " on "
            + ownerTypeTableName + "." + ownerTypeTableMapping.getIdColumnName() + " = "
            + relatedTypeTableName + "." + joinColumnManySide;
      } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
        sql += " LEFT JOIN " + jtm.fullyQualifiedTableName(throughJoinTable) + " on "
            + ownerTypeTableName + "." + ownerTypeTableMapping.getIdColumnName() + " = "
            + throughJoinTable + "." + throughOwnerTypeJoinColumn + " LEFT JOIN "
            + jtm.fullyQualifiedTableName(relatedTypeTableName) + " on " + throughJoinTable + "."
            + throughRelatedTypeJoinColumn + " = " + relatedTypeTableName + "."
            + relatedTypeTableMapping.getIdColumnName();
      }
    }
    if (MapperUtils.isNotBlank(whereClause)) {
      sql += " WHERE " + whereClause;
    }
    if (MapperUtils.isNotBlank(orderBy)) {
      sql += " ORDER BY " + orderBy;
    }
    return sql;
  }

  private String getCacheKey(JdbcTemplateMapper jdbcTemplateMapper) {
    // @formatter:off
    return String.join("-", 
        ownerType.getName(), 
        relatedType == null ? null : relatedType.getName(),
        relationshipType == null ? null : relationshipType.toString(), propertyName,
        joinColumnOwningSide, 
        joinColumnManySide, 
        throughJoinTable, 
        throughOwnerTypeJoinColumn,
        throughRelatedTypeJoinColumn,
        propertyName,
        whereClause,
        orderBy,
        jdbcTemplateMapper.toString());
    // @formatter:on
  }

  private String getSqlFromCache(String cacheKey) {
    return successQuerySqlCache.get(cacheKey);
  }

  private boolean previouslySuccessfulQuery(String key) {
    return successQuerySqlCache.containsKey(key);
  }

  private void addToCache(String key, String sql) {
    if (successQuerySqlCache.size() < CACHE_MAX_ENTRIES) {
      successQuerySqlCache.put(key, sql);
    } else {
      // remove a random entry from cache and add new entry
      String k = successQuerySqlCache.keySet().iterator().next();
      successQuerySqlCache.remove(k);

      successQuerySqlCache.put(key, sql);
    }
  }

}
