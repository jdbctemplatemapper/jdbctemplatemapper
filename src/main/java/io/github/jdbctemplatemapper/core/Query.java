package io.github.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
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

import io.github.jdbctemplatemapper.exception.QueryException;

public class Query<T> {
    private Class<T> mainClazz;
    private String whereClause;
    private Object[] whereParams;
    private String orderBy;

    private RelationshipType relationshipType;
    private Class<?> relatedClazz;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;
    //private String throughJoinTable;

    
    private JdbcTemplateMapper jtm;
    private MappingHelper mappingHelper;
    private TableMapping mainClazzTableMapping;
    private SelectMapper<?> mainClazzSelectMapper;

    private TableMapping relatedClazzTableMapping = null;
    private SelectMapper<?> relatedClazzSelectMapper = null;

    private Query(Class<T> type) {
        this.mainClazz = type;
    }

    public static <T> Query<T> type(Class<T> type) {
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
        // TONY check for null or empty
        this.joinColumn = MapperUtils.toLowerCase(joinColumn.trim());
        return this;
    }

    public Query<T> throughJoinTable(String tableName) {
        this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
        //this.throughJoinTable = tableName;
        return this;
    }

    public Query<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public List<T> execute(JdbcTemplateMapper jdbcTemplateMapper) {
        initialize(jdbcTemplateMapper);
        validate();
        
        String sql = "SELECT " + mainClazzSelectMapper.getColumnsSql();

        if (relatedClazz != null) {
            sql += "," + relatedClazzSelectMapper.getColumnsSql();
        }
        sql += " FROM " + mappingHelper.fullyQualifiedTableName(mainClazzTableMapping.getTableName()) + " "
                + mainClazzTableMapping.getTableName();

        if (relatedClazz != null) {
            String relatedTableAlias = getRelatedTableAlias(mainClazzTableMapping.getTableName());
            if (relationshipType == RelationshipType.HAS_MANY) {
          //@formatter:off
            sql += " LEFT JOIN " 
                + mappingHelper.fullyQualifiedTableName(relatedClazzTableMapping.getTableName()) + " " + relatedTableAlias 
                + " on " + mainClazzTableMapping.getTableName() + "." + mainClazzTableMapping.getIdColumnName() + " = " + relatedTableAlias +"." + joinColumn;
          //@formatter:on
            } else if (relationshipType == RelationshipType.HAS_ONE) {
                //@formatter:off
                sql += " LEFT JOIN " 
                    + mappingHelper.fullyQualifiedTableName(relatedClazzTableMapping.getTableName()) + " " + relatedTableAlias 
                    + " on " + mainClazzTableMapping.getTableName() + "."  + joinColumn + " = " + relatedTableAlias + "." + relatedClazzTableMapping.getIdColumnName();
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
                        // the property has already been validated so we know it is a
                        // collection that has been initialized
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
    
    private void initialize(JdbcTemplateMapper jdbcTemplateMapper) {     
        this.jtm = jdbcTemplateMapper;
        mappingHelper = jtm.getMappingHelper();
        mainClazzTableMapping = mappingHelper.getTableMapping(mainClazz);
        // using table name as alias
        mainClazzSelectMapper = jtm.getSelectMapper(mainClazz, mainClazzTableMapping.getTableName());
        if (relatedClazz != null) {
            relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);
            relatedClazzSelectMapper = jtm.getSelectMapper(relatedClazz,
                    getRelatedTableAlias(mainClazzTableMapping.getTableName()));
        }
    }
    
    private void validate() {
        if (relatedClazz != null) {     
            Object mainModel = null;
            try {
                mainModel = mainClazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
            if (!bw.isReadableProperty(propertyName)) {
                throw new QueryException(
                        "Invalid property name " + propertyName + " for class " + mainClazz.getSimpleName());
            }
            
            if (relationshipType == RelationshipType.HAS_ONE) {
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                if (! relatedClazz.isAssignableFrom(pd.getPropertyType())) {
                    throw new QueryException("property type conflict. property "
                            + mainClazz.getSimpleName() + "." + propertyName
                            + " is of type " + pd.getPropertyType().getSimpleName()
                            + " while type for hasOne relationship is "
                            + relatedClazz.getSimpleName());
                }
                
                if (mainClazzTableMapping.getProperyName(joinColumn) == null) {
                    throw new QueryException("Invalid join column " + joinColumn
                            + " table " + mainClazzTableMapping.getTableName() + " for object " + mainClazz.getSimpleName() + " does not have a column " + joinColumn);
                }
                
            }
            else if(relationshipType == RelationshipType.HAS_MANY) {
                if (relatedClazzTableMapping.getProperyName(joinColumn) == null) {
                    throw new QueryException("Invalid join column " + joinColumn
                            + " . table " + relatedClazzTableMapping.getTableName() + " for object " + relatedClazz.getSimpleName() + " does not have a column " + joinColumn);
                }
                
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
                    throw new QueryException("property " + mainClazz.getSimpleName()
                            + "." + propertyName
                            + " is not a collection. hasMany() relationship requires it to be a collection");
                }
                
                Class<?> propertyType = getGenericTypeOfCollection(mainModel, propertyName);
                if (propertyType == null) {
                    throw new QueryException(
                            "Collections without generic types are not supported. Collection for property "
                                    + mainClazz.getSimpleName() + "."
                                    + propertyName + " does not have a generic type.");
                }
                
                if (!propertyType.isAssignableFrom(relatedClazz)) {
                    throw new QueryException(
                            "Collection generic type and hasMany relationship type mismatch. "
                                    + mainClazz.getSimpleName() + "."
                                    + propertyName + " has generic type " + propertyType.getSimpleName()
                                    + " while the hasMany relationship is of type "
                                    + relatedClazz.getSimpleName());
                }
                Object value = bw.getPropertyValue(propertyName);
                if (value == null) {
                    throw new QueryException(
                            "Query only works with initialized collections. Collection property "
                                    + mainClazz.getSimpleName() + "."
                                    + propertyName + " is not initialized");
                }
            }
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

    private String getRelatedTableAlias(String tableName) {
        String tableAlias = "t1";
        // just making sure related table alias does not conflict with main table name.
        if (tableAlias.equals(tableName)) {
            tableAlias = "t2";
        }
        return tableAlias;
    }

}
