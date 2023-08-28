package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.exception.QueryException;
import io.github.jdbctemplatemapper.model.Order;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryOrderByTest {

    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;
    
    @Test
    public void query_orderBy_invalidWithJustPeriod_test() {
      //@formatter:off  
        
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Order.class)
             .orderBy(".")
             .execute(jtm); 
        });       
        //@formatter:on
        assertTrue(exception.getMessage()
                .contains("orderBy() Invalid table alias. Column should be prefixed by"));
    }

    @Test
    public void query_orderBy_multiClauseInvalidColumn_test() {
      //@formatter:off       
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Order.class)
             .orderBy("orders.status DeSC, orders.id Asc")
             .execute(jtm); 
        });       
        //@formatter:on
        assertTrue(exception.getMessage().contains("orderBy() invalid column name "));
    }

    @Test
    public void query_orderBy_invalidNoTableAlias_test() {
      //@formatter:off       
        Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Order.class)
             .orderBy("status DESC")
             .execute(jtm); 
        });       
        //@formatter:on
        assertTrue(
                exception.getMessage().contains("Invalid orderBy(). Note that the column name should be prefixed by table alias"));
    }

    @Test
    public void query_orderBy_invalidTableAlias_test() {
    //@formatter:off  
      Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Order.class)
             .orderBy(" xyz.status DESC ")
             .execute(jtm); 
      });
    //@formatter:on  
        assertTrue(exception.getMessage().contains("orderBy() Invalid table alias"));
    }

    @Test
    public void query_orderBy_invalidBlankValue_test() {
    //@formatter:off  
      Exception exception = Assertions.assertThrows(QueryException.class, () -> {
        Query.type(Order.class)
             .orderBy(" ")
             .execute(jtm); 
      });
    //@formatter:on  
        assertTrue(exception.getMessage().contains("orderBy() blank string is invalid"));
    }
}