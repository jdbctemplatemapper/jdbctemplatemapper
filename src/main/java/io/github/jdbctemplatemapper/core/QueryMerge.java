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
    private Class<T> ownerType;
    private RelationshipType relationshipType;
    private Class<?> relatedType;
    private String propertyName; // propertyName on ownerType that needs to be populated
    private String joinColumn;
    private JdbcTemplateMapper jtm;
    private MappingHelper mappingHelper;
    private TableMapping relatedTypeTableMapping = null;

    private QueryMerge(Class<T> type) {
        this.ownerType = type;
    }

    public static <T> IQueryMergeType<T> type(Class<T> type) {
        return new QueryMerge<T>(type);
    }

    public IQueryMergeHasMany<T> hasMany(Class<?> relatedType) {
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedType = relatedType;
        return this;
    }

    public IQueryMergeHasOne<T> hasOne(Class<?> relatedType) {
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedType = relatedType;
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
    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList) {
        initialize(jdbcTemplateMapper);
        QueryValidator.validate(jtm, ownerType, relationshipType, relatedType, joinColumn, propertyName, null, null,
                null);

        String joinPropertyName = relatedTypeTableMapping.getPropertyName(joinColumn);

        List<BeanWrapper> bwMergeList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        Set params = new HashSet<>();
        for (Object obj : mergeList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            params.add(bw.getPropertyValue(joinPropertyName));
            bwMergeList.add(bw);
        }

        List relatedList = jtm.findByProperty(relatedType, joinPropertyName, params);
        List<BeanWrapper> bwRelatedList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        for (Object obj : relatedList) {
            bwRelatedList.add(PropertyAccessorFactory.forBeanPropertyAccess(obj));
        }

        String relatedPropertyIdName = relatedTypeTableMapping.getIdPropertyName();

        for (BeanWrapper bw : bwMergeList) {
            if (relationshipType == RelationshipType.HAS_ONE) {
                Object relatedObj = getRelatedObject(bwRelatedList, relatedPropertyIdName,
                        bw.getPropertyValue(joinPropertyName));
                bw.setPropertyValue(propertyName, relatedObj);
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                List matchedList = getRelatedObjectList(bwRelatedList, joinPropertyName,
                        bw.getPropertyValue(joinPropertyName));

                Collection collection = (Collection) bw.getPropertyValue(propertyName);
                collection.addAll(matchedList);
            }
        }
    }

    private void initialize(JdbcTemplateMapper jdbcTemplateMapper) {
        jtm = jdbcTemplateMapper;
        mappingHelper = jtm.getMappingHelper();
        relatedTypeTableMapping = mappingHelper.getTableMapping(relatedType);
    }

    private Object getRelatedObject(List<BeanWrapper> relatedList, String idName, Object idValue) {
        for (BeanWrapper bw : relatedList) {
            // TONY handle null
            if (bw.getPropertyValue(idName).equals(idValue)) {
                return bw.getWrappedInstance();
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private List getRelatedObjectList(List<BeanWrapper> relatedList, String joinPropertyName,
            Object joinPropertyValue) {
        List list = new ArrayList();
        for (BeanWrapper bw : relatedList) {
            if (bw.getPropertyValue(joinPropertyName).equals(joinPropertyValue)) {
                list.add(bw.getWrappedInstance());
            }
        }
        return list;
    }

}
