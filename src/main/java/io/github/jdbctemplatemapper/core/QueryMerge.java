package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import io.github.jdbctemplatemapper.querymerge.IQueryMergeExecute;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasMany;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasOne;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumn;
import io.github.jdbctemplatemapper.querymerge.IQueryMergePopulateProperty;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeType;

public class QueryMerge<T> implements IQueryMergeType<T>, IQueryMergeHasMany<T>, IQueryMergeHasOne<T>,
        IQueryMergeJoinColumn<T>, IQueryMergePopulateProperty<T>, IQueryMergeExecute<T> {
    private Class<T> mainClazz;
    private RelationshipType relationshipType;
    private Class<?> relatedClazz;
    private String propertyName; // propertyName on main class that needs to be populated
    private String joinColumn;
    private JdbcTemplateMapper jtm;
    private MappingHelper mappingHelper;
    private TableMapping relatedClazzTableMapping = null;

    private QueryMerge(Class<T> type) {
        this.mainClazz = type;
    }

    public static <T> IQueryMergeType<T> type(Class<T> type) {
        return new QueryMerge<T>(type);
    }

    public IQueryMergeHasMany<T> hasMany(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public IQueryMergeHasOne<T> hasOne(Class<?> relatedClazz) {
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedClazz = relatedClazz;
        return this;
    }

    public IQueryMergeJoinColumn<T> joinColumn(String joinColumn) {
        this.joinColumn = joinColumn;
        return this;
    }

    public IQueryMergePopulateProperty<T> populateProperty(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> list) {
        initialize(jdbcTemplateMapper);
        QueryValidator.validate(jtm, mainClazz, relationshipType, relatedClazz, joinColumn, propertyName, null, null,
                null);

        String joinPropertyName = relatedClazzTableMapping.getPropertyName(joinColumn);
        Set params = new HashSet<>();
        for (Object obj : list) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            params.add(bw.getPropertyValue(joinPropertyName));
        }

        List relatedList = jtm.findByProperty(relatedClazz, joinPropertyName, params);
        String relatedPropertyIdName = relatedClazzTableMapping.getIdPropertyName();

        for (Object obj : list) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            if (relationshipType == RelationshipType.HAS_ONE) {
                Object relatedObj = getRelatedObject(relatedList, relatedPropertyIdName,
                        bw.getPropertyValue(joinPropertyName));
                bw.setPropertyValue(propertyName, relatedObj);
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                List matchedList = getRelatedObjectList(relatedList, joinPropertyName,
                        bw.getPropertyValue(joinPropertyName));

                Collection collection = (Collection) bw.getPropertyValue(propertyName);
                collection.addAll(matchedList);
            }
        }
    }

    private void initialize(JdbcTemplateMapper jdbcTemplateMapper) {
        jtm = jdbcTemplateMapper;
        mappingHelper = jtm.getMappingHelper();
        relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);
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
            if (bw.getPropertyValue(joinPropertyName).equals(joinPropertyValue)) {
                list.add(obj);
            }
        }
        return list;
    }

}
