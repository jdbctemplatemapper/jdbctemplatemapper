package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Product;
import io.github.jdbctemplatemapper.model.Product8;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CachingTest {
  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void query_simpleQuery_caching() {
    SimpleCache<String, String> cache = jtm.getQuerySqlCache();
    cache.clear();

    // where, orderBy and offsetLimit are not part of caching

    Query.type(Order.class)
         .where("orders.status = ?", "COMPLETED")
         .orderBy("orders.order_id")
         .execute(jtm);

    assertEquals(1, cache.getSize());

    // same query again
    Query.type(Order.class)
         .where("orders.status = ?", "COMPLETED")
         .orderBy("orders.order_id")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class).orderBy("orders.order_id").execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class).where("orders.status = ?", "COMPLETED").execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class, "o") // table alias will create new cache entry
         .execute(jtm);
    assertEquals(2, cache.getSize());

  }

  @Test
  public void query_belongsTo_caching() {
    SimpleCache<String, String> cache = jtm.getQuerySqlCache();
    cache.clear();

    Query.type(Order.class)
         .belongsTo(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.status    DESC")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    // same query again
    Query.type(Order.class)
         .belongsTo(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.status    DESC")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class)
         .belongsTo(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .orderBy("orders.status    DESC")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class)
         .belongsTo(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class, "o")
         .belongsTo(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(2, cache.getSize());

    Query.type(Order.class, "o")
         .belongsTo(Customer.class)
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(2, cache.getSize());


    Query.type(Order.class)
         .belongsTo(Customer.class, "c")
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(3, cache.getSize());

    Query.type(Order.class)
         .belongsTo(Customer.class, "c")
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(3, cache.getSize());

    Query.type(Order.class, "o")
         .belongsTo(Customer.class, "c")
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(4, cache.getSize());

    // same query
    Query.type(Order.class, "o")
         .belongsTo(Customer.class, "c")
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .execute(jtm);
    assertEquals(4, cache.getSize());


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

    Query.type(Order.class, "o")
         .belongsTo(Customer.class, "c")
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .orderBy("o.order_id")
         .limitOffsetClause(limitOffsetClause)
         .execute(jtm);
    assertEquals(4, cache.getSize());

    Query.type(Order.class, "o")
         .belongsTo(Customer.class, "c")
         .joinColumnOwningSide("customer_id")
         .populateProperty("customer")
         .orderBy("o.order_id")
         .limitOffsetClause(limitOffsetClause)
         .execute(jtm);
    assertEquals(4, cache.getSize());

  }

  @Test
  public void query_hasMany_caching() {
    SimpleCache<String, String> cache = jtm.getQuerySqlCache();
    cache.clear();

    Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.order_id, order_line.order_line_id")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    // same query
    Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.order_id, order_line.order_line_id")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    // caching does not include order by
    Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("orders.status = ?", "IN PROCESS")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    // caching does not include where
    Query.type(Order.class)
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .execute(jtm);
    assertEquals(1, cache.getSize());

    Query.type(Order.class, "o")
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("o.status = ?", "IN PROCESS")
         .orderBy("o.order_id, order_line.order_line_id")
         .execute(jtm);
    assertEquals(2, cache.getSize());

    Query.type(Order.class, "o")
         .hasMany(OrderLine.class)
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("o.status = ?", "IN PROCESS")
         .orderBy("o.order_id, order_line.order_line_id")
         .execute(jtm);
    assertEquals(2, cache.getSize());


    Query.type(Order.class)
         .hasMany(OrderLine.class, "ol")
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.order_id, ol.order_line_id")
         .execute(jtm);
    assertEquals(3, cache.getSize());

    Query.type(Order.class)
         .hasMany(OrderLine.class, "ol")
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("orders.status = ?", "IN PROCESS")
         .orderBy("orders.order_id, ol.order_line_id")
         .execute(jtm);
    assertEquals(3, cache.getSize());

    Query.type(Order.class, "o")
         .hasMany(OrderLine.class, "ol")
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("o.status = ?", "IN PROCESS")
         .orderBy("o.order_id, ol.order_line_id")
         .execute(jtm);
    assertEquals(4, cache.getSize());

    Query.type(Order.class, "o")
         .hasMany(OrderLine.class, "ol")
         .joinColumnManySide("order_id")
         .populateProperty("orderLines")
         .where("o.status = ?", "IN PROCESS")
         .orderBy("o.order_id, ol.order_line_id")
         .execute(jtm);
    assertEquals(4, cache.getSize());

  }

  @Test
  public void queryCount_singeTableCache_success() {
    SimpleCache<String, String> cache = jtm.getQueryCountSqlCache();
    cache.clear();
    QueryCount.type(Order.class).execute(jtm);
    assertEquals(1, cache.getSize());

    QueryCount.type(Order.class).execute(jtm);
    assertEquals(1, cache.getSize());

    QueryCount.type(Order.class).where("orders.status = ?", "IN PROCESS").execute(jtm);
    assertEquals(1, cache.getSize());

  }

  @Test
  public void queryCount_belongsToCache_success() {
    SimpleCache<String, String> cache = jtm.getQueryCountSqlCache();
    cache.clear();

    QueryCount.type(Order.class)
              .belongsTo(Customer.class)
              .joinColumnOwningSide("customer_id")
              .execute(jtm);
    assertEquals(1, cache.getSize());

    QueryCount.type(Order.class)
              .belongsTo(Customer.class)
              .joinColumnOwningSide("customer_id")
              .where("orders.status = ?", "IN PROCESS")
              .execute(jtm);
    assertEquals(1, cache.getSize());

    QueryCount.type(Order.class)
              .belongsTo(Customer.class, "c")
              .joinColumnOwningSide("customer_id")
              .execute(jtm);
    assertEquals(2, cache.getSize());

    QueryCount.type(Order.class)
              .belongsTo(Customer.class, "c")
              .joinColumnOwningSide("customer_id")
              .execute(jtm);
    assertEquals(2, cache.getSize());

  }

  @Test
  public void queryMerge_belongsToCache_success() {
    SimpleCache<String, String> cache = jtm.getQueryMergeSqlCache();
    cache.clear();

    List<Order> orders = Query.type(Order.class)
                              .where("orders.status = ?", "IN PROCESS")
                              .orderBy("orders.order_id DESC")
                              .execute(jtm);

    QueryMerge.type(Order.class)
              .belongsTo(Customer.class)
              .joinColumnOwningSide("CUSTOMER_ID")
              .populateProperty("customer")
              .execute(jtm, orders);
    assertEquals(1, cache.getSize());

    QueryMerge.type(Order.class)
              .belongsTo(Customer.class)
              .joinColumnOwningSide("CUSTOMER_ID")
              .populateProperty("customer")
              .execute(jtm, orders);
    assertEquals(1, cache.getSize());

    QueryMerge.type(Order.class)
              .belongsTo(Customer.class, "c")
              .joinColumnOwningSide("CUSTOMER_ID")
              .populateProperty("customer")
              .execute(jtm, orders);
    assertEquals(2, cache.getSize());

    QueryMerge.type(Order.class)
              .belongsTo(Customer.class, "c")
              .joinColumnOwningSide("CUSTOMER_ID")
              .populateProperty("customer")
              .execute(jtm, orders);
    assertEquals(2, cache.getSize());

  }

  @Test
  public void queryMerge_hasMany_success() {

    SimpleCache<String, String> cache = jtm.getQueryMergeSqlCache();
    cache.clear();

    List<Order> orders = Query.type(Order.class)
                              .where("orders.status = ?", "IN PROCESS")
                              .orderBy("orders.order_id")
                              .execute(jtm);

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class)
              .joinColumnManySide("ORDER_ID")
              .populateProperty("orderLines")
              .execute(jtm, orders);
    assertEquals(1, cache.getSize());

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class)
              .joinColumnManySide("ORDER_ID")
              .populateProperty("orderLines")
              .orderBy("order_line_id")
              .execute(jtm, orders);
    assertEquals(1, cache.getSize());

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class)
              .joinColumnManySide("ORDER_ID")
              .populateProperty("orderLines")
              .orderBy("order_line_id")
              .execute(jtm, orders);
    assertEquals(1, cache.getSize());

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class, "ol")
              .joinColumnManySide("ORDER_ID")
              .populateProperty("orderLines")
              .execute(jtm, orders);
    assertEquals(2, cache.getSize());

    QueryMerge.type(Order.class)
              .hasMany(OrderLine.class, "ol")
              .joinColumnManySide("ORDER_ID")
              .populateProperty("orderLines")
              .orderBy("ol.order_line_id")
              .execute(jtm, orders);
    assertEquals(2, cache.getSize());

  }

  @Test
  public void jtm_findByIdCache_test() {
    SimpleCache<String, String> cache = jtm.getBeanColumnsSqlCache();
    cache.clear();

    jtm.findById(Order.class, 1);
    assertEquals(1, cache.getSize());

    jtm.findById(Order.class, 2);
    assertEquals(1, cache.getSize());

    jtm.findById(Customer.class, 1);
    assertEquals(2, cache.getSize());

  }

  @Test
  public void jtm_insertCache_test() {
    SimpleCache<String, SimpleJdbcInsert> cache = jtm.getInsertCache();
    cache.clear();

    Order order = new Order();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2);
    jtm.insert(order);
    assertEquals(1, cache.getSize());
    jtm.delete(order);

    order = new Order();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2);
    jtm.insert(order);
    assertEquals(1, cache.getSize());
    jtm.delete(order);

    Customer customer = new Customer();
    customer.setLastName("xyz");
    customer.setFirstName("abc");
    jtm.insert(customer);
    assertEquals(2, cache.getSize());
    jtm.delete(customer);

  }

  @Test
  public void jtm_updateCache_test() {
    SimpleCache<String, SqlAndParams> cache = jtm.getUpdateCache();
    cache.clear();

    Customer customer = new Customer();
    customer.setLastName("xyz");
    customer.setFirstName("abc");
    jtm.insert(customer);

    customer.setLastName("a");
    jtm.update(customer);
    assertEquals(1, cache.getSize());

    customer.setLastName("b");
    jtm.update(customer);
    assertEquals(1, cache.getSize());

    Product product = new Product();
    product.setProductId(10022);
    product.setName("xyz");
    jtm.insert(product);

    product.setName("abc");
    jtm.update(product);
    assertEquals(2, cache.getSize());

    product.setName("aaa");
    jtm.update(product);
    assertEquals(2, cache.getSize());

    jtm.delete(customer);

    jtm.delete(product);

  }

  @Test
  public void jtm_updatePropertiesCache_test() {
    SimpleCache<String, SqlAndParams> cache = jtm.getUpdatePropertiesCache();
    cache.clear();

    Product8 product = new Product8();
    product.setProductId(811);
    product.setName("p-811");
    product.setCost(4.75);
    jtm.insert(product);

    jtm.updateProperties(product, "cost");
    assertEquals(1, cache.getSize());

    jtm.updateProperties(product, "cost");
    assertEquals(1, cache.getSize());

    jtm.updateProperties(product, "name");
    assertEquals(2, cache.getSize());

    jtm.updateProperties(product, "cost", "name");
    assertEquals(3, cache.getSize());

    jtm.updateProperties(product, "cost", "name");
    assertEquals(3, cache.getSize());

    product.setVersion(1);
    jtm.updateProperties(product, "cost", "name", "version");
    assertEquals(4, cache.getSize());

    // larger than ACHEABLE_UPDATE_PROPERTIES_COUNT = 3, so no caching
    product.setCreatedOn(LocalDateTime.now());
    jtm.updateProperties(product, "cost", "name", "version", "createdOn");
    assertEquals(4, cache.getSize()); // no caching so count remains the same

    jtm.delete(product);

  }

}
