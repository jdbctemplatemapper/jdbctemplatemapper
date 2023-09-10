package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.query.IQueryFluent;
import io.github.jdbctemplatemapper.query.IQueryHasMany;
import io.github.jdbctemplatemapper.query.IQueryHasOne;
import io.github.jdbctemplatemapper.query.IQueryJoinColumnManySide;
import io.github.jdbctemplatemapper.query.IQueryJoinColumnOwningSide;
import io.github.jdbctemplatemapper.query.IQueryOrderBy;
import io.github.jdbctemplatemapper.query.IQueryPopulateProperty;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinColumns;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinTable;
import io.github.jdbctemplatemapper.query.IQueryType;
import io.github.jdbctemplatemapper.query.IQueryWhere;

/**
 *
 *
 * <pre>
 * Fluent style queries for relationships hasOne, hasMany, hasMany through (many
 * to many).
 *
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#querying-relationships">Querying
 * relationships</a> for more info
 * </pre>
 *
 * @author ajoseph
 */
public class Query<T> implements IQueryFluent<T> {
  // key - cacheKey
  // value - sql
  private static Map<String, String> successQuerySqlCache = new ConcurrentHashMap<>();

  private Class<T> ownerType;
  private String whereClause;
  private Object[] whereParams;
  private String orderBy;

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
   * The where clause for the type. If you provide a where clause always parameterize it to avoid
   * SQL injection. Columns should always be prefixed with table name.
   *
   * @param whereClause the whereClause for the type. It has to be prefixed with table name.
   * @param params varArgs for the whereClause.
   * @return interface with the next methods in the chain
   */
  public IQueryWhere<T> where(String whereClause, Object... params) {
    Assert.notNull(whereClause, "whereClause cannot be null");
    this.whereClause = whereClause;
    this.whereParams = params;
    return this;
  }

  /**
   * The orderBy clause for the query. Columns should always be prefixed with table name. If you are
   * querying relationships (hasOne, hasMany, hasMany through) both the owning side and related side
   * columns can be used in the orderBy clause
   *
   * <p>orderBy is strictly validated to protect against SQL injection.
   *
   * @param orderBy the orderBy clause. It has to be prefixed with table name.
   * @return interface with the next methods in the chain
   */
  public IQueryOrderBy<T> orderBy(String orderBy) {
    Assert.notNull(orderBy, "orderBy cannot be null");
    this.orderBy = orderBy;
    return this;
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
   * The hasMany relationship. The 'populateProperty' should be a collection and has to be
   * initialized.
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
   * Join column for hasOne relationship: The join column (the foreign key) is in the table of the
   * owning model. Example: Order hasOne Customer. The join column(foreign key) will be on the table
   * order (of the owning model)
   *
   * <p>The join column should not have a table prefix.
   *
   * @param joinColumnOwningSide the join column on the owning side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide) {
    Assert.notNull(joinColumnOwningSide, "joinColumnOwningSide cannot be null");
    this.joinColumnOwningSide = MapperUtils.toLowerCase(joinColumnOwningSide.trim());
    return this;
  }

