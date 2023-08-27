package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public class QueryMerge<T> {
    private Class<T> type;

    private RelationshipType relationshipType;
    private Class<?> relatedClazz;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;
    //private String throughJoinTable;

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
        //this.throughJoinTable = tableName;
        return this;
    }

    public QueryMerge<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> list) {
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

        List relatedList = jtm.findByProperty(relatedClazz, joinPropertyName, params);
        String relatedPropertyIdName = relatedClazzTableMapping.getIdPropertyName();

        for (Object obj : list) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            if (relationshipType == RelationshipType.HAS_ONE) {
                // Object joinPropertyValue = bw.getPropertyValue(joinPropertyName);
                Object relatedObj = getRelatedObject(relatedList, relatedPropertyIdName,
                        bw.getPropertyValue(joinPropertyName));
                bw.setPropertyValue(propertyName, relatedObj);
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                // TONY fix the name
                List l = getRelatedObjectList(relatedList, joinPropertyName,
                        bw.getPropertyValue(joinPropertyName));
                        
                Collection collection = (Collection) bw.getPropertyValue(propertyName);
                collection.addAll(l);
            }
        }
    }

    private Object getRelatedObject(List<?> relatedList, String idName, Object idValue) {
        for (Object obj : relatedList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            // TONY handle null
            if (bw.getPropertyValue(idName).equals(idValue)) {
                return obj;
            }
        }
        return null;
    }

    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List getRelatedObjectList(List<?> relatedList, String joinPropertyName, Object joinPropertyValue) {
        List list = new ArrayList();
        for (Object obj : relatedList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);            
            // TONY handle null
            if (bw.getPropertyValue(joinPropertyName).equals(joinPropertyValue)) {
                list.add(obj);
            }
        }
        return list;
    }
    
    
    // Validation the joinProperty type should match id property type

}
