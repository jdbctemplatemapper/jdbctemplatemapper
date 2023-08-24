package io.github.jdbctemplatemapper.test;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.SelectMapper;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Product;
import io.github.jdbctemplatemapper.support.MapperResultSetExtractor;
import io.github.jdbctemplatemapper.support.MapperResultSetExtractorBuilder;

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
        //@formatter:on

        // MapperResultSetExtractor<Order> rsExtractor = new
        // MapperResultSetExtractor<Order>(Order.class,
        // orderSelectMapper, orderLineSelectMapper, productSelectMapper);

        // MapperResultSetExtractor<Order> rsExtractor = new
        // MapperResultSetExtractorBuilder<Order>(Order.class,
        // orderSelectMapper, orderLineSelectMapper, productSelectMapper)
        // .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
        // .relationship(OrderLine.class).hasOne(Product.class, "product")
        // .build();

      //@formatter:off   
        MapperResultSetExtractor<Order> rsExtractor = MapperResultSetExtractorBuilder
                .newMapperResultSetExtractorBuilder(Order.class, orderSelectMapper, orderLineSelectMapper,productSelectMapper)
                .relationship(Order.class).hasMany(OrderLine.class, "orderLines")
                .relationship(OrderLine.class).hasOne(Product.class, "product")
                .build();
      //@formatter:on
        
        List<Order> orders = jtm.getJdbcTemplate().query(sql, rsExtractor);

        System.out.println(orders.size());

    }

}