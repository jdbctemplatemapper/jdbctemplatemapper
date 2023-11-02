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
public class QueryCountTableAliasTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void singleTableCount_tableAlias_success() {
    Integer count = QueryCount.type(Order.class, "o") // single table query with alias
                              .execute(jtm);

    assertTrue(count > 0);

    count = QueryCount.type(Order.class, "o").where("o.status = ?", "IN PROCESS").execute(jtm);

    assertTrue(count > 0);
  }

  @Test
  public void toOne_executeCount_tableAlias_success_test() {

    Integer count = QueryCount.type(Order.class, "o")
                              .toOne(Customer.class, "c")
                              .joinColumnOwningSide("customer_id")
                              .where("o.status = ?", "IN PROCESS")
                              .execute(jtm);
    assertTrue(2 == count);

    count = QueryCount.type(Order.class)
                      .toOne(Customer.class, "c") // alias only for related
                      .joinColumnOwningSide("customer_id")
                      .where("orders.status = ?", "IN PROCESS")
                      .execute(jtm);
    assertTrue(2 == count);

    count = QueryCount.type(Order.class, "o") // alias only for owning
                      .toOne(Customer.class)
                      .joinColumnOwningSide("customer_id")
                      .where("o.status = ?", "IN PROCESS")
                      .execute(jtm);
    assertTrue(2 == count);

  }
}
