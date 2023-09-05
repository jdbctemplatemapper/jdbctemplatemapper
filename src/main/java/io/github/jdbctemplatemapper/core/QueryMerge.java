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
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumnManySide;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeJoinColumnOwningSide;
import io.github.jdbctemplatemapper.querymerge.IQueryMergePopulateProperty;
import io.github.jdbctemplatemapper.querymerge.IQueryMergeType;

/**
 * QueryMerge allows query results to be merged with results of another query
 * 
 * <pre>
 * Query api allows only one relationship to be queried. QueryMerge allows
 * separate query results to be merged
 *
 * @author ajoseph
 */
public class QueryMerge<T> implements IQueryMergeFluent<T> {
    private int inClauseChunkSize = 100;
    private Class<T> ownerType;
    private RelationshipType relationshipType;
    private Class<?> relatedType;
    private String propertyName; // propertyName on ownerType that needs to be populated
    private String joinColumnOwningSide;
    private String joinColumnManySide;

    private QueryMerge(Class<T> type) {
        this.ownerType = type;
    }

    /**
     * The type being merged with the new query. The execute method will populate a
     * list of this type with the relationship
     * 
     * @param type The type
     * @return interface with the next methods in the chain
     */
    public static <T> IQueryMergeType<T> type(Class<T> type) {
        Assert.notNull(type, "Type cannot be null");
        return new QueryMerge<T>(type);
    }

    /**
     * hasOne relationship
     *
     * @param hasOne related type
     * 
     * @return interface with the next methods in the chain
     */
    public IQueryMergeHasOne<T> hasOne(Class<?> relatedType) {
        Assert.notNull(relatedType, "relatedType cannot be null");
        this.relationshipType = RelationshipType.HAS_ONE;
        this.relatedType = relatedType;
        return this;
    }

    /**
     * hasMany relationship
     *
     * @param hasMany related type
     * 
     * @return interface with the next methods in the chain
     */
    public IQueryMergeHasMany<T> hasMany(Class<?> relatedType) {
        Assert.notNull(relatedType, "relatedType cannot be null");
        this.relationshipType = RelationshipType.HAS_MANY;
        this.relatedType = relatedType;
        return this;
    }

    /**
     * Join column for hasOne relationship. The join column (the foreign key) is in
     * the table of the owning model. Example: Order hasOne Customer. The join
     * column(foreign key) will be on the table order (of the owning model)
     * 
     * The join column should not have a table prefix.
     *
     * @param joinColumnOwningSide the join column on the owning side (with no table
     *                             prefix)
     * 
     * @return interface with the next methods in the chain
     */
    public IQueryMergeJoinColumnOwningSide<T> joinColumnOwningSide(String joinColumnOwningSide) {
        Assert.notNull(joinColumnOwningSide, "joinColumnOwningSide cannot be null");
        this.joinColumnOwningSide = MapperUtils.toLowerCase(joinColumnOwningSide.trim());
        return this;
    }

    /**
     * Join column for hasMany relationship: The join column (the foreign key) is in
     * the table of the many side. Example: Order hasMany OrderLine. The join column
     * will be on the table order_line (the many side)
     *
     * The join column should not have a table prefix.
     *
     * @param joinColumnManySide the join column on the many side (with no table
     *                           prefix)
     * 
     * @return interface with the next methods in the chain
     */
    public IQueryMergeJoinColumnManySide<T> joinColumnManySide(String joinColumnManySide) {
        Assert.notNull(joinColumnManySide, "joinColumnManySide cannot be null");
        this.joinColumnManySide = MapperUtils.toLowerCase(joinColumnManySide.trim());
        return this;
    }

    /**
     * The relationship property that needs to be populated on the owning type.
     * <p>
     * For hasMany() this property has to be an initialized collection (cannot be
     * null) and the generic type should match the hasMany related type.
     * <p>
     * For hasOne this property has to be the same type as hasOne related type
     * 
     * @param propertyName name of property that needs to be populated
     * @return interface with the next methods in the chain
     */
    public IQueryMergePopulateProperty<T> populateProperty(String propertyName) {
        Assert.notNull(propertyName, "propertyName cannot be null");
        this.propertyName = propertyName;
        return this;
    }

    /**
     * The query executes an sql 'IN' clause to get the related side objects and
     * merges those with the objects in the mergeList
     * <pre>
     * If the 'IN' clause gets large (ie the mergeList is large) multiple 'IN'
     * clause queries will be issued to get the records since some databases have
     * limits on size of 'IN' clause.
     * 
     * @param jdbcTemplateMapper the jdbcTemplateMapper
     * @param mergeList          a list of objects of owning type.
     * @return List<T> a list of the owning type with its relationship property
     *         populated
     */

