package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.query.IQueryFluent;
import io.github.jdbctemplatemapper.query.IQueryHasMany;
import io.github.jdbctemplatemapper.query.IQueryHasOne;
import io.github.jdbctemplatemapper.query.IQueryJoinColumn;
import io.github.jdbctemplatemapper.query.IQueryOrderBy;
import io.github.jdbctemplatemapper.query.IQueryPopulateProperty;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinColumns;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinTable;
import io.github.jdbctemplatemapper.query.IQueryType;
import io.github.jdbctemplatemapper.query.IQueryWhere;

public class Query<T> implements IQueryFluent<T> {
    private Class<T> ownerType;
    private String whereClause;
    private Object[] whereParams;
    private String orderBy;

    private RelationshipType relationshipType;
    private Class<?> relatedType;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;

    private String throughJoinTable;
    private String throughOwnerTypeJoinColumn;
    private String throughRelatedTypeJoinColumn;

    private Query(Class<T> type) {
        this.ownerType = type;
    }

    public static <T> IQueryType<T> type(Class<T> type) {
        return new Query<T>(type);
    }

    public IQueryWhere<T> where(String whereClause, Object... params) {
        Assert.notNull(whereClause, "WhereClause cannot be null");
        this.whereClause = whereClause;
        this.whereParams = params;
        return this;
    }

    public IQueryOrderBy<T> orderBy(String orderBy) {
        Assert.notNull(orderBy, "orderBy cannot be null");
        this.orderBy = orderBy;
        return this;
    }

    public IQueryHasMany<T> hasMany(Class<?> relatedType) {
        Assert.notNull(relatedType, "relatedType cannot be null");
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedType = relatedType;
        return this;
    }

    public IQueryHasOne<T> hasOne(Class<?> relatedType) {
        Assert.notNull(relatedType, "relatedType cannot be null");
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedType = relatedType;
        return this;
    }

    /**
     * hasOne relationship:
     *   The join column (the foreign key) is in the table of the owning model.
     *   Example: Order hasOne Customer. The join column(foreign key) will be on the table order (of the owning model)
     *   
     *   
     * hasMany relationship:
     *   The join column( foreign key)  is in the table of the related model.
     *   For example Order hasMay OrderLine then the join column will on the table order_line (of the related model)
     *
     * @param joinColumn the join column
     */
    public IQueryJoinColumn<T> joinColumn(String joinColumn) {
        Assert.notNull(joinColumn, "joinColumn cannot be null");
        this.joinColumn = MapperUtils.toLowerCase(joinColumn.trim());
        return this;
    }

    public IQueryThroughJoinTable<T> throughJoinTable(String tableName) {
        Assert.notNull(tableName, "tableName cannot be null");
        this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
        this.throughJoinTable = tableName;
        return this;
    }

    public IQueryThroughJoinColumns<T> throughJoinColumns(String ownerTypeJoinColumn, String relatedTypeJoinColumn) {
        Assert.notNull(ownerTypeJoinColumn, "ownerTypeJoinColumn cannot be null");
        Assert.notNull(relatedTypeJoinColumn, "relatedTypeJoinColumn cannot be null");
        this.throughOwnerTypeJoinColumn = ownerTypeJoinColumn;
        this.throughRelatedTypeJoinColumn = relatedTypeJoinColumn;
        return this;
    }

    public IQueryPopulateProperty<T> populateProperty(String propertyName) {
        Assert.notNull(propertyName, "propertyName cannot be null");
        this.propertyName = propertyName;
        return this;
    }

    public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
        Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");
        
