package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdbctemplatemapper.model.Customer;
import org.jdbctemplatemapper.model.Order;
import org.jdbctemplatemapper.model.OrderLine;
import org.jdbctemplatemapper.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcMapperTest {
  @Autowired private JdbcMapper jdbcMapper;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private NamedParameterJdbcTemplate npJdbcTemplate;

  @Test
  public void insertTest() throws Exception {
    Order order = new Order();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2);

    jdbcMapper.insert(order);

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
  public void updateTest() throws Exception {
    Order order = jdbcMapper.findById(1, Order.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    order.setStatus("IN PROCESS");

    jdbcMapper.update(order);

    order = jdbcMapper.findById(1, Order.class); // requery
    assertEquals("IN PROCESS", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void updatePropertyTest() throws Exception {
    Order order = jdbcMapper.findById(2, Order.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    order.setStatus("COMPLETE");
    jdbcMapper.update(order, "status");

    order = jdbcMapper.findById(2, Order.class); // requery
    assertEquals("COMPLETE", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void findByIdTest() throws Exception {
    Order order = jdbcMapper.findById(1, Order.class);

    assertNotNull(order.getId());
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getCreatedBy());
    assertNotNull(order.getCreatedOn());
    assertNotNull(order.getUpdatedBy());
    assertNotNull(order.getUpdatedOn());
    assertNotNull(order.getVersion());
  }

  @Test
  public void findAllTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class);
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
  public void findAllWithOrderByClauseTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

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
  public void deleteByObjectTest() throws Exception {
    Product product = jdbcMapper.findById(4, Product.class);

    int cnt = jdbcMapper.delete(product);

    assertTrue(cnt == 1);

    Product product1 = jdbcMapper.findById(4, Product.class);

    assertNull(product1);
  }

  @Test
  public void deleteByIdTest() throws Exception {
    int cnt = jdbcMapper.deleteById(5, Product.class);

    assertTrue(cnt == 1);

    Product product1 = jdbcMapper.findById(5, Product.class);

    assertNull(product1);
  }
  
  @Test
  public void toOneForObjectTest() throws Exception {
    Order order = jdbcMapper.findById(1, Order.class);
    // this method issues a query behind the scenes to populate customer
    jdbcMapper.toOneForObject(order, "customer", Customer.class);

    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }

  @Test
  public void toOneForObjectNullRelationshipTest() throws Exception {
    Order order = jdbcMapper.findById(3, Order.class);
    // order 3 has null customer
    jdbcMapper.toOneForObject(order, "customer", Customer.class);

    assertNull(order.getCustomer());
  }

  @Test
  public void toOneForObjectNoRecordTest() throws Exception {
    Order order = jdbcMapper.findById(999, Order.class);
    // order 3 has null customer
    jdbcMapper.toOneForObject(order, "customer", Customer.class);

    assertNull(order);
  }

  @Test
  public void toOneTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

    // this method issues a query behind the scenes to populate customer
    jdbcMapper.toOne(orders, "customer", Customer.class);

    assertTrue(orders.size() >= 3);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
    assertNull(orders.get(2).getCustomer()); // order 3 has null customer
  }

  @Test
  public void toOneNoRecordTest() throws Exception {
    // mimick query returning no orders
    List<Order> orders = null;
    jdbcMapper.toOne(orders, "customer", Customer.class);

    assertNull(orders);
  }

  @Test
  public void toOneMapperForObjectTest() throws Exception {
	// query which gets order and related customer
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order o"
            + " join customer c on o.customer_id = c.id"
            + " where o.id = ?";

    Integer orderId = 1;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "customer",
                  new SelectMapper<Customer>(Customer.class, "c_"));
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }

  @Test
  public void toOneMapperForObjectNullRelationshipTest() throws Exception {
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order o"
            + " left join customer c on o.customer_id = c.id"
            + " where o.id = ?";

    // order 3 has null customer
    Integer orderId = 3;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "customer",
                  new SelectMapper<Customer>(Customer.class, "c_"));
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertNull(order.getCustomer());
  }

  @Test
  public void toOneMapperForObjectNoRecordTest() throws Exception {
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order o"
            + " left join customer c on o.customer_id = c.id"
            + " where o.id = ?";

    // order 999 does not exist
    Integer orderId = 999;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "customer",
                  new SelectMapper<Customer>(Customer.class, "c_"));
            });

    assertNull(order);
  }

  @Test
  public void toOneMapperTest() throws Exception {
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order o"
            + " left join customer c on o.customer_id = c.id"
            + " order by o.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcMapper.toOneMapper(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "customer",
                  new SelectMapper<Customer>(Customer.class, "c_"));
            });

    assertTrue(orders.size() >= 3);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
    assertNull(orders.get(2).getCustomer()); // order 3 has null customer
  }

  @Test
  public void toOneMapperNoRecordTest() throws Exception {

    //  This query returns no records
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_id o_customer_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order o"
            + " left join customer c on o.customer_id = c.id"
            + " where o.id in (998, 999)"
            + " order by o.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcMapper.toOneMapper(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "customer",
                  new SelectMapper<Customer>(Customer.class, "c_"));
            });

    assertEquals(0, orders.size());
  }
  
  @Test
  public void toOneMergeTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

    List<Integer> customerIds =
        orders.stream().map(Order::getCustomerId).collect(Collectors.toList());

    String sql = "select * from customer where id in (:customerIds)";

    RowMapper<Customer> mapper = BeanPropertyRowMapper.newInstance(Customer.class);
    MapSqlParameterSource params = new MapSqlParameterSource("customerIds", customerIds);
    List<Customer> customers = npJdbcTemplate.query(sql, params, mapper);

    jdbcMapper.toOneMerge(orders, customers, "customer", "customerId");

    assertTrue(orders.size() >= 2);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
  }
  
  @Test
  public void toOneMergeNullArgsTest() throws Exception {

    List<Order> orders = null;
    List<Customer> customers = null;
    jdbcMapper.toOneMerge(orders, customers, "customer", "customerId");

    assertNull(orders);
  }
 
  @Test
  public void toManyForObjectTest() throws Exception {
    Order order = jdbcMapper.findById(1, Order.class);

    // This issues a query to get the orderlines
    jdbcMapper.toManyForObject(order, "orderLines", OrderLine.class, "order by id");

    assertEquals(1, order.getOrderLines().get(0).getProductId());
    assertEquals(2, order.getOrderLines().get(1).getProductId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }

  @Test
  public void toManyForObjectWithNoManyRecordsTest() throws Exception {

    // Order 3 has no orderLines
    Order order = jdbcMapper.findById(3, Order.class);

    jdbcMapper.toManyForObject(order, "orderLines", OrderLine.class, "order by id");

    assertNull(order.getOrderLines());
  }

  @Test
  public void toManyTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

    // This issues a query to get the orderlines
    jdbcMapper.toMany(orders, "orderLines", OrderLine.class, "order by id");

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
  public void toManyNoRecordsTest() throws Exception {
	  
	// mimick no order results
    List<Order> orders = null;

    // This issues a query to get the orderlines
    jdbcMapper.toMany(orders, "orderLines", OrderLine.class, "order by id");

    assertNull(orders);
  }

  @Test
  public void toManyMergeTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

    List<Integer> orderIds = orders.stream().map(Order::getCustomerId).collect(Collectors.toList());

    String sql = "select * from order_line where order_id in (:orderIds)";

    RowMapper<OrderLine> mapper = BeanPropertyRowMapper.newInstance(OrderLine.class);
    MapSqlParameterSource params = new MapSqlParameterSource("orderIds", orderIds);
    List<OrderLine> orderLines = npJdbcTemplate.query(sql, params, mapper);

    jdbcMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");

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
  public void toManyMergeNoRecordsTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");
    
    // mimick no orderLines
    List<OrderLine> orderLines = null;
    jdbcMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");

    assertTrue(orders.size() >= 3);
    assertNull(orders.get(0).getOrderLines());
    assertNull(orders.get(1).getOrderLines());
  }

  @Test
  public void toManyMergeNoManyRecordsTest() throws Exception {

	// mimick no orders and orderlines
    List<Order> orders = null;
    List<OrderLine> orderLines = null;
    jdbcMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");

    assertNull(orders);
  }

  @Test
  public void toManyMapperForObjectTest() throws Exception {

    // Query to get order and related orderLines
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order o"
            + " join order_line ol on o.id = ol.order_id"
            + " where o.id = ?"
            + " order by ol.id";

    Integer orderId = 1;

    Order order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.INTEGER}, // arg types
            rs -> {
              return jdbcMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "orderLines",
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"));
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertEquals(1, order.getOrderLines().get(0).getProductId());
    assertEquals(2, order.getOrderLines().get(1).getProductId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }

  @Test
  public void toManyMapperForObjectNoManyRecordTest() throws Exception {

    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order o"
            + " left join order_line ol on o.id = ol.order_id"
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
              return jdbcMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "orderLines",
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"));
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertNull(order.getOrderLines());
  }

  @Test
  public void toManyMapperForObjectNoRecordTest() throws Exception {

    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order o"
            + " left join order_line ol on o.id = ol.order_id"
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
              return jdbcMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "orderLines",
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"));
            });

    assertNull(order);
  }

  @Test
  public void toManyMapperTest() throws Exception {

    // Query to get a list of orders and their related orderLines
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order o"
            + " join order_line ol on o.id = ol.order_id"
            + " order by ol.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcMapper.toManyMapper(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "orderLines",
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"));
            });

    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }

  @Test
  public void toManyMapperNoRecordTest() throws Exception {

    // Query returns no records
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_id ol_order_id, ol.product_id ol_product_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order o"
            + " join order_line ol on o.id = ol.order_id"
            + " where o.id in (998, 999)"
            + " order by ol.id";

    List<Order> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcMapper.toManyMapper(
                  rs,
                  new SelectMapper<Order>(Order.class, "o_"),
                  "orderLines",
                  new SelectMapper<OrderLine>(OrderLine.class, "ol_"));
            });

    assertEquals(0, orders.size());
  }

  @Test
  @SuppressWarnings("all")
  public void multipleModelMapperTest() throws Exception {

    /*
     * query gets:
     * 1) order toMany orderLines
     * 2) order toOne customer
     * 3) orderline toOne product
     *
     */
    String sql =
        "select "
            + jdbcMapper.selectCols("order", "o")
            + ","
            + jdbcMapper.selectCols("order_line", "ol")
            + ","
            + jdbcMapper.selectCols("customer", "c")
            + ","
            + jdbcMapper.selectCols("product", "p")
            + " from jdbctemplatemapper.order o"
            + " left join order_line ol on o.id = ol.order_id"
            + " left join customer c on o.customer_id = c.id"
            + " left join product p on ol.product_id = p.id"
            + " where o.id in (1, 2, 3)";

    Map<String, List> resultMap =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcMapper.multipleModelMapper(
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

    assertEquals(3, orders.size());
    assertEquals(3, orderLines.size());
    assertEquals(2, customers.size());
    assertEquals(3, products.size());

    // Stitch together the objects to get a fully populated order object

    // tie orders to orderlines (toMany relationship)
    jdbcMapper.toManyMerge(orders, orderLines, "orderLines", "orderId");
    // tie orders to customer (toOne relationship)
    jdbcMapper.toOneMerge(orders, customers, "customer", "customerId");
    // tie orderLines to product (toOne relationship)
    jdbcMapper.toOneMerge(orderLines, products, "product", "productId");

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
  public void multipleModelMapperNoRecordsTest() throws Exception {

    // query returns no records
    String sql =
        "select "
            + jdbcMapper.selectCols("order", "o")
            + ","
            + jdbcMapper.selectCols("order_line", "ol")
            + ","
            + jdbcMapper.selectCols("customer", "c")
            + ","
            + jdbcMapper.selectCols("product", "p")
            + " from jdbctemplatemapper.order o"
            + " left join order_line ol on o.id = ol.order_id"
            + " left join customer c on o.customer_id = c.id"
            + " left join product p on ol.product_id = p.id"
            + " where o.id in (998,999)";

    Map<String, List> resultMap =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcMapper.multipleModelMapper(
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
}