    public void execute(JdbcTemplateMapper jdbcTemplateMapper, List<T> mergeList) {
        Assert.notNull(jdbcTemplateMapper, "jdbcTemplateMapper cannot be null");

        QueryValidator.validate(jdbcTemplateMapper, ownerType, relationshipType, relatedType, joinColumnOwningSide,
                joinColumnManySide, propertyName, null, null, null);

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
        String joinPropertyName = ownerTypeTableMapping.getPropertyName(joinColumnOwningSide);

        List<BeanWrapper> bwMergeList = new ArrayList<>(); // used to avoid excessive BeanWrapper creation
        Set params = new HashSet<>();
        for (T obj : mergeList) {
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            Object joinPropertyValue = bw.getPropertyValue(joinPropertyName);
            if (joinPropertyValue != null) {
                params.add(joinPropertyValue);
                bwMergeList.add(bw);
            }
        }

        if (MapperUtils.isEmpty(params)) {
            return;
        }

        SelectMapper<?> selectMapper = jtm.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());
        String sql = "SELECT " + selectMapper.getColumnsSql() + " FROM "
                + jtm.getMappingHelper().fullyQualifiedTableName(relatedTypeTableMapping.getTableName()) + " WHERE "
                + relatedTypeTableMapping.getIdColumnName() + " IN (:joinPropertyOwningSideValues)";

        String relatedModelIdPropName = relatedTypeTableMapping.getIdPropertyName();
        Map<Object, Object> idToRelatedModelMap = new HashMap<>();
        ResultSetExtractor<List<T>> rsExtractor = new ResultSetExtractor<List<T>>() {
            public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
                while (rs.next()) {
                    Object relatedModel = selectMapper.buildModel(rs);
                    if (relatedModel != null) {
                        BeanWrapper bwRelatedModel = PropertyAccessorFactory.forBeanPropertyAccess(relatedModel);
                        idToRelatedModelMap.put(bwRelatedModel.getPropertyValue(relatedModelIdPropName), relatedModel);
                    }
                }
                return null;
            }
        };

        // some databases have limits on number of entries in a 'IN' clause
        // Chunk the collection of propertyValues and make multiple calls as needed.
        Collection<List<?>> chunkedJoinPropertyOwningSideValues = MapperUtils.chunkTheCollection(params,
                inClauseChunkSize);
        for (List<?> joinPropertyOwningSideValues : chunkedJoinPropertyOwningSideValues) {
            MapSqlParameterSource queryParams = new MapSqlParameterSource("joinPropertyOwningSideValues",
                    joinPropertyOwningSideValues);
            jtm.getNamedParameterJdbcTemplate().query(sql, queryParams, rsExtractor);
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
        String joinPropertyName = relatedTypeTableMapping.getPropertyName(joinColumnManySide);
        String ownerTypeIdPropName = ownerTypeTableMapping.getIdPropertyName();

        Map<Object, BeanWrapper> idToOwnerModelMap = new HashMap<>();
        Set params = new HashSet<>();
        for (Object obj : mergeList) {
            BeanWrapper bwOwnerModel = PropertyAccessorFactory.forBeanPropertyAccess(obj);
            Object idValue = bwOwnerModel.getPropertyValue(ownerTypeIdPropName);
            if (idValue != null) {
                params.add(idValue);
                idToOwnerModelMap.put(idValue, bwOwnerModel);
            }
        }

        if (MapperUtils.isEmpty(params)) {
            return;
        }

        SelectMapper<?> selectMapper = jtm.getSelectMapper(relatedType, relatedTypeTableMapping.getTableName());
        String sql = "SELECT " + selectMapper.getColumnsSql() + " FROM "
                + jtm.getMappingHelper().fullyQualifiedTableName(relatedTypeTableMapping.getTableName()) + " WHERE "
                + joinColumnManySide + " IN (:ownerTypeIds)";

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
        Collection<List<?>> chunkedOwnerTypeIds = MapperUtils.chunkTheCollection(params, inClauseChunkSize);
        for (List ownerTypeIds : chunkedOwnerTypeIds) {
            MapSqlParameterSource queryParams = new MapSqlParameterSource("ownerTypeIds", ownerTypeIds);
            jtm.getNamedParameterJdbcTemplate().query(sql, queryParams, rsExtractor);
        }
    }
}
