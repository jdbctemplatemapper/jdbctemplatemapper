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
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryTableAliasTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void singleTable_tableAlias_success() {
    List<Order> orders = Query.type(Order.class, "o")
                              .where("o.status = ?", "IN PROCESS")
                              .orderBy("o.order_id")
                              .execute(jtm);

    assertTrue(orders.size() > 0);
  }

  @Test
  public void hasMany_tableAlias_success() {

    List<Order> orders = Query.type(Order.class, "o")
                              .hasMany(OrderLine.class, "ol")
                              .joinColumnManySide("order_id")
                              .populateProperty("orderLines")
                              .where("o.status = ? and ol.num_of_units > ?", "IN PROCESS", 0)
                              .orderBy("o.order_id, ol.order_line_id")
                              .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue(orders.get(0).getOrderLines().size() == 2);
    assertEquals("IN PROCESS", orders.get(1).getStatus());
    assertTrue(orders.get(0).getOrderLines().get(0).getNumOfUnits() > 0);
    assertTrue(orders.get(1).getOrderLines().size() == 1);

    orders = Query.type(Order.class, "o")
                  .hasMany(OrderLine.class) // no alias for hasMany
                  .joinColumnManySide("order_id")
                  .populateProperty("orderLines")
                  .where("o.status = ?", "IN PROCESS")
                  .orderBy("o.order_id, order_line.order_line_id")
                  .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue(orders.get(0).getOrderLines().size() == 2);
    assertEquals("IN PROCESS", orders.get(1).getStatus());
    assertTrue(orders.get(1).getOrderLines().size() == 1);

    orders = Query.type(Order.class)
                  .hasMany(OrderLine.class, "ol") // no alias for hasMany
                  .joinColumnManySide("order_id")
                  .populateProperty("orderLines")
                  .where("orders.status = ?", "IN PROCESS")
                  .orderBy("orders.order_id, ol.order_line_id")
                  .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue(orders.get(0).getOrderLines().size() == 2);
    assertEquals("IN PROCESS", orders.get(1).getStatus());
    assertTrue(orders.get(1).getOrderLines().size() == 1);
  }

  @Test
  public void hasOne_tableAlias_success() {
    List<Order> orders = Query.type(Order.class, "o")
                              .hasOne(Customer.class, "c")
                              .joinColumnOwningSide("customer_id")
                              .populateProperty("customer")
                              .where("o.status = ? and c.last_name like ?", "IN PROCESS", "%")
                              .orderBy("o.status DESC, c.first_name desc")
                              .execute(jtm);

    assertTrue(orders.size() == 2);
    assertTrue("tony".equals(orders.get(0).getCustomer().getFirstName()));
    assertTrue("jane".equals(orders.get(1).getCustomer().getFirstName()));

  }

}
