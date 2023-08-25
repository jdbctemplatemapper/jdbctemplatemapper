package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.MapperResultSetExtractor;
import io.github.jdbctemplatemapper.core.SelectMapper;
import io.github.jdbctemplatemapper.exception.MapperExtractorException;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.Order2;
import io.github.jdbctemplatemapper.model.Order3;
import io.github.jdbctemplatemapper.model.Order4;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Product;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MapperResultSetExtractorTest {

    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;

    @Test
    public void extractor_noMapperForExtractorType_Test() {
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderLineSelectMapper,productSelectMapper)// no selectMapper for Order.class
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
        //@formatter:on

        assertTrue(exception.getMessage().contains("Could not find a SelectMapper for extractorType "));

    }

    @Test
    public void extractor_noMapperForMainObject_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                .builder(Order.class, orderSelectMapper, productSelectMapper)// no orderLineSelectMapper
                .relationship(OrderLine.class).hasOne(Product.class, "product")
                .relationship(Order.class).hasMany(OrderLine.class, "orderLines")  
                .build();
        });
        //@formatter:on  

        assertTrue(exception.getMessage().contains("Could not find a SelectMapper for class"));

    }

    @Test
    public void extractor_noMapperForRelatedClass_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper)// no selectMapper for product.class
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Could not find a SelectMapper for related class"));

    }

    @Test
    public void extractor_invalidPropertyName_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper)
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLinesX")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Invalid property name "));

    }

    @Test
    public void extractor_hasManyPropertyNotACollection_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper)
                    .relationship(Order.class).hasMany(OrderLine.class, "status")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
        //@formatter:on
        assertTrue(exception.getMessage()
                .contains("is not a collection. hasMany() relationship requires it to be a collection"));

    }

    @Test
    public void extractor_hasOnePropertyTypeConflict_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");
        SelectMapper<Customer> customerSelectMapper = jtm.getSelectMapper(Customer.class, "c");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper, customerSelectMapper)
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Customer.class, "product")
                    .build();  
        });
        //@formatter:on
        assertTrue(exception.getMessage().contains("property type conflict."));
    }

    @Test
    public void extractor_duplicateRelationships_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper)
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
        
        //@formatter:on
        assertTrue(exception.getMessage().contains("there are duplicate relationships between"));

    }

    @Test
    public void extractor_duplicateSelectMappers_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper, productSelectMapper)
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
        //@formatter:on
        assertTrue(exception.getMessage().contains("duplicate selectMapper for type"));
    }
    
    @Test
    public void extractor_selectMapperNotNull_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper, productSelectMapper, null)
                    .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
           
        //@formatter:on
        assertTrue(exception.getMessage().contains("At least one selectMapper was null"));
    }
    
    @Test
    public void extractor_CollectionHasNoGenericType_Test() {
        //Order2 has collection does not have generic type
        SelectMapper<Order2> orderSelectMapper = jtm.getSelectMapper(Order2.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order2.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper, productSelectMapper)
                    .relationship(Order2.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
           
        //@formatter:on
        assertTrue(exception.getMessage().contains("Collections without generic types are not supported"));
    }
    
    @Test
    public void extractor_CollectionGenericMismatch_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper, productSelectMapper)
                    .relationship(Order.class).hasMany(Product.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
           
        //@formatter:on
        assertTrue(exception.getMessage().contains("Collection generic type mismatch"));
    }
    
    @Test
    public void extractor_CollectionHasToBeInitialized_Test() {
        SelectMapper<Order3> orderSelectMapper = jtm.getSelectMapper(Order3.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
           MapperResultSetExtractor
                    .builder(Order3.class, orderSelectMapper, orderLineSelectMapper, productSelectMapper, productSelectMapper)
                    .relationship(Order3.class).hasMany(OrderLine.class, "orderLines")
                    .relationship(OrderLine.class).hasOne(Product.class, "product")
                    .build();  
        });
           
        //@formatter:on
        assertTrue(exception.getMessage().contains("MapperResultSetExtractor only works with initialized collections."));
    }
    

    @Test
    public void extractor_FullyBuilt_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        
        String sql = "select" 
                + orderSelectMapper.getColumnsSql() 
                + "," 
                + orderLineSelectMapper.getColumnsSql() 
                + ","
                + productSelectMapper.getColumnsSql() 
                + " from orders o"
                + " left join order_line ol on o.order_id = ol.order_id"
                + " join product p on p.product_id = ol.product_id" 
                + " order by o.order_id, ol.order_line_id";  
  
        MapperResultSetExtractor<Order> resultSetExtractor = (MapperResultSetExtractor<Order>)MapperResultSetExtractor
                .builder(Order.class, orderSelectMapper, orderLineSelectMapper,productSelectMapper);
        
        //@formatter:on
        Exception exception = Assertions.assertThrows(MapperExtractorException.class, () -> {
            jtm.getJdbcTemplate().query(sql, resultSetExtractor);
        });

        assertTrue(exception.getMessage().contains("MapperResultSetExtractor was not fully built"));
    }

    @Test
    public void mapperResultSetExtractor_NoRelationship_Success_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");

        //@formatter:off  
        
        String sql = "select" 
                + orderSelectMapper.getColumnsSql() 
                + " from orders o";
     
  
        MapperResultSetExtractor<Order> resultSetExtractor = MapperResultSetExtractor
                .builder(Order.class, orderSelectMapper)
                .build();
        
        //@formatter:on

        List<Order> orders = jtm.getJdbcTemplate().query(sql, resultSetExtractor);
        assertTrue(orders.size() == 3);
    }
    
    @Test
    public void mapperResultSetExtractor_Set_Success_Test() {
        SelectMapper<Order4> orderSelectMapper = jtm.getSelectMapper(Order4.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        
        String sql = "select" 
                + orderSelectMapper.getColumnsSql() 
                + "," 
                + orderLineSelectMapper.getColumnsSql() 
                + ","
                + productSelectMapper.getColumnsSql() 
                + " from orders o"
                + " left join order_line ol on o.order_id = ol.order_id"
                + " join product p on p.product_id = ol.product_id" 
                + " order by o.order_id, ol.order_line_id";  
  
        MapperResultSetExtractor<Order4> resultSetExtractor = MapperResultSetExtractor
                .builder(Order4.class, orderSelectMapper, orderLineSelectMapper,productSelectMapper)
                .relationship(Order4.class).hasMany(OrderLine.class, "orderLines")
                .relationship(OrderLine.class).hasOne(Product.class, "product")
                .build();
        
        //@formatter:on

        List<Order4> orders = jtm.getJdbcTemplate().query(sql, resultSetExtractor);

        assertTrue(orders.size() == 2);
        assertTrue(orders.get(0).getOrderLines().size() == 2);
        assertEquals("IN PROCESS", orders.get(1).getStatus());
        assertTrue(orders.get(1).getOrderLines().size() == 1);
 
    }

    @Test
    public void mapperResultSetExtractor_List_Success_Test() {
        SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
        SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
        SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

        //@formatter:off  
        
        String sql = "select" 
                + orderSelectMapper.getColumnsSql() 
                + "," 
                + orderLineSelectMapper.getColumnsSql() 
                + ","
                + productSelectMapper.getColumnsSql() 
                + " from orders o"
                + " left join order_line ol on o.order_id = ol.order_id"
                + " join product p on p.product_id = ol.product_id" 
                + " order by o.order_id, ol.order_line_id";  
  
        MapperResultSetExtractor<Order> resultSetExtractor = MapperResultSetExtractor
                .builder(Order.class, orderSelectMapper, orderLineSelectMapper,productSelectMapper)
                .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                .relationship(OrderLine.class).hasOne(Product.class, "product")
                .build();
        
        //@formatter:on

        List<Order> orders = jtm.getJdbcTemplate().query(sql, resultSetExtractor);

        assertTrue(orders.size() == 2);
        assertTrue(orders.get(0).getOrderLines().size() == 2);
        assertEquals("IN PROCESS", orders.get(1).getStatus());
        assertTrue(orders.get(1).getOrderLines().size() == 1);
        assertTrue(orders.get(0).getOrderLines().get(0).getProductId() == 1);
        assertEquals("shoes", orders.get(0).getOrderLines().get(0).getProduct().getName());
        assertEquals("socks", orders.get(0).getOrderLines().get(1).getProduct().getName());
        assertEquals("laces", orders.get(1).getOrderLines().get(0).getProduct().getName());
    }

}