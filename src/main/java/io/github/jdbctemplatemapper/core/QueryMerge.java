package io.github.jdbctemplatemapper.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class QueryMerge<T> {
    private Class<T> type;

    private RelationshipType relationshipType;
    private Class<?> relatedClazz;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;
    private String throughJoinTable;

    private QueryMerge(Class<T> type) {
        this.type = type;
    }

    public static <T> QueryMerge<T> type(Class<T> type) {
        return new QueryMerge<T>(type);
    }

    public QueryMerge<T> hasMany(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public QueryMerge<T> hasOne(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public QueryMerge<T> joinColumn(String joinColumn) {
        this.joinColumn = joinColumn;
        return this;
    }

    public QueryMerge<T> throughJoinTable(String tableName) {
        this.relationshipType = RelationshipType.HAS_MANY_THROUGH;
        this.throughJoinTable = tableName;
        return this;
    }

    public QueryMerge<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<?> list) {
        JdbcTemplateMapper jtm = jdbcTemplateMapper;

        MappingHelper mappingHelper = jtm.getMappingHelper();
        mappingHelper.getTableMapping(type);
        
        TableMapping relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);

        Set params = new HashSet<>();        
        // TONY validate
        String joinPropertyName = relatedClazzTableMapping.getProperyName(joinColumn);

        for (Object obj : list) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            params.add(bw.getPropertyValue(joinPropertyName));
        }

        String sql = "SELECT " + jtm.getColumnsSql(relatedClazz) + " FROM "
                + mappingHelper.fullyQualifiedTableName(relatedClazzTableMapping.getTableName()) + " WHERE "
                + joinColumn + " IN (:propertyValues)";

        RowMapper<?> mapper = BeanPropertyRowMapper.newInstance(relatedClazz);

        List<?> relatedList = jtm.getNamedParameterJdbcTemplate().query(sql,
                new MapSqlParameterSource("propertyValues", params), mapper);
        
        String relatedPropertyIdName = relatedClazzTableMapping.getIdPropertyName();
             
        // TONY handle hasMany because relatedObj return could be a list.
        for (Object obj : list) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            Object joinPropertyValue = bw.getPropertyValue(joinPropertyName);
            Object relatedObj = getRelatedObject(relatedList, relatedPropertyIdName, joinPropertyValue);
            bw.setPropertyValue(propertyName, relatedObj);
            //bw.setPropertyValue(propertyName,  );
        }
    }
    
    // TONY: should handle hasMany so return should be List
    private Object getRelatedObject(List<?> relatedList, String idName, Object idValue) {
        for(Object obj : relatedList) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
        // TONY handle null
        if(bw.getPropertyValue(idName).equals(idValue))
            return obj; 
        }
        return null;
    }

}