        MappingHelper mappingHelper = jdbcTemplateMapper.getMappingHelper();
        TableMapping ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);
        SelectMapper<?> ownerTypeSelectMapper = jdbcTemplateMapper.getSelectMapper(ownerType, ownerTypeTableMapping.getTableName());
        
        TableMapping relatedTypeTableMapping = relatedType == null ? null : mappingHelper.getTableMapping(relatedType);
        // making it effectively final to be used in inner class ResultSetExtractor
        SelectMapper<?> relatedTypeSelectMapper = relatedType == null ? null : jdbcTemplateMapper.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());
  
        QueryValidator.validate(jdbcTemplateMapper, ownerType, relationshipType, relatedType, joinColumn, propertyName,
                throughJoinTable, throughOwnerTypeJoinColumn, throughRelatedTypeJoinColumn);

        QueryValidator.validateWhereAndOrderBy(jdbcTemplateMapper, whereClause, orderBy, ownerType, relatedType);
        
        String sql = "SELECT " + ownerTypeSelectMapper.getColumnsSql();

        String ownerTypeTableName = ownerTypeTableMapping.getTableName();

        if (relatedType != null) {
            sql += "," + relatedTypeSelectMapper.getColumnsSql();
        }
        sql += " FROM " + mappingHelper.fullyQualifiedTableName(ownerTypeTableName);

        if (relatedType != null) {
            String relatedTypeTableName = relatedTypeTableMapping.getTableName();
            if (relationshipType == RelationshipType.HAS_ONE) {
                // joinColumn is on owner table
                sql += " LEFT JOIN " + mappingHelper.fullyQualifiedTableName(relatedTypeTableMapping.getTableName())
                        + " on " + ownerTypeTableName + "." + joinColumn + " = " + relatedTypeTableName + "."
                        + relatedTypeTableMapping.getIdColumnName();
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                // joinColumn is on related table
                sql += " LEFT JOIN " + mappingHelper.fullyQualifiedTableName(relatedTypeTableName) + " on "
                        + ownerTypeTableName + "." + ownerTypeTableMapping.getIdColumnName() + " = "
                        + relatedTypeTableName + "." + joinColumn;
            } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
                sql += " LEFT JOIN " + mappingHelper.fullyQualifiedTableName(throughJoinTable) + " on " + ownerTypeTableName
                        + "." + ownerTypeTableMapping.getIdColumnName() + " = " + throughJoinTable + "."
                        + throughOwnerTypeJoinColumn + " JOIN "
                        + mappingHelper.fullyQualifiedTableName(relatedTypeTableName) + " on " + throughJoinTable + "."
                        + throughRelatedTypeJoinColumn + " = " + relatedTypeTableName + "."
                        + relatedTypeTableMapping.getIdColumnName();
            }
        }

        if (MapperUtils.isNotBlank(whereClause)) {
            sql += " WHERE " + whereClause;
        }

        if (MapperUtils.isNotBlank(orderBy)) {
            sql = sql + " ORDER BY " + orderBy;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Object, Object> idToOwnerModelMap = new HashMap<>();
                Map<Object, Object> idToRelatedModelMap = new HashMap<>();
                while (rs.next()) {
                    Object mainModel = getModel(rs, ownerTypeSelectMapper, idToOwnerModelMap);
                    if (relatedType != null) {
                        Object relatedModel = getModel(rs, relatedTypeSelectMapper, idToRelatedModelMap);
                        if (RelationshipType.HAS_ONE == relationshipType) {
                            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                            bw.setPropertyValue(propertyName, relatedModel);
                        } else if (RelationshipType.HAS_MANY == relationshipType
                                || RelationshipType.HAS_MANY_THROUGH == relationshipType) {
                            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                            // the property has already been validated so we know it is a
                            // collection that has been initialized
                            Collection collection = (Collection) bw.getPropertyValue(propertyName);
                            collection.add(relatedModel);
                        }
                    }
                }
                return (List<T>) new ArrayList<>(idToOwnerModelMap.values());
            }
        };

        if (whereParams == null) {
            return jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor);
        } else {
            return jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor, whereParams);
        }
    }

    private Object getModel(ResultSet rs, SelectMapper<?> selectMapper, Map<Object, Object> idToModelMap)
            throws SQLException {
        Object id = rs.getObject(selectMapper.getResultSetModelIdColumnLabel());
        Object model = idToModelMap.get(id);
        if (model == null) {
            model = selectMapper.buildModel(rs); // builds the model from resultSet
            idToModelMap.put(id, model);
        }
        return model;
    }
}
