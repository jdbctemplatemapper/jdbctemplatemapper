package io.github.jdbctemplatemapper.test;

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
import io.github.jdbctemplatemapper.core.QueryMerge;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryMergeTableAliasTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void hasOne_tableAlias_success() {

    List<Order> orders = Query.type(Order.class)
                              .where("orders.status = ?", "IN PROCESS")
                              .orderBy("orders.order_id DESC")
                              .execute(jtm);

    QueryMerge.type(Order.class)
              .hasOne(Customer.class, "c")
              .joinColumnTypeSide("CUSTOMER_ID")
              .populateProperty("customer")
              .execute(jtm, orders);

    assertTrue("jane".equals(orders.get(0).getCustomer().getFirstName()));
    assertTrue("joe".equals(orders.get(1).getCustomer().getLastName()));
  }

  @Test
  public void hasMany_tableAlias_success() {

    List<Order> orders = Query.type(Order.class)
                              .where("orders.status = ?", "IN PROCESS")
                              .orderBy("orders.order_id DESC")
                              .execute(jtm);

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class, "ol")
              .joinColumnManySide("order_id")
              .populateProperty("orderLines")
              .execute(jtm, orders);

    boolean oneOrderLines = false;
    boolean twoOrderLines = false;
    int cnt = 0;
    for (Order order : orders) {
      if (order.getOrderLines().size() == 1) {

        oneOrderLines = true;
      }
      if (order.getOrderLines().size() == 2) {
        twoOrderLines = true;
      }
      cnt += order.getOrderLines().size();
    }
    assertTrue(oneOrderLines);
    assertTrue(twoOrderLines);
    assertTrue(cnt == 3);

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class, "ol")
              .joinColumnManySide("order_id")
              .populateProperty("orderLines")
              .orderBy("ol.order_line_id")
              .execute(jtm, orders);
  }
}
