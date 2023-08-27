package io.github.jdbctemplatemapper.test;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryTest {

    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;

    //@Test
    public void find_hasMany_Test() {
      //@formatter:off
        List<Order> list = Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status DESC")
        .hasMany(OrderLine.class)
        .joinColumn("order_id")
        .populateProperty("orderLines")
        .execute(jtm); 
        
        
        System.out.println("size:" + list.size());
        
        for(Order o : list){
            System.out.println("OrderLine.size: " + o.getOrderLines().size());
        }
        
      //@formatter:on
    }

    @Test
    public void find_hasOne_Test() {
      //@formatter:off
        Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status DESC")
        .hasOne(Customer.class)
        .joinColumn("customer_id")
        .populateProperty("customer")
        .execute(jtm);
        
      //@formatter:on
    }

    // @Test
    public void find_typeOnly_Test() {
      //@formatter:off
        Query.type(Order.class)
        .execute(jtm);
        
      //@formatter:on
    }

}
