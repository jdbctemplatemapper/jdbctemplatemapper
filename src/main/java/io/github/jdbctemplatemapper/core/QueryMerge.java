package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.querymerge.IQueryMergeFluent;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasMany;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasOne;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumn;
import io.github.jdbctemplatemapper.querymerge.IQueryMergePopulateProperty;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeType;

public class QueryMerge<T> implements IQueryMergeFluent<T>{
    private Class<T> ownerType;
    private RelationshipType relationshipType;
    private Class<?> relatedType;
    private String propertyName; // propertyName on ownerType that needs to be populated
    private String joinColumn;

    private QueryMerge(Class<T> type) {
        this.ownerType = type;
    }

    public static <T> IQueryMergeType<T> type(Class<T> type) {
        Assert.notNull(type, "Type cannot be null");
        return new QueryMerge<T>(type);
    }

    public IQueryMergeHasMany<T> hasMany(Class<?> relatedType) {
        Assert.notNull(relatedType, "relatedType cannot be null");
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedType = relatedType;
        return this;
    }

    public IQueryMergeHasOne<T> hasOne(Class<?> relatedType) {
        Assert.notNull(relatedType, "relatedType cannot be null");
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedType = relatedType;
        return this;
    }

    public IQueryMergeJoinColumn<T> joinColumn(String joinColumn) {
        Assert.notNull(joinColumn, "joinColumn cannot be null");
        this.joinColumn = joinColumn;
        return this;
    }

    public IQueryMergePopulateProperty<T> populateProperty(String propertyName) {
        Assert.notNull(propertyName, "propertyName cannot be null");
        this.propertyName = propertyName;
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList) {
        Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");
        
        MappingHelper mappingHelper = jdbcTemplateMapper.getMappingHelper();
        TableMapping ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);
        TableMapping relatedTypeTableMapping = mappingHelper.getTableMapping(relatedType);
        
        QueryValidator.validate(jdbcTemplateMapper, ownerType, relationshipType, relatedType, joinColumn, propertyName, null, null,
                null);

        String joinPropertyName = relatedTypeTableMapping.getPropertyName(joinColumn);

        List<BeanWrapper> bwMergeList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        Set params = new HashSet<>();
        for (Object obj : mergeList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            params.add(bw.getPropertyValue(joinPropertyName));
            bwMergeList.add(bw);
        }

        List relatedList = jdbcTemplateMapper.findByProperty(relatedType, joinPropertyName, params);
        List<BeanWrapper> bwRelatedList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        for (Object obj : relatedList) {
            bwRelatedList.add(PropertyAccessorFactory.forBeanPropertyAccess(obj));
        }

        String ownerTypeIdName = ownerTypeTableMapping.getIdPropertyName();
        String relatedTypeIdName = relatedTypeTableMapping.getIdPropertyName();

        for (BeanWrapper bw : bwMergeList) {
            if (relationshipType == RelationshipType.HAS_ONE) {
                Object relatedObj = getRelatedObject(bwRelatedList, relatedTypeIdName,
                        bw.getPropertyValue(joinPropertyName));
                bw.setPropertyValue(propertyName, relatedObj);
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                List matchedList = getRelatedObjectList(bwRelatedList, joinPropertyName,
                        bw.getPropertyValue(ownerTypeIdName));

                Collection collection = (Collection) bw.getPropertyValue(propertyName);
                collection.addAll(matchedList);
            }
        }
    }

    private Object getRelatedObject(List<BeanWrapper> relatedList, String propName, Object matchValue) {
        for (BeanWrapper bw : relatedList) {
            if (MapperUtils.equals(bw.getPropertyValue(propName), matchValue)) {
                return bw.getWrappedInstance();
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List getRelatedObjectList(List<BeanWrapper> relatedList, String joinPropertyName,
            Object matchValue) {
        List list = new ArrayList();
        for (BeanWrapper bw : relatedList) {
            if (MapperUtils.equals(bw.getPropertyValue(joinPropertyName), matchValue)) {
                list.add(bw.getWrappedInstance());
            }
        }
        return list;
    }
}
