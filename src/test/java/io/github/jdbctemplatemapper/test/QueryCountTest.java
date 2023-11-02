package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.QueryCount;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Order;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryCountTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;


  @Test
  public void singeTable_count_success() {
    Integer count = QueryCount.type(Order.class).execute(jtm);
    assertTrue(3 == count);

    count = QueryCount.type(Order.class).where("orders.status = ?", "IN PROCESS").execute(jtm);

    assertTrue(2 == count);
  }

  @Test
  public void toOne_count_success() {
    Integer count = QueryCount.type(Order.class)
                              .toOne(Customer.class)
                              .joinColumnOwningSide("customer_id")
                              .where("orders.status = ?", "IN PROCESS")
                              .execute(jtm);

    assertTrue(2 == count);
  }
}
