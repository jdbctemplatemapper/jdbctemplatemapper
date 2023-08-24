package io.github.jdbctemplatemapper.support;

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

import io.github.jdbctemplatemapper.core.SelectMapper;
import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Product;

public class MapperResultSetExtractor<T> implements ResultSetExtractor<T> {

    
    Class<?> rootClazz;
    SelectMapper<?>[] selectMappers = {};
    List<Relationship> relationships = new ArrayList<>();

    public MapperResultSetExtractor(SelectMapper<?>... selectMappers) {
        this.rootClazz = Order.class;
        
        this.selectMappers = selectMappers;
        
        Relationship rel1 = new Relationship(Order.class, RelationshipType.HAS_MANY, OrderLine.class, "orderLines");
        rel1.setSelectMapperMainClazz(getSelectMapper(Order.class));
        rel1.setSelectMapperRelatedClazz(getSelectMapper(OrderLine.class));
        relationships.add(rel1);
        
        Relationship rel2 = new Relationship(OrderLine.class, RelationshipType.HAS_ONE, Product.class, "product");
        rel2.setSelectMapperMainClazz(getSelectMapper(OrderLine.class));
        rel2.setSelectMapperRelatedClazz(getSelectMapper(Product.class));
        relationships.add(rel2);
 

    }

    public MapperResultSetExtractor(MapperResultSetExtractorBuilder<T> builder) {
    }

    @Override
    public T extractData(ResultSet rs) throws SQLException, DataAccessException {

        Map<String, Map<Object, Object>> smIdToModelMap = new HashMap<>();

        for (SelectMapper<?> selectMapper : selectMappers) {
            smIdToModelMap.put(getSelectMapperKey(selectMapper), new HashMap<>());
        }

        while (rs.next()) {
            
            for (Relationship relationship : relationships) {
                SelectMapper<?> smMainClazz = relationship.getSelectMapperMainClazz();
                
                Object mainModel = getModel(rs, smMainClazz, smIdToModelMap.get(getSelectMapperKey(smMainClazz)));
                             
                SelectMapper<?> smRelatedClazz = relationship.getSelectMapperRelatedClazz();
                
                Object relatedModel = getModel(rs, smRelatedClazz, smIdToModelMap.get(getSelectMapperKey(smRelatedClazz)));
      
                if(RelationshipType.HAS_ONE == relationship.getRelationshipType()) {
                    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                    bw.setPropertyValue(relationship.getPropertyName(), relatedModel);
                }
                
                if(RelationshipType.HAS_MANY == relationship.getRelationshipType()) {
                    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
                    Object value = bw.getPropertyValue(relationship.getPropertyName());
                    if (value instanceof Collection<?>){
                       int size = ((Collection) value).size();
                       bw.setPropertyValue(relationship.getPropertyName()+"[" + size + "]", relatedModel);
                    }
                }               
            }  
        }
        
        
         Map<Object, Object> map = smIdToModelMap.get(getSelectMapperKey(selectMappers[0]));
         
         for(Object obj : map.values()) {
             System.out.println("Object class name " + obj.getClass().getSimpleName());
         }
         
         return null;
         
    }

    private String getSelectMapperKey(SelectMapper<?> selectMapper) {
        return selectMapper.getType().getSimpleName() + "-" + selectMapper.getTableAlias();
    }
    
    private SelectMapper<?> getSelectMapper(Class<?> clazz){
    for (SelectMapper<?> selectMapper : selectMappers) {
        if(selectMapper.getType() == clazz) {
            return selectMapper;
        }
    }
    throw new MapperException("No select mapper for clazz " + clazz.getName());
    }
    
    @SuppressWarnings("unchecked")
    public Object getModel(ResultSet rs, SelectMapper<?> selectMapper, Map<Object, Object> idToModelMap) throws SQLException{
       Object id = rs.getObject(selectMapper.getResultSetModelIdColumnLabel());     
        Object model = idToModelMap.get(id);
        if (model == null) {
            model = selectMapper.buildModel(rs); // builds the model from resultSet
            idToModelMap.put(id, model);
         }
         return model;      
     }
}
