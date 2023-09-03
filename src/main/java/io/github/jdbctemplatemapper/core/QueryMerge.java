package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeFluent;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasMany;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeHasOne;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumn;
import io.github.jdbctemplatemapper.querymerge.IQueryMergePopulateProperty;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeType;

public class QueryMerge<T> implements IQueryMergeFluent<T> {
    private int inClauseChunkSize = 100;
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

    /**
     * hasOne relationship: The join column (the foreign key) is in the table of the
     * owning model. Example: Order hasOne Customer. The join column(foreign key)
     * will be on the table order (owning model)
     * 
     * 
     * hasMany relationship: The join column( foreign key) is in the table of the
     * related model. For example Order hasMay OrderLine then the join column will
     * on the table order_line (related model)
     *
     * @param joinColumn the join column
     */
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
        if (mergeList == null) {
            return;
        }
        MappingHelper mappingHelper = jdbcTemplateMapper.getMappingHelper();
        TableMapping ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);
        TableMapping relatedTypeTableMapping = mappingHelper.getTableMapping(relatedType);

        QueryValidator.validate(jdbcTemplateMapper, ownerType, relationshipType, relatedType, joinColumn, propertyName,
                null, null, null);

        // its already validated
        String joinPropertyName = relatedTypeTableMapping.getPropertyName(joinColumn);

        List<BeanWrapper> bwMergeList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        Set params = new HashSet<>();
        for (Object obj : mergeList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            params.add(bw.getPropertyValue(joinPropertyName));
            bwMergeList.add(bw);
        }

        List<?> relatedList = getByPropertyMultipleValues(jdbcTemplateMapper, relatedType, joinPropertyName, params);

        List<BeanWrapper> bwRelatedList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        for (Object obj : relatedList) {
            bwRelatedList.add(PropertyAccessorFactory.forBeanPropertyAccess(obj));
        }

        String ownerTypeIdPropName = ownerTypeTableMapping.getIdPropertyName();
        String relatedTypeIdPropName = relatedTypeTableMapping.getIdPropertyName();

        for (BeanWrapper bw : bwMergeList) {
            if (relationshipType == RelationshipType.HAS_ONE) {
                Object relatedObj = getRelatedObject(bwRelatedList, relatedTypeIdPropName,
                        bw.getPropertyValue(joinPropertyName));
                bw.setPropertyValue(propertyName, relatedObj);
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                List matchedList = getRelatedObjectList(bwRelatedList, joinPropertyName,
                        bw.getPropertyValue(ownerTypeIdPropName));
                if (matchedList != null) {
                    Collection collection = (Collection) bw.getPropertyValue(propertyName);
                    collection.addAll(matchedList);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <U> List<U> getByPropertyMultipleValues(JdbcTemplateMapper jtm, Class<U> clazz, String joinPropertyName,
            Set<?> propertyValues) {
        List<U> resultList = new ArrayList<>();
        MappingHelper mappingHelper = jtm.getMappingHelper();
        TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
        // already validated
        String joinPropColumnName = tableMapping.getColumnName(joinPropertyName);
        if (MapperUtils.isEmpty(propertyValues)) {
            return resultList;
        }

        String sql = "SELECT " + jtm.getColumnsSql(clazz) + " FROM "
                + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " WHERE " + joinPropColumnName
                + " IN (:propertyValues)";

        RowMapper<U> mapper = BeanPropertyRowMapper.newInstance(clazz);

        // some databases have limits on number of entries in a 'IN' clause
        // Chunk the collection of propertyValues and make multiple calls as needed.
        Collection<List<?>> chunkedPropertyValues = MapperUtils.chunkTheCollection(propertyValues, inClauseChunkSize);
        for (List<?> propValues : chunkedPropertyValues) {
            MapSqlParameterSource params = new MapSqlParameterSource("propertyValues", propValues);
            resultList.addAll(jtm.getNamedParameterJdbcTemplate().query(sql, params, mapper));
        }

        return resultList;
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
    private List getRelatedObjectList(List<BeanWrapper> relatedList, String joinPropertyName, Object matchValue) {
        List list = new ArrayList();
        for (BeanWrapper bw : relatedList) {
            if (MapperUtils.equals(bw.getPropertyValue(joinPropertyName), matchValue)) {
                list.add(bw.getWrappedInstance());
            }
        }
        return list.size() > 0 ? list : null;
    }
}
