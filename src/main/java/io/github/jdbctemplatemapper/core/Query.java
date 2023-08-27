package io.github.jdbctemplatemapper.core;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

public class Query<T> {
    private Class<T> type;
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

    private Query(Class<T> type) {
        this.type = type;
    }

    public static <T> Query<T> find(Class<T> type) {
        return new Query<T>(type);
    }

    public static <T> Query<T> merge(Class<T> type) {
        return new Query<T>(type);
    }

    public Query<T> where(String whereClause, Object... params) {
        this.whereClause = whereClause;
        this.whereParams = params;
        return this;
    }

    public Query<T> orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public Query<T> hasMany(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public Query<T> hasOne(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public Query<T> joinColumn(String joinColumn) {
        this.joinColumn = joinColumn;
        return this;
    }

    public Query<T> throughJoinTable(String tableName) {
        this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
        this.throughJoinTable = tableName;
        return this;
    }

    public Query<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
        jtm = jdbcTemplateMapper;

        mappingHelper = jtm.getMappingHelper();
        mainClazzTableMapping = mappingHelper.getTableMapping(type);
        mainClazzSelectMapper = jtm.getSelectMapper(type, mainClazzTableMapping.getTableName());

        if (relatedClazz != null) {
            relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);
            relatedClazzSelectMapper = jtm.getSelectMapper(relatedClazz, "t2");
            // TONY validate join Table
        }

        String sql = "SELECT " + mainClazzSelectMapper.getColumnsSql();

        if (relatedClazz != null) {
            sql += "," + relatedClazzSelectMapper.getColumnsSql();
        }
        sql += " FROM " + mappingHelper.fullyQualifiedTableName(mainClazzTableMapping.getTableName()) + " "
                + mainClazzTableMapping.getTableName();

        if (relatedClazz != null) {
            if (relationshipType == RelationshipType.HAS_MANY) {
          //@formatter:off
            sql += " LEFT JOIN " 
                + mappingHelper.fullyQualifiedTableName(relatedClazzTableMapping.getTableName()) + " t2" 
                + " on " + mainClazzTableMapping.getTableName() + "." + mainClazzTableMapping.getIdColumnName() + " = " + "t2." + joinColumn;
          //@formatter:on
            } else if (relationshipType == RelationshipType.HAS_ONE) {
                //@formatter:off
                sql += " LEFT JOIN " 
                    + mappingHelper.fullyQualifiedTableName(relatedClazzTableMapping.getTableName()) + " t2" 
                    + " on " + mainClazzTableMapping.getTableName() + "."  + joinColumn + " = " + "t2." + relatedClazzTableMapping.getIdColumnName();
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
                    Object relatedModel = getModel(rs, relatedClazzSelectMapper, idToRelatedModelMap);

                    if (RelationshipType.HAS_ONE == relationshipType) {
                        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                        bw.setPropertyValue(propertyName, relatedModel);
                    }

                    if (RelationshipType.HAS_MANY == relationshipType) {
                        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                        // the property has already been validated by builder() so we know it is a
                        // collection
                        // that has been initialized
                        @SuppressWarnings("rawtypes")
                        Collection collection = (Collection) bw.getPropertyValue(propertyName);
                        collection.add(relatedModel);
                    }
                }

                return (List<T>) new ArrayList<>(idToModelMap.values());
            }
        };
        

        return jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor, whereParams);

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

    private Class<?> getGenericTypeOfCollection(Object mainObj, String propertyName) {
        try {
            Field field = mainObj.getClass().getDeclaredField(propertyName);

            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            Type[] genericType = pt.getActualTypeArguments();

            if (genericType != null && genericType.length > 0) {
                return Class.forName(genericType[0].getTypeName());
            }

        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    private void validate(JdbcTemplateMapper jtm) {
        String[] words = orderBy.split("\\s+");
        for (String word : words) {

        }
    }

}
