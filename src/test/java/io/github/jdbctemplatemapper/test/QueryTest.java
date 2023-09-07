package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.github.jdbctemplatemapper.model.Customer7;
import io.github.jdbctemplatemapper.model.InvalidTableObject;
import io.github.jdbctemplatemapper.model.NoTableAnnotationModel;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.Order2;
import io.github.jdbctemplatemapper.model.Order4;
import io.github.jdbctemplatemapper.model.Order7;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.OrderLine7;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryTest {
    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;

   
    @Test
    public void type_null_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(null)
        .execute(jtm);     
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Type cannot be null"));
    }
    
    @Test
    public void where_null_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .where(null)
        .execute(jtm);     
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("whereClause cannot be null"));
    }
    
    @Test
    public void orderBy_null_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .orderBy(null)
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("orderBy cannot be null"));
    }
    
    @Test
    public void hasOne_null_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasOne(null)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("relatedType cannot be null"));
    }
    
    @Test
    public void hasMany_null_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasMany(null)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("relatedType cannot be null"));
    }
    
    @Test
    public void hasOne_joinColumnNull_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnOwningSide(null)
         .populateProperty("customer")
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("joinColumnOwningSide cannot be null"));
    }
    
    @Test
    public void hasMany_joinColumnNull_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide(null)
         .populateProperty("orderLines")
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("joinColumnManySide cannot be null"));
    }
    
    
    @Test
    public void hasOne_populatePropertyNull_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty(null)
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("propertyName cannot be null"));
    }
    
    @Test
    public void hasMany_populatePropertyNull_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty(null)
        .execute(jtm);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("propertyName cannot be null"));
    }
    
    @Test
    public void hasOne_jdbcTemplateMapperNull_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
        .execute(null);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("jdbcTemplateMapper cannot be null"));
    }
    
    @Test
    public void hasMany_jdbcTemplateMapperNull_test() {
        //@formatter:off  
       Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
        .execute(null);     
       });
        //@formatter:on
       assertTrue(exception.getMessage().contains("jdbcTemplateMapper cannot be null"));
    }
    
    
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
    public void hasMany_invalidHasManyClass_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
            Query.type(Order.class)
                    .hasMany(NoTableAnnotationModel.class)
                    .joinColumnManySide("order_id")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("does not have the @Table annotation"));
    }

    @Test
    public void hasOne_invalidHasManyClass2_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
            Query.type(Order.class)
                    .hasOne(NoTableAnnotationModel.class)
                    .joinColumnOwningSide("order_id")
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
                    .joinColumnManySide("x")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Invalid join column"));
    }

    @Test
    public void hasMany_invalidJoinColumnWithPrefix_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasMany(OrderLine.class)
                    .joinColumnManySide("order_line.order_id")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Invalid joinColumn"));
    }

    @Test
    public void hasMany_invalidJoinColumnBlank_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasMany(OrderLine.class)
                    .joinColumnManySide("     ")
                    .populateProperty("orderLines")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("joinColumnManySide cannot be blank"));
    }

    @Test
    public void hasOne_invalidJoinColumnWithPrefix_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasOne(Customer.class)
                    .joinColumnOwningSide("order.customer_id")
                    .populateProperty("customer")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("Invalid joinColumn"));
    }

    @Test
    public void hasOne_invalidJoinColumnBlank_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasOne(Customer.class)
                    .joinColumnOwningSide("")
                    .populateProperty("customer")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("joinColumnOwningSide cannot be blank"));
    }

    @Test
    public void hasOne_invalidJoinColumn_test() {
        //@formatter:off  
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
            Query.type(Order.class)
                    .hasOne(Customer.class)
                    .joinColumnOwningSide("x")
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
                    .joinColumnManySide("order_id")
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
                    .joinColumnManySide("order_id")
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
                    .joinColumnManySide("customer_id")
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
                    .joinColumnOwningSide("customer_id")
                    .populateProperty("status")
                    .execute(jtm);      
       });
        //@formatter:on
        assertTrue(exception.getMessage().contains("property type conflict"));
    }

    @Test
    public void hasMany_List_success_test() {
      //@formatter:off
        List<Order> orders = Query.type(Order.class)
        .hasMany(OrderLine.class) 
        .joinColumnManySide("order_id")
        .populateProperty("orderLines") // list
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.order_id, order_line.order_line_id")
        .execute(jtm);

        assertTrue(orders.size() == 2);
        assertTrue(orders.get(0).getOrderLines().size() == 2);
        assertEquals("IN PROCESS", orders.get(1).getStatus());
        assertTrue(orders.get(1).getOrderLines().size() == 1);
       
        
      //@formatter:on
    }

    @Test
    public void hasMany_Set_success_test() {
      //@formatter:off
        List<Order4> orders = Query.type(Order4.class)
        .hasMany(OrderLine.class)
        .joinColumnManySide("order_id")
        .populateProperty("orderLines") //set
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.order_id ASC")
        .execute(jtm); 
        
        assertTrue(orders.size() == 2);

        
      //@formatter:on
    }

    @Test
    public void hasOne_success_test() {
      //@formatter:off
        List<Order> orders = Query.type(Order.class)
             .hasOne(Customer.class)
             .joinColumnOwningSide("customer_id")
             .populateProperty("customer")
             .where("orders.status = ?", "IN PROCESS")
             .orderBy("orders.status    DESC")
             .execute(jtm);
        
        assertTrue(orders.size() == 2);
        assertTrue("tony".equals(orders.get(0).getCustomer().getFirstName()));
        assertTrue("jane".equals(orders.get(1).getCustomer().getFirstName()));
        
        
      //@formatter:on
    }

    @Test
    public void typeOnly_success_test() {
      //@formatter:off
        List<Order> orders = Query.type(Order.class)
        .execute(jtm);
        
        assertTrue(orders.size() == 3);
        
      //@formatter:on
    }

    @Test
    public void whereonly_success_test() {
      //@formatter:off
        List<Order> orders = Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .execute(jtm);
        
        assertTrue(orders.size() == 2);
        
      //@formatter:on
    }
    
    @Test
    public void whereAndOrderBy_success_test() {
      //@formatter:off
        List<Order> orders = Query.type(Order.class)
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.order_id")
        .execute(jtm);
        
        assertTrue(orders.size() == 2);
        assertTrue(orders.get(0).getOrderId() == 1);
        assertTrue(orders.get(1).getOrderId() == 2);
      //@formatter:on
    }
    
    @Test
    public void hasOne_withoutWhereAndOrderBy_success_test() {
      //@formatter:off
        List<Order> orders = 
                Query.type(Order.class)
        .hasOne(Customer.class)
        .joinColumnOwningSide("customer_id")
        .populateProperty("customer")
        .execute(jtm);
        
        assertNotNull(orders.get(0).getCustomer());
        
      //@formatter:on
    }
    
    @Test
    public void hasMany_withoutWhereAndOrderBy_success_test() {
        //@formatter:off
          List<Order> orders = Query.type(Order.class)
          .hasMany(OrderLine.class) 
          .joinColumnManySide("order_id")
          .populateProperty("orderLines") // list
          .execute(jtm);
          
          assertTrue(orders.get(0).getOrderLines().size() > 0);
        //@formatter:on
         
      }
   
    
    @Test
    public void hasOne_nonDefaultNaming_success_test() {
      //@formatter:off
        List<Order7> orders = Query.type(Order7.class)
             .hasOne(Customer7.class)
             .joinColumnOwningSide("customer_id")
             .populateProperty("customer")
             .where("orders.status = ?", "IN PROCESS")
             .orderBy("orders.status    DESC")
             .execute(jtm);
        
        assertTrue(orders.size() == 2);
        assertTrue("tony".equals(orders.get(0).getCustomer().getFirstName()));
        assertTrue("jane".equals(orders.get(1).getCustomer().getFirstName()));
        
        
      //@formatter:on
    }
    
    @Test
    public void hasMany_NonDefaultNaming_success_test() {
      //@formatter:off
        List<Order7> orders = 
        Query.type(Order7.class)
        .hasMany(OrderLine7.class) 
        .joinColumnManySide("order_id")
        .populateProperty("orderLines")
        .where("orders.status = ?", "IN PROCESS")
        .orderBy("orders.order_id, order_line.order_line_id")
        .execute(jtm);

        assertTrue(orders.size() == 2);
        assertTrue(orders.get(0).getId() != null);
        assertTrue(orders.get(0).getOrderLines().size() == 2);
        assertTrue(orders.get(0).getOrderLines().get(0).getId() != null);
        assertEquals("IN PROCESS", orders.get(1).getStatus());
        assertTrue(orders.get(1).getOrderLines().size() == 1);
        
      //@formatter:on
    }
}
