package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.Assert;

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
        this.joinColumn = MapperUtils.toLowerCase(joinColumn.trim());
        return this;
    }

    public IQueryMergePopulateProperty<T> populateProperty(String propertyName) {
        Assert.notNull(propertyName, "propertyName cannot be null");
        this.propertyName = propertyName;
        return this;
    }

    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList) {
        Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

        QueryValidator.validate(jdbcTemplateMapper, ownerType, relationshipType, relatedType, joinColumn, propertyName,
                null, null, null);

        if (MapperUtils.isEmpty(mergeList)) {
            return;
        }

        if (relationshipType == RelationshipType.HAS_ONE) {
            processHasOne(jdbcTemplateMapper, mergeList, ownerType, relatedType);
        } else {
            processHasMany(jdbcTemplateMapper, mergeList, ownerType, relatedType);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processHasOne(JdbcTemplateMapper jtm, List<T> mergeList, Class<?> ownerType, Class<?> relatedType) {

        TableMapping ownerTypeTableMapping = jtm.getMappingHelper().getTableMapping(ownerType);
        TableMapping relatedTypeTableMapping = jtm.getMappingHelper().getTableMapping(relatedType);
        String joinPropertyName = ownerTypeTableMapping.getPropertyName(joinColumn);

        List<BeanWrapper> bwMergeList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        Set queryParams = new HashSet<>();
        for (T obj : mergeList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            Object joinPropertyValue = bw.getPropertyValue(joinPropertyName);
            if (joinPropertyValue != null) {
                queryParams.add(joinPropertyValue);
                bwMergeList.add(bw);
            }
        }

        if (MapperUtils.isEmpty(queryParams)) {
            return;
        }

        SelectMapper<?> selectMapper = jtm.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());
        String sql = "SELECT " + selectMapper.getColumnsSql() + " FROM "
                + jtm.getMappingHelper().fullyQualifiedTableName(relatedTypeTableMapping.getTableName()) + " WHERE "
                + relatedTypeTableMapping.getIdColumnName() + " IN (:propertyValues)";

        Map<Object, Object> idToRelatedModelMap = new HashMap<>();
        ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                while (rs.next()) {
                    Object id = rs.getObject(selectMapper.getResultSetModelIdColumnLabel());
                    if (id != null) {
                        Object relatedModel = selectMapper.buildModel(rs);
                        idToRelatedModelMap.put(id, relatedModel);
                    }
                }
                return null;
            }
        };

        // some databases have limits on number of entries in a 'IN' clause
        // Chunk the collection of propertyValues and make multiple calls as needed.
        Collection<List<?>> chunkedPropertyValues = MapperUtils.chunkTheCollection(queryParams, inClauseChunkSize);
        for (List<?> propValues : chunkedPropertyValues) {
            MapSqlParameterSource params = new MapSqlParameterSource("propertyValues", propValues);
            jtm.getNamedParameterJdbcTemplate().query(sql, params, rsExtractor);
        }

        for (BeanWrapper bw : bwMergeList) {
            // find the matching related model
            Object relatedModel = idToRelatedModelMap.get(bw.getPropertyValue(joinPropertyName));
            if (relatedModel != null) {
                bw.setPropertyValue(propertyName, relatedModel);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processHasMany(JdbcTemplateMapper jtm, List<T> mergeList, Class<?> ownerType, Class<?> relatedType) {

        TableMapping ownerTypeTableMapping = jtm.getMappingHelper().getTableMapping(ownerType);
        TableMapping relatedTypeTableMapping = jtm.getMappingHelper().getTableMapping(relatedType);
        String joinPropertyName = relatedTypeTableMapping.getPropertyName(joinColumn);
        String ownerTypeIdPropName = ownerTypeTableMapping.getIdPropertyName();

        Map<Object, BeanWrapper> idToOwnerModelMap = new HashMap<>();
        Set queryParams = new HashSet<>();
        for (Object obj : mergeList) {
            BeanWrapper bwOwnerModel = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            Object idValue = bwOwnerModel.getPropertyValue(ownerTypeIdPropName);
            queryParams.add(idValue);
            idToOwnerModelMap.put(idValue, bwOwnerModel);
        }

        if (MapperUtils.isEmpty(queryParams)) {
            return;
        }

        SelectMapper<?> selectMapper = jtm.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());
        String sql = "SELECT " + selectMapper.getColumnsSql() + " FROM "
                + jtm.getMappingHelper().fullyQualifiedTableName(relatedTypeTableMapping.getTableName()) + " WHERE "
                + joinColumn + " IN (:propertyValues)";

        ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                while (rs.next()) {
                    Object relatedModel = selectMapper.buildModel(rs);
                    BeanWrapper bwRelatedModel = PropertyAccessorFactory.forBeanPropertyAccess(relatedModel);
                    Object joinPropertyValue = bwRelatedModel.getPropertyValue(joinPropertyName);
                    BeanWrapper bwOwnerModel = idToOwnerModelMap.get(joinPropertyValue);
                    if (bwOwnerModel != null) {
                        // already validated so we know collection is initialized
                        Collection collection = (Collection) bwOwnerModel.getPropertyValue(propertyName);
                        collection.add(relatedModel);
                    }
                }
                return null;
            }
        };

        // some databases have limits on number of entries in a 'IN' clause
        // Chunk the collection of propertyValues and make multiple calls as needed.
        Collection<List<?>> chunkedPropertyValues = MapperUtils.chunkTheCollection(queryParams, inClauseChunkSize);
        for (List propValues : chunkedPropertyValues) {
            MapSqlParameterSource params = new MapSqlParameterSource("propertyValues", propValues);
            jtm.getNamedParameterJdbcTemplate().query(sql, params, rsExtractor);
        }
    }
}
