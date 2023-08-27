package io.github.jdbctemplatemapper.test;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.core.QueryMerge;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Product;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryMergeTest {
    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;
    
    @Test
    public void merge_hasOne_Test() {
        //@formatter:off
        List<Order> orders = Query.find(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status DESC")
        .hasMany(OrderLine.class)
        .joinColumn("order_id")
        .populateProperty("orderLines")
        .execute(jtm);       
      //@formatter:on
        
        List<OrderLine> flatOrderLines = orders.stream()
        .map(x -> x.getOrderLines())
        .flatMap(list -> list.stream())
        .collect(Collectors.toList());
        
      //@formatter:off
        QueryMerge.type(OrderLine.class)
        .hasOne(Product.class)
        .joinColumn("product_id")
        .populateProperty("product")
        .execute(jtm, flatOrderLines);       
      //@formatter:on
    }
    
}