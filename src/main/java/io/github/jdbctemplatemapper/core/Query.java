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

public class Query<T> implements IQueryType<T>, IQueryWhere<T>, IQueryOrderBy<T>, IQueryHasMany<T>, IQueryHasOne<T>,
        IQueryJoinColumn<T>, IQueryThroughJoinTable<T>, IQueryThroughJoinColumns<T>, IQueryPopulateProperty<T>,
        IQueryExecute<T> {
    private Class<T> mainClazz;
    private String whereClause;
    private Object[] whereParams;
    private String orderBy;

    private RelationshipType relationshipType;
    private Class<?> relatedClazz;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;
    private String throughJoinTable;

    private JdbcTemplateMapper jtm;
    private MappingHelper mappingHelper;
    private TableMapping mainClazzTableMapping;
    private SelectMapper<?> mainClazzSelectMapper;

    private TableMapping relatedClazzTableMapping = null;
    private SelectMapper<?> relatedClazzSelectMapper = null;

    String mainClazzJoinColumn;
    String relatedClazzJoinColumn;

    private Query(Class<T> type) {
        this.mainClazz = type;
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

    public IQueryHasMany<T> hasMany(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public IQueryHasOne<T> hasOne(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public IQueryJoinColumn<T> joinColumn(String joinColumn) {
        // TONY check for null or empty
        this.joinColumn = MapperUtils.toLowerCase(joinColumn.trim());
        return this;
    }

    public IQueryThroughJoinTable<T> throughJoinTable(String tableName) {
        this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
        this.throughJoinTable = tableName;
        return this;
    }

    public IQueryThroughJoinColumns<T> throughJoinColumns(String mainClassJoinColumn, String relatedClassJoinColumn) {
        this.mainClazzJoinColumn = mainClassJoinColumn;
        this.relatedClazzJoinColumn = relatedClassJoinColumn;
        return this;
    }

    public IQueryPopulateProperty<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
        initialize(jdbcTemplateMapper);

        QueryValidator.validate(jtm, mainClazz, relationshipType, relatedClazz, joinColumn, propertyName);

        QueryValidator.validateOrderBy(jtm, orderBy, mainClazz, relatedClazz, null);

        String sql = "SELECT " + mainClazzSelectMapper.getColumnsSql();

        String mainClazzTableName = mainClazzTableMapping.getTableName();

        if (relatedClazz != null) {
            sql += "," + relatedClazzSelectMapper.getColumnsSql();
        }
        sql += " FROM " + mappingHelper.fullyQualifiedTableName(mainClazzTableName);

        if (relatedClazz != null) {
            String relatedClazzTableName = relatedClazzTableMapping.getTableName();
            if (relationshipType == RelationshipType.HAS_ONE) {
                //@formatter:off
                sql += " LEFT JOIN " 
                    + mappingHelper.fullyQualifiedTableName(relatedClazzTableMapping.getTableName()) 
                    + " on " + mainClazzTableName + "."  + joinColumn + " = " + relatedClazzTableName + "." + relatedClazzTableMapping.getIdColumnName();
              //@formatter:on
            } else if (relationshipType == RelationshipType.HAS_MANY) {
              //@formatter:off
              sql += " LEFT JOIN " 
                + mappingHelper.fullyQualifiedTableName(relatedClazzTableName) 
                + " on " + mainClazzTableName + "." + mainClazzTableMapping.getIdColumnName() + " = " + relatedClazzTableName +"." + joinColumn;
          //@formatter:on
            } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
                //@formatter:off
                sql += " JOIN " 
                    + mappingHelper.fullyQualifiedTableName(throughJoinTable) 
                    + " on " + mainClazzTableName + "." + mainClazzTableMapping.getIdColumnName() + " = " + throughJoinTable +"." + mainClazzJoinColumn
                + " JOIN "
                + mappingHelper.fullyQualifiedTableName(relatedClazzTableName)
                + " on " + throughJoinTable +"." + relatedClazzJoinColumn + " = " + relatedClazzTableName + "." + relatedClazzTableMapping.getIdColumnName();

              //@formatter:on
            }
        }

        if (MapperUtils.isNotEmpty(whereClause)) {
            sql += " WHERE " + whereClause;
        }

        if (MapperUtils.isNotEmpty(orderBy)) {
            sql = sql + " ORDER BY " + orderBy;
        }

        @SuppressWarnings("unchecked")
        ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Object, Object> idToModelMap = new HashMap<>();
                Map<Object, Object> idToRelatedModelMap = new HashMap<>();

                while (rs.next()) {
                    Object mainModel = getModel(rs, mainClazzSelectMapper, idToModelMap);
                    if (relatedClazz != null) {
                        Object relatedModel = getModel(rs, relatedClazzSelectMapper, idToRelatedModelMap);
                        if (RelationshipType.HAS_ONE == relationshipType) {
                            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                            bw.setPropertyValue(propertyName, relatedModel);
                        }
                        if (RelationshipType.HAS_MANY == relationshipType) {
                            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                            // the property has already been validated so we know it is a
                            // collection that has been initialized
                            @SuppressWarnings("rawtypes")
                            Collection collection = (Collection) bw.getPropertyValue(propertyName);
                            collection.add(relatedModel);
                        }

                        if (RelationshipType.HAS_MANY_THROUGH == relationshipType) {
                            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                            // the property has already been validated so we know it is a
                            // collection that has been initialized
                            @SuppressWarnings("rawtypes")
                            Collection collection = (Collection) bw.getPropertyValue(propertyName);
                            collection.add(relatedModel);
                        }
                    }
                }
                return (List<T>) new ArrayList<>(idToModelMap.values());
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
        this.mainClazzTableMapping = mappingHelper.getTableMapping(mainClazz);
        // using table name as alias
        this.mainClazzSelectMapper = jtm.getSelectMapper(mainClazz, mainClazzTableMapping.getTableName());
        if (relatedClazz != null) {
            this.relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);
            this.relatedClazzSelectMapper = jtm.getSelectMapper(relatedClazz, relatedClazzTableMapping.getTableName());
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
