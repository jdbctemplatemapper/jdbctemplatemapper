package io.github.jdbctemplatemapper.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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

import io.github.jdbctemplatemapper.core.SelectMapper;
import io.github.jdbctemplatemapper.exception.MapperException;

public class MapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {

    Class<T> rootClazz;
    SelectMapper<?>[] selectMappers = {};
    List<Relationship> relationships = new ArrayList<>();
   

    public MapperResultSetExtractor(Class<T> rootClazz, List<Relationship> relationships, SelectMapper<?>... selectMappers) {
        this.rootClazz = rootClazz;

        this.relationships = relationships;
        this.selectMappers = selectMappers;
        
        for(Relationship relationship : relationships) {
            relationship.setSelectMapperMainClazz(getSelectMapper(relationship.getMainClazz()));
            relationship.setSelectMapperRelatedClazz(getSelectMapper(relationship.getRelatedClazz()));
        }
        

        //Relationship rel1 = new Relationship(Order.class, RelationshipType.HAS_MANY, OrderLine.class, "orderLines");
        //rel1.setSelectMapperMainClazz(getSelectMapper(Order.class));
        //rel1.setSelectMapperRelatedClazz(getSelectMapper(OrderLine.class));
        //relationships.add(rel1);

        //Relationship rel2 = new Relationship(OrderLine.class, RelationshipType.HAS_ONE, Product.class, "product");
        //rel2.setSelectMapperMainClazz(getSelectMapper(OrderLine.class));
        //rel2.setSelectMapperRelatedClazz(getSelectMapper(Product.class));
        //relationships.add(rel2);

    }
    

    // public MapperResultSetExtractor(MapperResultSetExtractorBuilder<T> builder) {
    // }

    @Override
    public List<T> extractData(ResultSet rs) throws SQLException, DataAccessException {

        // key - SelectMapper type.
        // value - a Map, key: id of model, value: model
        Map<Class<?>, Map<Object, Object>> smIdToModelMap = new HashMap<>();

        // initialize all select mappers idToModel map
        for (SelectMapper<?> selectMapper : selectMappers) {
            smIdToModelMap.put(getSelectMapperKey(selectMapper), new HashMap<>());
        }

        while (rs.next()) {

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
                    Object value = bw.getPropertyValue(relationship.getPropertyName());
                    if (value == null) {
                        
                        PropertyDescriptor pd = bw.getPropertyDescriptor(relationship.getPropertyName());
                        if (List.class == pd.getPropertyType() || ArrayList.class == pd.getPropertyType()) {
                            Class<?> type = getGenericTypeOfCollection(mainModel, relationship.getPropertyName());
                            List<?> list = createArrayList(type);
                            bw.setPropertyValue(relationship.getPropertyName(), list);
                            bw.setPropertyValue(relationship.getPropertyName() + "[0]", relatedModel);
                        }
                        else if (Set.class == pd.getPropertyType() || HashSet.class == pd.getPropertyType()) {
                            Class<?> type = getGenericTypeOfCollection(mainModel, relationship.getPropertyName());
                            Set<?> set = createHashSet(type);
                            bw.setPropertyValue(relationship.getPropertyName(), set);
                            bw.setPropertyValue(relationship.getPropertyName() + "[0]", relatedModel);
                        }
                        //
                        
                        
                    } else {
                        if (value instanceof Collection<?>) {
                            int size = ((Collection<?>) value).size();
                            bw.setPropertyValue(relationship.getPropertyName() + "[" + size + "]", relatedModel);
                        }
                    }

                }
            }
        }

        Map<Object, Object> map = smIdToModelMap.get(getSelectMapperKey(getSelectMapper(rootClazz)));

        return (List<T>) new ArrayList(map.values());
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
        throw new MapperException("No select mapper for clazz " + clazz.getName());
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
            } else {
                throw new RuntimeException("Could not find generic type for property: " + propertyName + " in class: "
                        + mainObj.getClass().getSimpleName());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> List<?> createArrayList(Class<?> clazz) {
        return new ArrayList<T>();
    }

    private <T> Set<?> createHashSet(Class<?> clazz) {
        return new HashSet<T>();
    }

}
