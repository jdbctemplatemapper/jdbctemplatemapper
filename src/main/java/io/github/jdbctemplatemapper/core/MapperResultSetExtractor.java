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
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.exception.MapperExtractorException;

public class MapperResultSetExtractor<T>
        implements ResultSetExtractor<List<T>>, IMapperExtractionBuilder<T>, IRelationshipBuilder<T> {

    private Class<T> extractorType;
    private SelectMapper<?>[] selectMappers = {};
    private List<Relationship> relationships = new ArrayList<>();
    private Relationship tmpRelationship;

    private boolean fullyBuilt = false;

    private MapperResultSetExtractor(Class<T> extractorType, SelectMapper<?>... selectMappers) {
        this.extractorType = extractorType;
        this.selectMappers = selectMappers;
    }

    private MapperResultSetExtractor(Class<T> extractorType, List<Relationship> relationships,
            SelectMapper<?>... selectMappers) {
        Assert.notNull(extractorType, "extractorType must not be null");
        Assert.notEmpty(selectMappers, "selectMappers must not be null or empty. There should be at least one.");

        this.extractorType = extractorType;
        this.selectMappers = selectMappers;
        this.relationships = relationships;
        if (relationships != null) {
            for (Relationship relationship : relationships) {
                relationship.setSelectMapperMainClazz(getSelectMapper(relationship.getMainClazz()));
                relationship.setSelectMapperRelatedClazz(getSelectMapper(relationship.getRelatedClazz()));
            }
        }
        validate();
    }

    public static <T> IMapperExtractionBuilder<T> builder(Class<T> extractorType, SelectMapper<?>... selectMappers) {
        return new MapperResultSetExtractor<T>(extractorType, selectMappers);
    }

    public IRelationshipBuilder<T> relationship(Class<?> clazz) {
        this.tmpRelationship = new Relationship(clazz);
        return this;
    }

    public IMapperExtractionBuilder<T> hasMany(Class<?> relatedClazz, String propertyName) {
        tmpRelationship.setRelatedClazz(relatedClazz);
        tmpRelationship.setPropertyName(propertyName);
        tmpRelationship.setRelationshipType(RelationshipType.HAS_MANY);
        relationships.add(tmpRelationship);
        return this;
    }

    public IMapperExtractionBuilder<T> hasOne(Class<?> relatedClazz, String propertyName) {
        tmpRelationship.setRelatedClazz(relatedClazz);
        tmpRelationship.setPropertyName(propertyName);
        tmpRelationship.setRelationshipType(RelationshipType.HAS_ONE);
        relationships.add(tmpRelationship);
        return this;
    }

    public MapperResultSetExtractor<T> build() {
        MapperResultSetExtractor<T> obj = new MapperResultSetExtractor<T>(extractorType, relationships, selectMappers);
        obj.fullyBuilt = true;
        return obj;
    }

    
    @SuppressWarnings("unchecked")
    @Override
    public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (!fullyBuilt) {
            throw new MapperExtractorException(
                    "MapperResultSetExtractor was not fully built. Use MapperResultSetExtractor.builder()");
        }

        // key - SelectMapper type.
        // value - a Map, key: id of model, value: model
        Map<Class<?>, Map<Object, Object>> smIdToModelMap = new HashMap<>();

        // initialize all select mappers idToModel map
        for (SelectMapper<?> selectMapper : selectMappers) {
            smIdToModelMap.put(getSelectMapperKey(selectMapper), new HashMap<>());
        }

        while (rs.next()) {
            if (MapperUtils.isEmpty(relationships)) {
                // just get the extractorType model
                getModel(rs, getSelectMapper(extractorType),
                        smIdToModelMap.get(getSelectMapperKey(getSelectMapper(extractorType))));
            } else {
                for (Relationship relationship : relationships) {
                    SelectMapper<?> smMainClazz = relationship.getSelectMapperMainClazz();
                    Object mainModel = getModel(rs, smMainClazz, smIdToModelMap.get(getSelectMapperKey(smMainClazz)));
                    
                    SelectMapper<?> smRelatedClazz = relationship.getSelectMapperRelatedClazz();
                    Object relatedModel = getModel(rs, smRelatedClazz,
                            smIdToModelMap.get(getSelectMapperKey(smRelatedClazz)));

                    if (RelationshipType.HAS_ONE == relationship.getRelationshipType()) {
                        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                        bw.setPropertyValue(relationship.getPropertyName(), relatedModel);
                    }

                    if (RelationshipType.HAS_MANY == relationship.getRelationshipType()) {
                        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                        // the property has already been validated by builder() so we know it is a collection
                        // that has been initialized
                        @SuppressWarnings("rawtypes")
                        Collection collection = (Collection) bw.getPropertyValue(relationship.getPropertyName());
                        collection.add(relatedModel);                       
                    }
                }
            }
        }
        Map<Object, Object> map = smIdToModelMap.get(getSelectMapperKey(getSelectMapper(extractorType)));
        return (List<T>) new ArrayList<>(map.values());
    }

    private Class<?> getSelectMapperKey(SelectMapper<?> selectMapper) {
        return selectMapper.getType();
    }

    private SelectMapper<?> getSelectMapper(Class<?> clazz) {
        for (SelectMapper<?> selectMapper : selectMappers) {
            if (selectMapper.getType() == clazz) {
                return selectMapper;
            }
        }
        return null;
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

    private void validate() {
        checkSelectMappersNotNull();

        // check there is a SelectMapper for extractorType
        if (getSelectMapper(extractorType) == null) {
            throw new MapperExtractorException(
                    "Could not find a SelectMapper for extractorType " + extractorType.getName());
        }

        // make sure relationship classes have selectMappers
        if (MapperUtils.isNotEmpty(relationships)) {
            for (Relationship relationship : relationships) {
                if (relationship.getSelectMapperMainClazz() == null) {
                    throw new MapperExtractorException(
                            "Could not find a SelectMapper for class " + relationship.getMainClazz().getName());
                }
                if (relationship.getSelectMapperRelatedClazz() == null) {
                    throw new MapperExtractorException("Could not find a SelectMapper for related class "
                            + relationship.getRelatedClazz().getName());
                }

                Object mainModel = null;
                try {
                    mainModel = relationship.getMainClazz().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                if (!bw.isReadableProperty(relationship.getPropertyName())) {
                    throw new MapperExtractorException("Invalid property name " + relationship.getPropertyName()
                            + " for class " + relationship.getMainClazz());
                }

                if (relationship.getRelationshipType() == RelationshipType.HAS_MANY) {
                    PropertyDescriptor pd = bw.getPropertyDescriptor(relationship.getPropertyName());
                    if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
                        throw new MapperExtractorException("property " + relationship.getMainClazz().getName() + "."
                                + relationship.getPropertyName()
                                + " is not a collection. hasMany() relationship requires it to be a collection");
                    }

                    Class<?> type = getGenericTypeOfCollection(mainModel, relationship.getPropertyName());
                    if (type == null) {
                        throw new MapperExtractorException(
                                "Collections which do not have generic type are not supported"
                                        + relationship.getMainClazz().getName() + "." + relationship.getPropertyName());

                    }
                    if (!type.isAssignableFrom(relationship.getRelatedClazz())) {
                        throw new MapperExtractorException("Collection generic type mismatch "
                                + relationship.getMainClazz().getName() + "." + relationship.getPropertyName()
                                + " has genric type " + type.getName() + " while the related class is of type "
                                + relationship.getRelatedClazz().getName());
                    }

                    Object value = bw.getPropertyValue(relationship.getPropertyName());
                    if (value == null) {
                        throw new MapperExtractorException("Collection property "
                                + relationship.getMainClazz().getName() + "." + relationship.getPropertyName()
                                + " has to initialized. MapperResultSetExtractor only works with initialized collections");
                    }

                } else if (relationship.getRelationshipType() == RelationshipType.HAS_ONE) {
                    PropertyDescriptor pd = bw.getPropertyDescriptor(relationship.getPropertyName());
                    if (!relationship.getRelatedClazz().isAssignableFrom(pd.getPropertyType())) {
                        throw new MapperExtractorException(
                                "property type conflict. property " + relationship.getMainClazz() + "."
                                        + relationship.getPropertyName() + " is of type " + pd.getPropertyType()
                                        + " while type for hasOne relationship is " + relationship.getRelatedClazz());
                    }
                }
            }
        }

        checkDuplicateRelationships();
        checkDuplicateSelectMappers();
    }

    // dont allow duplicate relationships with same combination of main and related
    // classes.
    void checkDuplicateRelationships() {
        List<String> seen = new ArrayList<>();
        for (Relationship r : relationships) {
            String str = r.getMainClazz().getName() + "-" + r.getRelatedClazz().getName();
            if (seen.contains(str)) {
                throw new MapperExtractorException("there are duplicate relationships between "
                        + r.getMainClazz().getName() + " and " + r.getRelatedClazz().getName());
            } else {
                seen.add(str);
            }
        }
    }

    // dont allow duplicate selectMappers ie selectMappers of same class
    void checkDuplicateSelectMappers() {
        List<Class<?>> seen = new ArrayList<>();
        for (SelectMapper<?> sm : selectMappers) {
            if (seen.contains(sm.getType())) {
                throw new MapperExtractorException("duplicate selectMapper for type " + sm.getType().getName());
            } else {
                seen.add(sm.getType());
            }
        }
    }

    // a select mapper cannot be null
    void checkSelectMappersNotNull() {
        for (SelectMapper<?> sm : selectMappers) {
            if (sm == null) {
                throw new MapperExtractorException("At least one selectMapper was null");
            }
        }
    }
    
}
