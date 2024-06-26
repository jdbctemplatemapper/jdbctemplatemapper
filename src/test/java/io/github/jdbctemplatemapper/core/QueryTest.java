package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.exception.QueryException;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Customer7;
import io.github.jdbctemplatemapper.model.InvalidTableObject;
import io.github.jdbctemplatemapper.model.NoTableAnnotationModel;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.Order2;
import io.github.jdbctemplatemapper.model.Order3;
import io.github.jdbctemplatemapper.model.Order4;
import io.github.jdbctemplatemapper.model.Order7;
import io.github.jdbctemplatemapper.model.Order9;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.OrderLine7;
import io.github.jdbctemplatemapper.model.PersonView;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryTest {
  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void type_null_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(null).execute(jtm);
    });

    assertTrue(exception.getMessage().contains("type cannot be null"));
  }

  @Test
  public void where_null_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class).where(null).execute(jtm);
    });

    assertTrue(exception.getMessage().contains("whereClause cannot be null"));
  }

  @Test
  public void orderBy_null_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class).orderBy(null).execute(jtm);
    });

    assertTrue(exception.getMessage().contains("orderBy cannot be null"));
  }

  @Test
  public void limitOffsetClause_null_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class).limitOffsetClause(null).execute(jtm);
    });

    assertTrue(exception.getMessage().contains("limitOffsetClause cannot be null"));
  }

  @Test
  public void hasOne_null_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasOne(null)
           .joinColumnTypeSide("customer_id")
           .populateProperty("customer")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("relatedType cannot be null"));
  }

  @Test
  public void hasMany_null_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasMany(null)
           .joinColumnManySide("order_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("relatedType cannot be null"));
  }

  @Test
  public void hasOne_joinColumnNull_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide(null)
           .populateProperty("customer")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("joinColumnTypeSide cannot be null"));
  }

  @Test
  public void hasMany_joinColumnNull_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide(null)
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("joinColumnManySide cannot be null"));
  }

  @Test
  public void hasOne_populatePropertyNull_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide("customer_id")
           .populateProperty(null)
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("propertyName cannot be null"));
  }

  @Test
  public void hasMany_populatePropertyNull_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_id")
           .populateProperty(null)
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("propertyName cannot be null"));
  }

  @Test
  public void hasOne_jdbcTemplateMapperNull_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide("customer_id")
           .populateProperty("customer")
           .execute(null);
    });

    assertTrue(exception.getMessage().contains("jdbcTemplateMapper cannot be null"));
  }

  @Test
  public void hasMany_jdbcTemplateMapperNull_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_id")
           .populateProperty("orderLines")
           .execute(null);
    });

    assertTrue(exception.getMessage().contains("jdbcTemplateMapper cannot be null"));
  }

  @Test
  public void invalidType_test() {

    Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
      Query.type(InvalidTableObject.class).execute(jtm);
    });

    assertTrue(exception.getMessage().contains("Unable to locate meta-data for table"));
  }

  @Test
  public void hasMany_invalidHasManyClass_test() {

    Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
      Query.type(Order.class)
           .hasMany(NoTableAnnotationModel.class)
           .joinColumnManySide("order_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("does not have the @Table annotation"));
  }

  @Test
  public void hasOne_invalidHasManyClass2_test() {

    Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
      Query.type(Order.class)
           .hasOne(NoTableAnnotationModel.class)
           .joinColumnTypeSide("order_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("does not have the @Table annotation"));
  }

  @Test
  public void hasMany_invalidJoinColumn_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("x")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("Invalid join column"));
  }

  @Test
  public void hasMany_invalidJoinColumnWithPrefix_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_line.order_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("Invalid joinColumn"));
  }

  @Test
  public void hasMany_invalidJoinColumnBlank_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("     ")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("joinColumnManySide cannot be null or blank"));
  }

  @Test
  public void hasOne_invalidJoinColumnWithPrefix_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide("order.customer_id")
           .populateProperty("customer")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("Invalid joinColumn"));
  }

  @Test
  public void hasOne_invalidJoinColumnBlank_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide("")
           .populateProperty("customer")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("joinColumnTypeSide cannot be null or blank"));
  }

  @Test
  public void hasOne_invalidJoinColumn_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide("x")
           .populateProperty("customer")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("Invalid join column"));
  }

  @Test
  public void hasMany_populatePropertyNotACollection_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_id")
           .populateProperty("customer")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("is not a collection"));
  }

  @Test
  public void hasMany_populatePropertyCollectionHasNoGenericType_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order2.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(
        exception.getMessage().contains("Collections without generic types are not supported"));
  }

  @Test
  public void hasMany_populatePropertyCollectionNotInitialized_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order3.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(exception.getMessage()
                        .contains("Only initialized collections can be populated by queries"));
  }

  @Test
  public void hasMany_populatePropertyCollectionTypeMismatch_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasMany(Customer.class)
           .joinColumnManySide("customer_id")
           .populateProperty("orderLines")
           .execute(jtm);
    });

    assertTrue(
        exception.getMessage()
                 .contains("Collection generic type and hasMany relationship type mismatch"));
  }

  @Test
  public void hasOne_populatePropertyTypeConflict_test() {

    Exception exception = Assertions.assertThrows(QueryException.class, () -> {
      Query.type(Order.class)
           .hasOne(Customer.class)
           .joinColumnTypeSide("customer_id")
           .populateProperty("status")
           .execute(jtm);
    });

    assertTrue(exception.getMessage().contains("property type conflict"));
  }

  @Test
  public void hasMany_OrderLinesInitializedWithValues_test() {
    Order9 order = new Order9();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2);
    jtm.insert(order);


    List<Order9> orders = Query.type(Order9.class)
                               .hasMany(OrderLine.class)
                               .joinColumnManySide("order_id")
                               .populateProperty("orderLines") // list
                               .where("orders.order_id = ?", order.getOrderId())
                               .execute(jtm);

    assertEquals(0, orders.get(0).getOrderLines().size());

    jtm.delete(order);


  }

  @Test
  public void hasMany_List_success_test() {

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
    assertTrue(orders.get(0).getOrderLines().get(0).getNumOfUnits() > 0);
    assertTrue(orders.get(1).getOrderLines().size() == 1);


  }

  @Test
  public void hasMany_Set_success_test() {

    List<Order4> orders = Query.type(Order4.class)
                               .hasMany(OrderLine.class)
                               .joinColumnManySide("order_id")
                               .populateProperty("orderLines") // set
                               .where("orders.status = ?", "IN PROCESS")
                               .orderBy("orders.order_id ASC")
                               .execute(jtm);

    assertTrue(orders.size() == 2);


  }

  @Test
  public void hasOne_success_test() {

    List<Order> orders = Query.type(Order.class)
                              .hasOne(Customer.class)
                              .joinColumnTypeSide("customer_id")
                              .populateProperty("customer")
                              .where("orders.status = ?", "IN PROCESS")
                              .orderBy("orders.order_id    ASC")
                              .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue("tony".equals(orders.get(0).getCustomer().getFirstName()));
    assertTrue("jane".equals(orders.get(1).getCustomer().getFirstName()));


  }

  @Test
  public void typeOnly_success_test() {
    List<Order> orders = Query.type(Order.class).execute(jtm);
    assertTrue(orders.size() > 0);
  }

  @Test
  public void databaseView_success_test() {
    List<PersonView> list = Query.type(PersonView.class).execute(jtm);
    assertTrue(list.size() > 0);
  }

  @Test
  public void whereonly_success_test() {

    List<Order> orders =
        Query.type(Order.class).where("orders.status = ?", "IN PROCESS").execute(jtm);

    assertTrue(orders.size() == 2);
  }

  @Test
  public void whereWithMapSqlParameterSource_success_test() {

    List<Order> orders = Query.type(Order.class)
                              .where("orders.status = :status",
                                  new MapSqlParameterSource().addValue("status", "IN PROCESS"))
                              .execute(jtm);

    assertTrue(orders.size() == 2);
  }


  @Test
  public void whereAndOrderBy_success_test() {

    List<Order> orders = Query.type(Order.class)
                              .where("orders.status = ?", "IN PROCESS")
                              .orderBy("orders.order_id")
                              .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue(orders.get(0).getOrderId() == 1);
    assertTrue(orders.get(1).getOrderId() == 2);

  }

  @Test
  public void hasOne_withoutWhereAndOrderBy_success_test() {

    List<Order> orders = Query.type(Order.class)
                              .hasOne(Customer.class)
                              .joinColumnTypeSide("customer_id")
                              .populateProperty("customer")
                              .execute(jtm);

    assertNotNull(orders.get(0).getCustomer());


  }

  @Test
  public void hasMany_withoutWhereAndOrderBy_success_test() {

    List<Order> orders = Query.type(Order.class)
                              .hasMany(OrderLine.class)
                              .joinColumnManySide("order_id")
                              .populateProperty("orderLines") // list
                              .execute(jtm);

    assertTrue(orders.get(0).getOrderLines().size() > 0);


  }

  @Test
  public void hasOne_nonDefaultNaming_success_test() {

    List<Order7> orders = Query.type(Order7.class)
                               .hasOne(Customer7.class)
                               .joinColumnTypeSide("customer_id")
                               .populateProperty("customer")
                               .where("orders.status = ?", "IN PROCESS")
                               .orderBy("orders.status DESC, orders.order_id asc")
                               .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue("tony".equals(orders.get(0).getCustomer().getFirstName()));
    assertTrue("jane".equals(orders.get(1).getCustomer().getFirstName()));


  }

  @Test
  public void hasMany_NonDefaultNaming_success_test() {

    List<Order7> orders = Query.type(Order7.class)
                               .hasMany(OrderLine7.class)
                               .joinColumnManySide("ORDER_ID")
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


  }


  @Test
  public void hasMany_limitOffsetClause_failure_test() {

    Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
      Query.type(Order.class)
           .hasMany(OrderLine.class)
           .joinColumnManySide("order_id")
           .populateProperty("orderLines")
           .limitOffsetClause("OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY")
           .execute(jtm);
    });

    assertTrue(
        exception.getMessage()
                 .contains(
                     "limitOffsetClause is not supported for hasMany and hasMany through relationships."));
  }

  @Test
  public void limitOffsetClauseOnly_success_test() {
    String limitOffsetClause = null;
    if (jdbcDriver.contains("postgres")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY";
    }
    if (jdbcDriver.contains("mysql")) {
      limitOffsetClause = "LIMIT 10 OFFSET 0";
    }
    if (jdbcDriver.contains("oracle")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
    }
    if (jdbcDriver.contains("sqlserver")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
    }

    List<Order> orders = Query.type(Order.class)
                              .orderBy("orders.order_id") // SQLServer needs orderBy to work with
                                                          // offset
                              .limitOffsetClause(limitOffsetClause)
                              .execute(jtm);

    assertTrue(orders.size() > 0);

  }

  @Test
  public void hasOne_limitOffsetClause_success_test() {
    String limitOffsetClause = null;
    if (jdbcDriver.contains("postgres")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY";
    }
    if (jdbcDriver.contains("mysql")) {
      limitOffsetClause = "LIMIT 10 OFFSET 0";
    }
    if (jdbcDriver.contains("oracle")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
    }
    if (jdbcDriver.contains("sqlserver")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
    }

    List<Order> orders = Query.type(Order.class)
                              .hasOne(Customer.class)
                              .joinColumnTypeSide("customer_id")
                              .populateProperty("customer")
                              .orderBy("customer.customer_id")
                              .limitOffsetClause(limitOffsetClause)
                              .execute(jtm);

    assertTrue(orders.size() > 0);
  }

  @Test
  public void query_methodChainingSequenceTest_success_test() {
    String limitOffsetClause = null;
    if (jdbcDriver.contains("postgres")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH FIRST 10 ROWS ONLY";
    }
    if (jdbcDriver.contains("mysql")) {
      limitOffsetClause = "LIMIT 10 OFFSET 0";
    }
    if (jdbcDriver.contains("oracle")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
    }
    if (jdbcDriver.contains("sqlserver")) {
      limitOffsetClause = "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY";
    }

    Query.type(Order.class).execute(jtm);

    Query.type(Order.class).where("orders.status = ?", "IN PROCESS").execute(jtm);

    Query.type(Order.class).orderBy("orders.order_id").execute(jtm);

    Query.type(Order.class)
         .orderBy("orders.order_id")
         .limitOffsetClause(limitOffsetClause)
         .execute(jtm);

    Query.type(Order.class)
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.order_id")
         .execute(jtm);

    Query.type(Order.class)
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.order_id")
         .limitOffsetClause(limitOffsetClause)
         .execute(jtm);

    Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnTypeSide("customer_id")
         .populateProperty("customer")
         .where("orders.status = ?", "IN PROCESS")
         .execute(jtm);

    Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnTypeSide("customer_id")
         .populateProperty("customer")
         .orderBy("customer.customer_id")
         .execute(jtm);

    Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnTypeSide("CUSTOMER_ID")
         .populateProperty("customer")
         .orderBy("orders.order_id")
         .limitOffsetClause(limitOffsetClause)
         .execute(jtm);

    Query.type(Order.class)
         .hasOne(Customer.class)
         .joinColumnTypeSide("customer_id")
         .populateProperty("customer")
         .where("orders.status = ? and customer.last_name like ?", "IN PROCESS", "%")
         .orderBy("customer.customer_id")
         .limitOffsetClause(limitOffsetClause)
         .execute(jtm);
  }
}
