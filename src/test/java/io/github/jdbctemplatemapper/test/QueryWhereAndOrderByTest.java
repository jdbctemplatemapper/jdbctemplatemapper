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
import io.github.jdbctemplatemapper.model.OrderLine;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryWhereAndOrderByTest {

    @Value("${spring.datasource.driver-class-name}")
    private String jdbcDriver;

    @Autowired
    private JdbcTemplateMapper jtm;

    @Test
    public void query_where_invalidBlank_test() {
    // @formatter:off

    Exception exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> {
              Query.type(Order.class).where("").execute(jtm);
            });
    // @formatter:on
        assertTrue(exception.getMessage().contains("whereClause cannot be null or blank"));
    }

    @Test
    public void query_orderBy_invalidWithJustPeriod_test() {
    // @formatter:off

    Exception exception =
        Assertions.assertThrows(
            QueryException.class,
            () -> {
              Query.type(Order.class).orderBy(".").execute(jtm);
            });
    // @formatter:on
        assertTrue(exception.getMessage().contains("orderBy() invalid table alias"));
    }

    @Test
    public void query_orderBy_multiClauseInvalidColumn_test() {
    // @formatter:off
    Exception exception =
        Assertions.assertThrows(
            QueryException.class,
            () -> {
              Query.type(Order.class)
                  .orderBy("orders.status DeSC, orders.id Asc") // orders.id is invalid
                  .execute(jtm);
            });
    // @formatter:on
        assertTrue(exception.getMessage().contains("orderBy() invalid column name "));
    }

    @Test
    public void query_orderBy_invalidNoTableAlias_test() {
    // @formatter:off
    Exception exception =
        Assertions.assertThrows(
            QueryException.class,
            () -> {
              Query.type(Order.class).orderBy("status DESC").execute(jtm);
            });
    // @formatter:on
        assertTrue(exception.getMessage()
                .contains("Invalid orderBy(). Note that the column name should be prefixed by table alias"));
    }

    @Test
    public void query_orderBy_invalidTableAlias_test() {
    // @formatter:off
    Exception exception =
        Assertions.assertThrows(
            QueryException.class,
            () -> {
              Query.type(Order.class).orderBy(" xyz.status DESC ").execute(jtm);
            });
    // @formatter:on
        assertTrue(exception.getMessage().contains("orderBy() invalid table alias"));
    }

    @Test
    public void query_orderBy_invalidBlankValue_test() {
    // @formatter:off
    Exception exception =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> {
              Query.type(Order.class).orderBy(" ").execute(jtm);
            });
    // @formatter:on
        assertTrue(exception.getMessage().contains("orderBy cannot be null or blank"));
    }

    @Test
    public void orderBy_invalidRelatedTable_Test() {
    // @formatter:off
    Exception exception =
        Assertions.assertThrows(
            QueryException.class,
            () -> {
              Query.type(Order.class)
                  .hasMany(OrderLine.class)
                  .joinColumnManySide("order_id")
                  .populateProperty("orderLines")
                  .where("orders.status = ?", "IN PROCESS")
                  .orderBy("orders.status ,  order_linex.num_of_units DESC")
                  .execute(jtm);
            });

    // @formatter:on

        assertTrue(exception.getMessage().contains("orderBy() invalid table alias"));
    }

    @Test
    public void orderBy_Success_Test() {
    // @formatter:off
    Query.type(Order.class).orderBy("orders.status").execute(jtm);

    Query.type(Order.class).orderBy("orders.status DESC").execute(jtm);

    Query.type(Order.class).orderBy("orders.status  ASC ").execute(jtm);

    Query.type(Order.class).orderBy("orders.STATUS ASC  ,  orders.order_id   Desc").execute(jtm);

    // @formatter:on
    }

    @Test
    public void orderBy_relationshipColumn_hasMany_success_Test() {
    // @formatter:off
        Query.type(Order.class)
            .hasMany(OrderLine.class)
            .joinColumnManySide("order_id")
            .populateProperty("orderLines")
            .where("orders.status = ?", "IN PROCESS")
            .orderBy(" orders.status  ASC,  order_line.num_of_units DESC")
            .execute(jtm);
    // @formatter:on
    }
}
