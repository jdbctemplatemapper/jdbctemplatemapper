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

import io.github.jdbctemplatemapper.query.IQueryExecute;
import io.github.jdbctemplatemapper.query.IQueryHasMany;
import io.github.jdbctemplatemapper.query.IQueryHasOne;
import io.github.jdbctemplatemapper.query.IQueryJoinColumn;
import io.github.jdbctemplatemapper.query.IQueryOrderBy;
import io.github.jdbctemplatemapper.query.IQueryPopulateProperty;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinColumns;
import io.github.jdbctemplatemapper.query.IQueryThroughJoinTable;
import io.github.jdbctemplatemapper.query.IQueryType;
import io.github.jdbctemplatemapper.query.IQueryWhere;

public class Query<T> implements IQueryType<T>, IQueryWhere<T>, IQueryOrderBy<T>, IQueryHasMany<T>, IQueryHasOne<T>,
        IQueryJoinColumn<T>, IQueryThroughJoinTable<T>, IQueryThroughJoinColumns<T>, IQueryPopulateProperty<T>,
        IQueryExecute<T> {
    private Class<T> ownerType;
    private String whereClause;
    private Object[] whereParams;
    private String orderBy;

    private RelationshipType relationshipType;
    private Class<?> relatedType;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;

    private JdbcTemplateMapper jtm;
    private MappingHelper mappingHelper;
    private TableMapping ownerTypeTableMapping;
    private SelectMapper<?> ownerTypeSelectMapper;

    private TableMapping relatedTypeTableMapping = null;
    private SelectMapper<?> relatedTypeSelectMapper = null;

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
        this.whereClause = whereClause;
        this.whereParams = params;
        return this;
    }

    public IQueryOrderBy<T> orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public IQueryHasMany<T> hasMany(Class<?> relatedType) {
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedType = relatedType;
        return this;
    }

    public IQueryHasOne<T> hasOne(Class<?> relatedType) {
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedType = relatedType;
        return this;
    }

    public IQueryJoinColumn<T> joinColumn(String joinColumn) {
        if(joinColumn != null) {
            this.joinColumn = MapperUtils.toLowerCase(joinColumn.trim());
        }
        return this;
    }

    public IQueryThroughJoinTable<T> throughJoinTable(String tableName) {
        this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
        this.throughJoinTable = tableName;
        return this;
    }

    public IQueryThroughJoinColumns<T> throughJoinColumns(String ownerTypeJoinColumn, String relatedTypeJoinColumn) {
        this.throughOwnerTypeJoinColumn = ownerTypeJoinColumn;
        this.throughRelatedTypeJoinColumn = relatedTypeJoinColumn;
        return this;
    }

    public IQueryPopulateProperty<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
        initialize(jdbcTemplateMapper);

        QueryValidator.validate(jtm, ownerType, relationshipType, relatedType, joinColumn, propertyName,
                throughJoinTable, throughOwnerTypeJoinColumn, throughRelatedTypeJoinColumn);

        QueryValidator.validateWhereAndOrderBy(jtm, whereClause, orderBy, ownerType, relatedType);

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
                sql += " JOIN " + mappingHelper.fullyQualifiedTableName(throughJoinTable) + " on " + ownerTypeTableName
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

    private void initialize(JdbcTemplateMapper jdbcTemplateMapper) {
        this.jtm = jdbcTemplateMapper;
        this.mappingHelper = jtm.getMappingHelper();
        this.ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);
        this.ownerTypeSelectMapper = jtm.getSelectMapper(ownerType, ownerTypeTableMapping.getTableName());
        if (relatedType != null) {
            this.relatedTypeTableMapping = mappingHelper.getTableMapping(relatedType);
            this.relatedTypeSelectMapper = jtm.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());
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