  /**
   * Join column for hasMany relationship: The join column (the foreign key) is in the table of the
   * many side. Example: Order hasMany OrderLine. The join column will be on the table order_line
   * (the many side)
   *
   * <p>The join column should not have a table prefix.
   *
   * @param joinColumnManySide the join column on the many side (with no table prefix)
   * @return interface with the next methods in the chain
   */
  public IQueryJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide) {
    Assert.notNull(joinColumnManySide, "joinColumnManySide cannot be null");
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
    Assert.notNull(tableName, "tableName cannot be null");
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
  public IQueryThroughJoinColumns<T> throughJoinColumns(
      String ownerTypeJoinColumn, String relatedTypeJoinColumn) {
    Assert.notNull(ownerTypeJoinColumn, "ownerTypeJoinColumn cannot be null");
    Assert.notNull(relatedTypeJoinColumn, "relatedTypeJoinColumn cannot be null");
    this.throughOwnerTypeJoinColumn = ownerTypeJoinColumn;
    this.throughRelatedTypeJoinColumn = relatedTypeJoinColumn;
    return this;
  }

  /**
   * The relationship property that needs to be populated on the owning type.
   *
   * <p>For hasMany() this property has to be an initialized collection (cannot be null) and the
   * generic type should match the hasMany related type. For hasOne this property has to be the same
   * type as hasOne related type
   *
   * @param propertyName name of property that needs to be populated
   * @return interface with the next methods in the chain
   */
  public IQueryPopulateProperty<T> populateProperty(String propertyName) {
    Assert.notNull(propertyName, "propertyName cannot be null");
    this.propertyName = propertyName;
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

    MappingHelper mappingHelper = jdbcTemplateMapper.getMappingHelper();
    TableMapping ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);
    SelectMapper<?> ownerTypeSelectMapper =
        jdbcTemplateMapper.getSelectMapper(ownerType, ownerTypeTableMapping.getTableName());

    TableMapping relatedTypeTableMapping =
        relatedType == null ? null : mappingHelper.getTableMapping(relatedType);
    // making it effectively final to be used in inner class ResultSetExtractor
    SelectMapper<?> relatedTypeSelectMapper =
        relatedType == null
            ? null
            : jdbcTemplateMapper.getSelectMapper(
                relatedType, relatedTypeTableMapping.getTableName());

    String sql = null;
    String cacheKey = getQueryCacheKey(jdbcTemplateMapper);
    if (!previouslySuccessfulQuery(cacheKey)) {
      QueryValidator.validate(
          jdbcTemplateMapper,
          ownerType,
          relationshipType,
          relatedType,
          joinColumnOwningSide,
          joinColumnManySide,
          propertyName,
          throughJoinTable,
          throughOwnerTypeJoinColumn,
          throughRelatedTypeJoinColumn);
      QueryValidator.validateWhereAndOrderBy(
          jdbcTemplateMapper, whereClause, orderBy, ownerType, relatedType);

      sql = "SELECT " + ownerTypeSelectMapper.getColumnsSql();
      String ownerTypeTableName = ownerTypeTableMapping.getTableName();
      if (relatedType != null) {
        sql += "," + relatedTypeSelectMapper.getColumnsSql();
      }
      sql += " FROM " + mappingHelper.fullyQualifiedTableName(ownerTypeTableName);
      if (relatedType != null) {
        String relatedTypeTableName = relatedTypeTableMapping.getTableName();
        if (relationshipType == RelationshipType.HAS_ONE) {
          // joinColumn is on owner table
          sql +=
              " LEFT JOIN "
                  + mappingHelper.fullyQualifiedTableName(relatedTypeTableMapping.getTableName())
                  + " on "
                  + ownerTypeTableName
                  + "."
                  + joinColumnOwningSide
                  + " = "
                  + relatedTypeTableName
                  + "."
                  + relatedTypeTableMapping.getIdColumnName();
        } else if (relationshipType == RelationshipType.HAS_MANY) {
          // joinColumn is on related table
          sql +=
              " LEFT JOIN "
                  + mappingHelper.fullyQualifiedTableName(relatedTypeTableName)
                  + " on "
                  + ownerTypeTableName
                  + "."
                  + ownerTypeTableMapping.getIdColumnName()
                  + " = "
                  + relatedTypeTableName
                  + "."
                  + joinColumnManySide;
        } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
          sql +=
              " LEFT JOIN "
                  + mappingHelper.fullyQualifiedTableName(throughJoinTable)
                  + " on "
                  + ownerTypeTableName
                  + "."
                  + ownerTypeTableMapping.getIdColumnName()
                  + " = "
                  + throughJoinTable
                  + "."
                  + throughOwnerTypeJoinColumn
                  + " LEFT JOIN "
                  + mappingHelper.fullyQualifiedTableName(relatedTypeTableName)
                  + " on "
                  + throughJoinTable
                  + "."
                  + throughRelatedTypeJoinColumn
                  + " = "
                  + relatedTypeTableName
                  + "."
                  + relatedTypeTableMapping.getIdColumnName();
        }
      }
      if (MapperUtils.isNotBlank(whereClause)) {
        sql += " WHERE " + whereClause;
      }
      if (MapperUtils.isNotBlank(orderBy)) {
        sql = sql + " ORDER BY " + orderBy;
      }
    } else {
      sql = successQuerySqlCache.get(cacheKey);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    ResultSetExtractor<List<T>> rsExtractor =
        new ResultSetExtractor<List<T>>() {
          public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
            Map<Object, Object> idToOwnerModelMap = new LinkedHashMap<>(); // to retain order
            Map<Object, Object> idToRelatedModelMap = new HashMap<>();
            while (rs.next()) {
              Object ownerModel = getModel(rs, ownerTypeSelectMapper, idToOwnerModelMap);
              if (relatedType != null) {
                Object relatedModel = getModel(rs, relatedTypeSelectMapper, idToRelatedModelMap);
                if (RelationshipType.HAS_ONE == relationshipType) {
                  BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
                  bw.setPropertyValue(propertyName, relatedModel);
                } else if (RelationshipType.HAS_MANY == relationshipType
                    || RelationshipType.HAS_MANY_THROUGH == relationshipType) {
                  if (relatedModel != null) {
                    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
                    // the property has already been validated so we know it is a
                    // collection that has been initialized
                    Collection collection = (Collection) bw.getPropertyValue(propertyName);
                    collection.add(relatedModel);
                  }
                }
              }
            }
            return (List<T>) new ArrayList<>(idToOwnerModelMap.values());
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
      successQuerySqlCache.put(cacheKey, sql);
    }
    return resultList;
  }

  private Object getModel(
      ResultSet rs, SelectMapper<?> selectMapper, Map<Object, Object> idToModelMap)
      throws SQLException {
    Object model = null;
    Object id = rs.getObject(selectMapper.getResultSetModelIdColumnLabel());
    if (id != null) {
      model = idToModelMap.get(id);
      if (model == null) {
        model = selectMapper.buildModel(rs); // builds the model from resultSet
        idToModelMap.put(id, model);
      }
    }
    return model;
  }

  private String getQueryCacheKey(JdbcTemplateMapper jdbcTemplateMapper) {
    // @formatter:off
    return String.join(
        "-",
        ownerType.getName(),
        relatedType == null ? "" : relatedType.getName(),
        relationshipType == null ? "" : relationshipType.toString(),
        propertyName,
        joinColumnOwningSide,
        joinColumnManySide,
        throughJoinTable,
        throughOwnerTypeJoinColumn,
        throughRelatedTypeJoinColumn,
        whereClause,
        orderBy,
        jdbcTemplateMapper.toString());
    // @formatter:on
  }

  private boolean previouslySuccessfulQuery(String key) {
    return successQuerySqlCache.containsKey(key);
  }
}
