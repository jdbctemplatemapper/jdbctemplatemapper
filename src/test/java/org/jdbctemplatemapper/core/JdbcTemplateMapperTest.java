package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdbctemplatemapper.model.Customer;
import org.jdbctemplatemapper.model.NoIdObject;
import org.jdbctemplatemapper.model.NoTableObject;
import org.jdbctemplatemapper.model.Order;
import org.jdbctemplatemapper.model.OrderLine;
import org.jdbctemplatemapper.model.Person;
import org.jdbctemplatemapper.model.Product;
import org.jdbctemplatemapper.model.TypeCheck;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcTemplateMapperTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired private JdbcTemplateMapper jdbcTemplateMapper;

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate npJdbcTemplate;

  private boolean flag = false;

  @BeforeEach
  public void setup() {
    if (!flag) {
      this.jdbcTemplate = jdbcTemplateMapper.getJdbcTemplate();
      this.npJdbcTemplate = jdbcTemplateMapper.getNamedParameterJdbcTemplate();
      flag = true;
    }
  }

  @Test
  public void insert_Test() {
    Order order = new Order();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2);

    jdbcTemplateMapper.insert(order);

    order = jdbcTemplateMapper.findById(order.getId(), Order.class);

    assertNotNull(order.getId());
    assertNotNull(order.getCreatedBy());
    assertNotNull(order.getCreatedOn());
    assertNotNull(order.getUpdatedBy());
    assertNotNull(order.getUpdatedOn());
    assertEquals(1, order.getVersion());
    assertEquals("tester", order.getCreatedBy());
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void insert_withNonNullIdFailureTest() {
    Order order = new Order();
    order.setId(2002);
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2);

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.insert(order);
        });
  }

  @Test
  public void insert_withNoVersionAndCreatedInfoTest() {

    // Customer table does not have version, create_on, created_by etc
    Customer customer = new Customer();
    customer.setFirstName("aaa");
    customer.setLastName("bbb");

    jdbcTemplateMapper.insert(customer);

    Customer customer1 = jdbcTemplateMapper.findById(customer.getId(), Customer.class);

    assertNotNull(customer1.getId());
    assertEquals("aaa", customer1.getFirstName());
    assertEquals("bbb", customer1.getLastName());
  }

  @Test
  public void insert_nullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.insert(null);
        });
  }

  @Test
  public void insert_noIdObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          NoIdObject pojo = new NoIdObject();
          pojo.setSomething("abc");
          jdbcTemplateMapper.insert(pojo);
        });
  }

  @Test
  public void insertWithId_Test() {
    Product product = new Product();
    product.setId(1001);
    product.setName("hat");
    product.setCost(12.25);

    jdbcTemplateMapper.insertWithId(product);

    product = jdbcTemplateMapper.findById(1001, Product.class);
    assertNotNull(product.getId());
    assertEquals("hat", product.getName());
    assertEquals(12.25, product.getCost());
    assertNotNull(product.getCreatedBy());
    assertNotNull(product.getCreatedOn());
    assertNotNull(product.getUpdatedBy());
    assertNotNull(product.getUpdatedOn());
    assertEquals(1, product.getVersion());
    assertEquals("tester", product.getCreatedBy());
    assertEquals("tester", product.getUpdatedBy());
  }

  @Test
  public void insertWithId_withNullIdFailureTest() {
    Product product = new Product();
    product.setName("hat");
    product.setCost(12.25);
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.insertWithId(product);
        });
  }

  @Test
  public void insertWithId_nullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.insertWithId(null);
        });
  }

  @Test
  public void insertWithId_noIdObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          NoIdObject pojo = new NoIdObject();
          pojo.setSomething("abc");
          jdbcTemplateMapper.insertWithId(pojo);
        });
  }

  @Test
  public void update_Test() throws Exception {
    Order order = jdbcTemplateMapper.findById(1, Order.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    Thread.sleep(1000); // avoid timing issue.

    order.setStatus("IN PROCESS");

    jdbcTemplateMapper.update(order);

    order = jdbcTemplateMapper.findById(1, Order.class); // requery
    assertEquals("IN PROCESS", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void update_withNoVersionAndUpdateInfoTest() {
    Customer customer = jdbcTemplateMapper.findById(4, Customer.class);
    customer.setFirstName("xyz");
    jdbcTemplateMapper.update(customer);

    Customer customer1 = jdbcTemplateMapper.findById(4, Customer.class); // requery
    assertEquals("xyz", customer1.getFirstName());
  }

  @Test
  public void update_throwsOptimisticLockingExceptionTest() {
    Assertions.assertThrows(
        OptimisticLockingException.class,
        () -> {
          Order order = jdbcTemplateMapper.findById(2, Order.class);
          order.setVersion(order.getVersion() - 1);
          jdbcTemplateMapper.update(order);
        });
  }

  @Test
  public void update_nullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.update(null);
        });
  }

  @Test
  public void update_noTableObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          NoTableObject pojo = new NoTableObject();
          pojo.setSomething("abc");
          jdbcTemplateMapper.update(pojo);
        });
  }

  @Test
  public void update_nonDatabasePropertyTest() {
    Person person = jdbcTemplateMapper.findById(1, Person.class);

    person.setSomeNonDatabaseProperty("xyz");
    jdbcTemplateMapper.update(person);

    // requery
    person = jdbcTemplateMapper.findById(1, Person.class);

    assertNull(person.getSomeNonDatabaseProperty());
  }

  @Test
  public void update_byPropertyTest() throws Exception {
    Order order = jdbcTemplateMapper.findById(2, Order.class);

    Integer prevVersion = order.getVersion();

    LocalDateTime prevUpdatedOn = order.getUpdatedOn();
    order.setStatus("COMPLETE");

    Thread.sleep(1000); // avoid timing issue.
    jdbcTemplateMapper.update(order, "status");

    order = jdbcTemplateMapper.findById(2, Order.class); // requery

    assertEquals("COMPLETE", order.getStatus());
    assertTrue(order.getVersion() > prevVersion); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void update_byMultiPropertyTest() {
    Customer customer = jdbcTemplateMapper.findById(3, Customer.class);

    customer.setFirstName("Dan");
    customer.setLastName("Hughes");
    jdbcTemplateMapper.update(customer, "lastName", "firstName");

    customer = jdbcTemplateMapper.findById(3, Customer.class); // requery
    assertEquals("Dan", customer.getFirstName());
    assertEquals("Hughes", customer.getLastName());
  }

  @Test
  public void update_byPropertythrowsOptimisticLockingExceptionTest() {
    Assertions.assertThrows(
        OptimisticLockingException.class,
        () -> {
          Order order = jdbcTemplateMapper.findById(2, Order.class);
          order.setVersion(order.getVersion() - 1);
          order.setStatus("COMPLETE");
          jdbcTemplateMapper.update(order, "status"); // just update status
        });
  }

  @Test
  public void update_byPropertyNullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.update(null, "abc");
        });
  }

  @Test
  public void update_byPropertyNoIdObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          NoIdObject pojo = new NoIdObject();
          pojo.setSomething("abc");
          jdbcTemplateMapper.update(pojo, "something");
        });
  }

  @Test
  public void update_byPropertyAutoAssignFieldFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          Order order = jdbcTemplateMapper.findById(2, Order.class);
          order.setStatus("CLOSED");
          jdbcTemplateMapper.update(order, "status", "updatedOn");
        });
  }

  @Test
  public void update_nonDatabasePropertyFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          Person person = jdbcTemplateMapper.findById(1, Person.class);
          person.setSomeNonDatabaseProperty("xyz");
          jdbcTemplateMapper.update(person, "someNonDatabaseProperty");
        });
  }

  @Test
  public void findById_Test() {
    Order order = jdbcTemplateMapper.findById(1, Order.class);

    assertNotNull(order.getId());
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getCreatedBy());
    assertNotNull(order.getCreatedOn());
    assertNotNull(order.getUpdatedBy());
    assertNotNull(order.getUpdatedOn());
    assertNotNull(order.getVersion());
  }

  @Test
  public void findAll_Test() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class);
    assertTrue(orders.size() >= 2);

    for (int idx = 0; idx < orders.size(); idx++) {
      assertNotNull(orders.get(idx).getId());
      assertNotNull(orders.get(idx).getOrderDate());
      assertNotNull(orders.get(idx).getCreatedBy());
      assertNotNull(orders.get(idx).getCreatedOn());
      assertNotNull(orders.get(idx).getUpdatedBy());
      assertNotNull(orders.get(idx).getUpdatedOn());
      assertNotNull(orders.get(idx).getVersion());
    }
  }

  @Test
  public void findAll_WithOrderByClauseTest() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class, "order by id");

    assertTrue(orders.size() >= 2);

    for (int idx = 0; idx < orders.size(); idx++) {
      assertNotNull(orders.get(idx).getId());
      assertNotNull(orders.get(idx).getOrderDate());
      assertNotNull(orders.get(idx).getCreatedBy());
      assertNotNull(orders.get(idx).getCreatedOn());
      assertNotNull(orders.get(idx).getUpdatedBy());
      assertNotNull(orders.get(idx).getUpdatedOn());
      assertNotNull(orders.get(idx).getVersion());
    }
  }

  @Test
  public void deleteByObject_Test() {
    Product product = jdbcTemplateMapper.findById(4, Product.class);

    int cnt = jdbcTemplateMapper.delete(product);

    assertTrue(cnt == 1);

    Product product1 = jdbcTemplateMapper.findById(4, Product.class);

    assertNull(product1);
  }

  @Test
  public void delete_nullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.delete(null);
        });
  }

  @Test
  public void delete_noIdObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          NoIdObject pojo = new NoIdObject();
          pojo.setSomething("abc");
          jdbcTemplateMapper.delete(pojo);
        });
  }

  @Test
  public void deleteById_Test() {
    int cnt = jdbcTemplateMapper.deleteById(5, Product.class);

    assertTrue(cnt == 1);

    Product product1 = jdbcTemplateMapper.findById(5, Product.class);

    assertNull(product1);
  }

  @Test
  public void deleteById_nullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.deleteById(null, Product.class);
        });
  }

  @Test
  public void toOneForObject_Test() {
    Order order = jdbcTemplateMapper.findById(1, Order.class);
    // this method issues a query behind the scenes to populate customer
    jdbcTemplateMapper.toOneForObject(order, Customer.class, "customer", "customerId");

    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }

  @Test
  public void toOneForObject_NullRelationshipTest() {
    Order order = jdbcTemplateMapper.findById(3, Order.class);
    // order 3 has null customer
    jdbcTemplateMapper.toOneForObject(order, Customer.class, "customer", "customerId");

    assertNull(order.getCustomer());
  }

  @Test
  public void toOneForObject_NoRecordTest() {
    Order order = jdbcTemplateMapper.findById(999, Order.class);
    // order 3 has null customer
    jdbcTemplateMapper.toOneForObject(order, Customer.class, "customer", "customerId");

    assertNull(order);
  }

  @Test
  public void toOneForList_Test() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class, "order by id");

    // this method issues a query behind the scenes to populate customer
    jdbcTemplateMapper.toOneForList(orders, Customer.class, "customer", "customerId");

    assertTrue(orders.size() >= 3);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
    assertNull(orders.get(2).getCustomer()); // order 3 has null customer
  }

  @Test
  public void toOneForList_NoRecordTest() {
    // mimick query returning no orders
    List<Order> orders = null;
    jdbcTemplateMapper.toOneForList(orders, Customer.class, "customer", "customerId");

    assertNull(orders);
  }

  @Test
  public void toOneForList_InvalidArgumentsFailureTest() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class);

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // invalid mainObjRelationshipPropertyName
          jdbcTemplateMapper.toOneForList(orders, Customer.class, "xyz", "customerId");
        });

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // invalid mainObjJoinPropertyName
          jdbcTemplateMapper.toOneForList(orders, Customer.class, "customer", "customerIdx");
        });

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // type of mainOjJoinPropertyName has to be Integer or Long
          jdbcTemplateMapper.toOneForList(orders, Customer.class, "customer", "status");
        });
  }

  @Test
  public void toOneMapperForObject_Test() {
    // query which gets order and related customer
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.orders o"
            + " join jdbctemplatemapper.customer c on o.customer_id = c.id"
            + " where o.id = ?";

    Integer orderId = 1;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcTemplateMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  "customer",
                  "customerId");
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }

  @Test
  public void toOneMapperForObject_NullRelationshipTest() {
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.customer c on o.customer_id = c.id"
            + " where o.id = ?";

    // order 3 has null customer
    Integer orderId = 3;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcTemplateMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  "customer",
                  "customerId");
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertNull(order.getCustomer());
  }

  @Test
  public void toOneMapperForObject_NoRecordTest() {
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.customer c on o.customer_id = c.id"
            + " where o.id = ?";

    // order 999 does not exist
    Integer orderId = 999;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcTemplateMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  "customer",
                  "customerId");
            });

    assertNull(order);
  }

  @Test
  public void toOneMapperForList_Test() {
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.customer c on o.customer_id = c.id"
            + " order by o.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.toOneMapperForList(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  "customer",
                  "customerId");
            });

    assertTrue(orders.size() >= 3);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
    assertNull(orders.get(2).getCustomer()); // order 3 has null customer
  }

  @Test
  public void toOneMapperForList_NoRecordTest() {

    //  This query returns no records
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.orders o"
            + " left join customer c on o.customer_id = c.id"
            + " where o.id in (998, 999)"
            + " order by o.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.toOneMapperForList(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  "customer",
                  "customerId");
            });

    assertEquals(0, orders.size());
  }

  @Test
  public void toOneMerge_Test() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class, "order by id");

    List<Integer> customerIds =
        orders.stream().map(Order::getCustomerId).collect(Collectors.toList());

    String sql = "select * from jdbctemplatemapper.customer where id in (:customerIds)";

    RowMapper<Customer> mapper = BeanPropertyRowMapper.newInstance(Customer.class);
    MapSqlParameterSource params = new MapSqlParameterSource("customerIds", customerIds);
    List<Customer> customers = npJdbcTemplate.query(sql, params, mapper);

    jdbcTemplateMapper.toOneMerge(orders, customers, "customer", "customerId");

    assertTrue(orders.size() >= 2);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
  }

  @Test
  public void toOneMerge_NullArgsTest() {

    List<Order> orders = null;
    List<Customer> customers = null;
    jdbcTemplateMapper.toOneMerge(orders, customers, "customer", "customerId");

    assertNull(orders);
  }

  @Test
  public void toManyForObject_Test() {
    Order order = jdbcTemplateMapper.findById(1, Order.class);

    // This issues a query to get the orderlines
    jdbcTemplateMapper.toManyForObject(
        order, OrderLine.class, "orderLines", "orderId", "order by id");

    assertEquals(1, order.getOrderLines().get(0).getProductId());
    assertEquals(2, order.getOrderLines().get(1).getProductId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }

  @Test
  public void toManyForObject_NoManyRecordsTest() {

    // Order 3 has no orderLines
    Order order = jdbcTemplateMapper.findById(3, Order.class);

    jdbcTemplateMapper.toManyForObject(
        order, OrderLine.class, "orderLines", "orderId", "order by id");

    assertNull(order.getOrderLines());
  }

  @Test
  public void toManyForList_Test() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class, "order by id");

    // This issues a query to get the orderlines
    jdbcTemplateMapper.toManyForList(
        orders, OrderLine.class, "orderLines", "orderId", "order by id");

    assertTrue(orders.size() >= 3);
    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProductId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProductId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
    assertNull(orders.get(2).getOrderLines());
  }

  @Test
  public void toManyForList_InvalidArgumentsFailureTest() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class);

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // invalid mainObj property
          jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "orderLinex", "orderId");
        });

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // mainObjCollectionPropertyName has to be of type List
          jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "person", "orderId");
        });

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // invalid manySideJoinPropertyName
          jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "orderLines", "orderIdx");
        });

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          //  manySideJoinPropert cannot be id
          jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "orderLines", "id");
        });

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          // manySideJoinPropertyName has to be Integer/Long
          jdbcTemplateMapper.toManyForList(orders, OrderLine.class, "orderLines", "status");
        });
  }

  @Test
  public void toManyForList_NoRecordsTest() {
    // mimick no order results
    List<Order> orders = null;

    // This issues a query to get the orderlines
    jdbcTemplateMapper.toManyForList(
        orders, OrderLine.class, "orderLines", "orderId", "order by id");

    assertNull(orders);
  }

  @Test
  public void toManyMerge_Test() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class, "order by id");

    List<Integer> orderIds = orders.stream().map(Order::getCustomerId).collect(Collectors.toList());

    String sql = "select * from jdbctemplatemapper.order_line where order_id in (:orderIds)";

    RowMapper<OrderLine> mapper = BeanPropertyRowMapper.newInstance(OrderLine.class);
    MapSqlParameterSource params = new MapSqlParameterSource("orderIds", orderIds);
    List<OrderLine> orderLines = npJdbcTemplate.query(sql, params, mapper);

    jdbcTemplateMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");

    assertTrue(orders.size() >= 3);
    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProductId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProductId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());

    assertNull(orders.get(2).getOrderLines());
  }

  @Test
  public void toManyMerge_NoRecordsTest() {
    List<Order> orders = jdbcTemplateMapper.findAll(Order.class, "order by id");

    // mimick no orderLines
    List<OrderLine> orderLines = null;
    jdbcTemplateMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");

    assertTrue(orders.size() >= 3);
    assertNull(orders.get(0).getOrderLines());
    assertNull(orders.get(1).getOrderLines());
  }

  @Test
  public void toManyMerge_NoManyRecordsTest() {

    // mimick no orders and orderlines
    List<Order> orders = null;
    List<OrderLine> orderLines = null;
    jdbcTemplateMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");

    assertNull(orders);
  }

  @Test
  public void toManyMapperForObject_Test() {

    // Query to get order and related orderLines
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.orders o"
            + " join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " where o.id = ?"
            + " order by ol.id";

    Integer orderId = 1;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcTemplateMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  "orderLines",
                  "orderId");
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertEquals(1, order.getOrderLines().get(0).getProductId());
    assertEquals(2, order.getOrderLines().get(1).getProductId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }

  @Test
  public void toManyMapperForObject_NoManyRecordTest() {

    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " where o.id = ?"
            + " order by ol.id";

    // order 3 has no orderLines
    Integer orderId = 3;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcTemplateMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  "orderLines",
                  "orderId");
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertNull(order.getOrderLines());
  }

  @Test
  public void toManyMapperForObject_NoRecordTest() {

    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " where o.id = ?"
            + " order by ol.id";

    // invalid order number will return no records
    Integer orderId = 999;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcTemplateMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  "orderLines",
                  "orderId");
            });

    assertNull(order);
  }

  @Test
  public void toManyMapperForList_Test() {

    // Query to get a list of orders and their related orderLines
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.orders o"
            + " join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " order by ol.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.toManyMapperForList(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  "orderLines",
                  "orderId");
            });

    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }

  @Test
  public void toManyMapperForList_NoRecordTest() {

    // Query returns no records
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.orders o"
            + " join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " where o.id in (998, 999)"
            + " order by ol.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.toManyMapperForList(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  "orderLines",
                  "orderId");
            });

    assertEquals(0, orders.size());
  }

  @Test
  @SuppressWarnings("all")
  public void multipleModelMapper_Test() {
    // query gets:
    // 1) order toMany orderLines
    // 2) order toOne customer
    // 3) orderline toOne product

    String sql =
        "select "
            + jdbcTemplateMapper.selectCols("orders", "o")
            + jdbcTemplateMapper.selectCols("order_line", "ol")
            + jdbcTemplateMapper.selectCols("customer", "c")
            + jdbcTemplateMapper.selectCols("product", "p", false)
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " left join jdbctemplatemapper.customer c on o.customer_id = c.id"
            + " left join jdbctemplatemapper.product p on ol.product_id = p.id"
            + " where o.id in (1, 2, 3)"
            + " order by o.id, ol.id";

    // the map key is the SelectMapper() prefix
    Map<String, List> resultMap =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.multipleModelMapper(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  new SelectMapper<Product>(Product.class, "p_"));
            });

    List<Order> orders = resultMap.get("o_");
    List<OrderLine> orderLines = resultMap.get("ol_");
    List<Customer> customers = resultMap.get("c_");
    List<Product> products = resultMap.get("p_");

    System.out.println(orders);
    assertEquals(3, orders.size());
    assertEquals(3, orderLines.size());
    assertEquals(2, customers.size());
    assertEquals(3, products.size());

    // Stitch together the objects to get a fully populated order object

    // tie orders to orderlines (toMany relationship)
    jdbcTemplateMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");
    // tie orders to customer (toOne relationship)
    jdbcTemplateMapper.toOneMerge(orders, customers, "customer", "customerId");
    // tie orderLines to product (toOne relationship)
    jdbcTemplateMapper.toOneMerge(orderLines, products, "product", "productId");

    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProduct().getId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProduct().getId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());

    assertEquals(3, orders.get(1).getOrderLines().get(0).getProduct().getId());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }

  @Test
  @SuppressWarnings("all")
  public void multipleModelMapper_NoRecordsTest() {

    // query returns no records
    String sql =
        "select "
            + jdbcTemplateMapper.selectCols("orders", "o")
            + jdbcTemplateMapper.selectCols("order_line", "ol")
            + jdbcTemplateMapper.selectCols("customer", "c")
            + jdbcTemplateMapper.selectCols("product", "p", false)
            + " from jdbctemplatemapper.orders o"
            + " left join jdbctemplatemapper.order_line ol on o.id = ol.order_id"
            + " left join jdbctemplatemapper.customer c on o.customer_id = c.id"
            + " left join jdbctemplatemapper.product p on ol.product_id = p.id"
            + " where o.id in (998,999)";

    Map<String, List> resultMap =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.multipleModelMapper(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"),
                  new SelectMapper<Customer>(Customer.class, "c_"),
                  new SelectMapper<Product>(Product.class, "p_"));
            });

    List<Order> orders = resultMap.get("o_");
    List<OrderLine> orderLines = resultMap.get("ol_");
    List<Customer> customers = resultMap.get("c_");
    List<Product> products = resultMap.get("p_");

    assertEquals(0, orders.size());
    assertEquals(0, orderLines.size());
    assertEquals(0, customers.size());
    assertEquals(0, products.size());
  }

  @Test
  public void selectCol_InvalidTableNameFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.selectCols("aaaaaaaaa", "a");
        });
  }

  @Test
  public void insert_TypeCheckTest() {
    TypeCheck obj = new TypeCheck();
    
    obj.setLocalDateData(LocalDate.now());   
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    
    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    }
    else {
    	obj.setJavaUtilDateTsData(new Date());
    }

    jdbcTemplateMapper.insert(obj);
    
    TypeCheck tc = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());
    System.out.println(tc.getBigDecimalData());
    System.out.println(obj.getBigDecimalData());
    assertTrue(tc.getBigDecimalData().compareTo(obj.getBigDecimalData()) == 0);
    
    if (jdbcDriver.contains("sqlserver")) {
      assertNotNull(tc.getJavaUtilDateDtData());
    }
    else {
    	assertNotNull(tc.getJavaUtilDateTsData());
    }
  }
  
  @Test
  public void update_TypeCheckTest(){
    TypeCheck obj = new TypeCheck();
    
    obj.setLocalDateData(LocalDate.now());   
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    
    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    }
    else {
    	obj.setJavaUtilDateTsData(new Date());
    }

    jdbcTemplateMapper.insert(obj);
    
  
    TypeCheck tc = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
    
    TypeCheck tc1 = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
    
    
    Instant instant = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDay = Date.from(instant);
    
    Instant instant1 = LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDayDateTime = Date.from(instant1);
    
    tc1.setLocalDateData(LocalDate.now().plusDays(1));   
    tc1.setJavaUtilDateData(nextDay);
    tc1.setLocalDateTimeData(LocalDateTime.now().plusDays(1));
    tc1.setBigDecimalData(new BigDecimal("11.34"));
    if (jdbcDriver.contains("sqlserver")) {
        tc1.setJavaUtilDateDtData(nextDayDateTime);
      }
      else {
      	tc1.setJavaUtilDateTsData(nextDayDateTime);
      }
    
    jdbcTemplateMapper.update(tc1);
    
    
    TypeCheck tc2 = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
   
    assertTrue(tc2.getLocalDateData().isAfter(tc.getLocalDateData()));
    assertTrue(tc2.getJavaUtilDateData().getTime() > tc.getJavaUtilDateData().getTime());
    assertTrue(tc2.getLocalDateTimeData().isAfter(tc.getLocalDateTimeData()));
    assertTrue(tc2.getBigDecimalData().compareTo(new BigDecimal("11.34")) == 0);
    
    if (jdbcDriver.contains("sqlserver")) {
    assertTrue(tc2.getJavaUtilDateDtData().getTime() > tc.getJavaUtilDateDtData().getTime());
    }
    else {
    	assertTrue(tc2.getJavaUtilDateTsData().getTime() > tc.getJavaUtilDateTsData().getTime());
    }
  }
  
  @Test
  public void selectCol_TypeCheckQueryTest(){
    TypeCheck obj = new TypeCheck();
    
    obj.setLocalDateData(LocalDate.now());   
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    
    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    }
    else {
    	obj.setJavaUtilDateTsData(new Date());
    }

    jdbcTemplateMapper.insert(obj);
    
    String sql = "select" + jdbcTemplateMapper.selectCols("type_check", "tc", false)
      + " from type_check tc where tc.id = ?";
    
    
    
   
    Map<String, List> resultMap =
            jdbcTemplate.query(
                sql,
                new Object[] {obj.getId()}, // args
                new int[] {java.sql.Types.INTEGER}, // arg types
                rs -> {
                  return jdbcTemplateMapper.multipleModelMapper(
                      rs,
                      new SelectMapper<TypeCheck>(TypeCheck.class, "tc_"));
                });
    List<TypeCheck> tcList = resultMap.get("tc_");
    
    TypeCheck tc = tcList.get(0);
    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());
    assertTrue(tc.getBigDecimalData().compareTo(new BigDecimal("10.23")) == 0);
    
    if (jdbcDriver.contains("sqlserver")) {
      assertNotNull(tc.getJavaUtilDateDtData());
    }
    else {
    	assertNotNull(tc.getJavaUtilDateTsData());
    }
  
  }
}
