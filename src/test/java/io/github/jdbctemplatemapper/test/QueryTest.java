package io.github.jdbctemplatemapper.test;

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
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.exception.QueryException;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.InvalidTableObject;
import io.github.jdbctemplatemapper.model.NoTableAnnotationModel;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.Order2;
import io.github.jdbctemplatemapper.model.Order4;
import io.github.jdbctemplatemapper.model.OrderLine;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryTest {

    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;

    @Test
    public void invalidType_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
         Query.type(InvalidTableObject.class)
        .execute(jtm);     
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Could not find table"));
    }
    
    @Test
    public void hasMany_invalidClass_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
            Query.type(Order.class)
                    .hasMany(NoTableAnnotationModel.class)
                    .joinColumn("order_id")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("does not have the @Table annotation"));
    }
    
    @Test
    public void hasOne_invalidClass_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
            Query.type(Order.class)
                    .hasOne(NoTableAnnotationModel.class)
                    .joinColumn("order_id")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("does not have the @Table annotation"));
    }
    
    @Test
    public void hasMany_invalidJoinColumn_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasMany(OrderLine.class)
                    .joinColumn("x")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Invalid join column"));
    }
    
    
    @Test
    public void hasOne_invalidJoinColumn_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasOne(Customer.class)
                    .joinColumn("x")
                    .populateProperty("customer")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Invalid join column"));
    }
    

    @Test
    public void hasMany_populatePropertyNotACollection_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasMany(OrderLine.class)
                    .joinColumn("order_id")
                    .populateProperty("customer")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("is not a collection"));
    } 
    
    @Test
    public void hasMany_populatePropertyCollectionHasNoGenericType_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order2.class)
                    .hasMany(OrderLine.class)
                    .joinColumn("order_id")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("Collections without generic types are not supported"));
    } 
    
    @Test
    public void hasMany_populatePropertyCollectionTypeMismatch_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasMany(Customer.class)
                    .joinColumn("customer_id")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("Collection generic type and hasMany relationship type mismatch"));
    } 
    
   @Test
    public void hasOne_populatePropertyTypeConflict_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasOne(Customer.class)
                    .joinColumn("customer_id")
                    .populateProperty("status")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("property type conflict"));
    }
    
    
    @Test
    public void hasMany_List_success_test() {
      //@formatter:off
        List<Order> list = Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status DESC")
        .hasMany(OrderLine.class) 
        .joinColumn("order_id")
        .populateProperty("orderLines") // list
        .execute(jtm); 
        
      //@formatter:on
    }
    
    @Test
    public void hasMany_Set_success_test() {
      //@formatter:off
        List<Order4> list = Query.type(Order4.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status DESC")
        .hasMany(OrderLine.class)
        .joinColumn("order_id")
        .populateProperty("orderLines") //set
        .execute(jtm); 
        
      //@formatter:on
    }

    @Test
    public void hasOne_success_test() {
      //@formatter:off
        Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.status    DESC")
        .hasOne(Customer.class)
        .joinColumn("customer_id")
        .populateProperty("customer")
        .execute(jtm);
        
      //@formatter:on
    }

    @Test
    public void typeOnly_success_test() {
      //@formatter:off
        Query.type(Order.class)
        .execute(jtm);
        
      //@formatter:on
    }

}
