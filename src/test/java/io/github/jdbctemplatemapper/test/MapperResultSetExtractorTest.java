package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.MapperResultSetExtractor;
import io.github.jdbctemplatemapper.core.SelectMapper;
import io.github.jdbctemplatemapper.model.Order;
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
    public void mapperResultSetExtractor_Test() {
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