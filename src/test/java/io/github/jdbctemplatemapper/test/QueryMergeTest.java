package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.core.QueryMerge;
import io.github.jdbctemplatemapper.exception.QueryException;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Customer2;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.Order5;
import io.github.jdbctemplatemapper.model.Order6;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.OrderLine1;
import io.github.jdbctemplatemapper.model.Product;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryMergeTest {
    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;
    
    
    @Test
    public void hasOne_null_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasOne(null)
         .joinColumn("customer_id")
         .populateProperty("customer")
        .execute(jtm, orders);     
       });
       
        //@formatter:on
       assertTrue(exception.getMessage().contains("relatedType cannot be null"));
    }
    
    @Test
    public void hasMany_null_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasMany(null)
         .joinColumn("order_id")
         .populateProperty("orderLines")
        .execute(jtm, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("relatedType cannot be null"));
    }
    
    @Test
    public void hasOne_joinColumnNull_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasOne(Customer.class)
         .joinColumn(null)
         .populateProperty("customer")
        .execute(jtm, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("joinColumn cannot be null"));
    }
    
    @Test
    public void hasMany_joinColumnNull_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumn(null)
         .populateProperty("orderLines")
        .execute(jtm, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("joinColumn cannot be null"));
    }
    
    @Test
    public void hasOne_populatePropertyNull_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasOne(Customer.class)
         .joinColumn("customer_id")
         .populateProperty(null)
        .execute(jtm, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("propertyName cannot be null"));
    }
    
    @Test
    public void hasMany_populatePropertyNull_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumn("order_id")
         .populateProperty(null)
        .execute(jtm, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("propertyName cannot be null"));
    }
    
    @Test
    public void hasOne_jdbcTemplateMapperNull_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasOne(Customer.class)
         .joinColumn("customer_id")
         .populateProperty("customer")
        .execute(null, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("jdbcTemplateMapper cannot be null"));
    }
    
    @Test
    public void hasMany_jdbcTemplateMapperNull_test() {
        List<Order> orders = new ArrayList<>();
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         QueryMerge.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumn("order_id")
         .populateProperty("orderLines")
        .execute(null, orders);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("jdbcTemplateMapper cannot be null"));
    }
    
    
    @Test
    public void hasMany_joinTypeMismatch_test() {
        List<Order5> orders = new ArrayList<>();
        //@formatter:off
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        QueryMerge.type(Order5.class)
        .hasMany(OrderLine1.class)
        .joinColumn("order_id")
        .populateProperty("orderLines")
        .execute(jtm, orders);       
        });
        //@formatter:on     
        assertTrue(exception.getMessage()
                .contains("Property type mismatch."));
    }
    
    @Test
    public void hasOne_joinTypeMismatch_test() {
        List<Order6> orders = new ArrayList<>();
        //@formatter:off
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        QueryMerge.type(Order6.class)
        .hasOne(Customer2.class)
        .joinColumn("customer_id")
        .populateProperty("customer")
        .execute(jtm, orders);       
        });
        //@formatter:on     
        assertTrue(exception.getMessage()
                .contains("Property type mismatch."));
    }  
    
    
    @Test 
    public void hasOne_mergeListNull_success(){
     Assertions.assertDoesNotThrow(() -> {
        QueryMerge.type(Order.class)
        .hasOne(Customer.class)
        .joinColumn("customer_id")
        .populateProperty("customer")
        .execute(jtm, null);
      });
    }
    
    @Test 
    public void hasOne_mergeListEmpty_success(){
        
     List<Order> orders = new ArrayList<Order>();
     Assertions.assertDoesNotThrow(() -> {
        QueryMerge.type(Order.class)
        .hasOne(Customer.class)
        .joinColumn("customer_id")
        .populateProperty("customer")
        .execute(jtm, orders);
      });
     
     assertTrue(orders.size() == 0); 
     
    }
    
    @Test 
    public void hasMany_mergeListNull_success(){
     Assertions.assertDoesNotThrow(() -> {
        QueryMerge.type(Order.class)
        .hasMany(OrderLine.class)
        .joinColumn("order_id")
        .populateProperty("orderLines")
        .execute(jtm, null);
      });
    }
    
    @Test 
    public void hasMany_mergeListEmpty_success(){
        List<Order> orders = new ArrayList<Order>();
     Assertions.assertDoesNotThrow(() -> {
        QueryMerge.type(Order.class)
        .hasMany(OrderLine.class)
        .joinColumn("order_id")
        .populateProperty("orderLines")
        .execute(jtm, orders);
      });
     
     assertTrue(orders.size() == 0); 
     
    }
    
    
    @Test
    public void hasOne_success_test() {
        //@formatter:off
        List<Order> orders = Query.type(Order.class)
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
    
    
    @Test
    public void hasMany_success_test() {
        //@formatter:off
        List<Order> orders = Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status DESC")
        .hasOne(Customer.class)
        .joinColumn("customer_id")
        .populateProperty("customer")
        .execute(jtm);       
      //@formatter:on
        
      //@formatter:off
        QueryMerge.type(Order.class)
        .hasMany(OrderLine.class)
        .joinColumn("order_id")
        .populateProperty("orderLines")
        .execute(jtm, orders);       
      //@formatter:on
        
    }
    
}