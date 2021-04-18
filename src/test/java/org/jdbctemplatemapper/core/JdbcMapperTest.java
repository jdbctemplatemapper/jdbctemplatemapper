package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  public void toOneMapperForObjectTest() throws Exception {
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
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
  public void toOneMapperListTest() throws Exception {
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order o"
            + " join customer c on o.customer_id = c.id"
            + " where o.id in (1,2)"
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

    assertEquals(2, orders.size());
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
  }

  @Test
  public void toOneTest() throws Exception {
    Order order = jdbcMapper.findById(1, Order.class);
    // this method issues a query behind the scenes to populate customer
    jdbcMapper.toOne(order, "customer", Customer.class);

    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }

  @Test
  public void toOneListTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

    // this method issues a query behind the scenes to populate customer
    jdbcMapper.toOne(orders, "customer", Customer.class);

    assertTrue(orders.size() >= 2);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
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
  public void toManyMapperListTest() throws Exception {

    // Query to get list of orders and their related orderLines
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
  public void toManyTest() throws Exception {
    Order order = jdbcMapper.findById(1, Order.class);

    // This issues a query to get the orderlines
    jdbcMapper.toMany(order, "orderLines", OrderLine.class, "order by id");

    assertEquals(1, order.getOrderLines().get(0).getProductId());
    assertEquals(2, order.getOrderLines().get(1).getProductId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }

  @Test
  public void toManyListTest() throws Exception {
    List<Order> orders = jdbcMapper.findAll(Order.class, "order by id");

    // This issues a query to get the orderlines
    jdbcMapper.toMany(orders, "orderLines", OrderLine.class, "order by id");

    assertTrue(orders.size() >= 2);
    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProductId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProductId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
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

    assertTrue(orders.size() >= 2);
    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProductId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProductId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }

  @Test
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
            + " join customer c on o.customer_id = c.id"
            + " join product p on ol.product_id = p.id"
            + " where o.id in (1, 2)";

    System.out.println(sql);

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

    assertEquals(2, orders.size());
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
}
