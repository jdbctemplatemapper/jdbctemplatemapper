package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdbctemplatemapper.model.CustomerLong;
import org.jdbctemplatemapper.model.OrderLineLong;
import org.jdbctemplatemapper.model.OrderLong;
import org.jdbctemplatemapper.model.ProductLong;
import org.junit.jupiter.api.BeforeEach;
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
public class JdbcTemplateMapperLongTest {
  @Autowired 
  private JdbcTemplateMapper jdbcTemplateMapper;

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
  public void insert_LongTest(){
    OrderLong order = new OrderLong();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerLongId(2L);

    jdbcTemplateMapper.insert(order);

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
  public void insertWithId_Test() {
    ProductLong product = new ProductLong();
    product.setId(1000000000001L);
    product.setName("hat");
    product.setCost(12.25);

    jdbcTemplateMapper.insertWithId(product);

    product = jdbcTemplateMapper.findById(1000000000001L, ProductLong.class);
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
  public void update_LongTest(){
    OrderLong order = jdbcTemplateMapper.findById(1L, OrderLong.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    order.setStatus("IN PROCESS");

    jdbcTemplateMapper.update(order);

    order = jdbcTemplateMapper.findById(1L, OrderLong.class); // requery
    assertEquals("IN PROCESS", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void update_LongPropertyTest(){
    OrderLong order = jdbcTemplateMapper.findById(2L, OrderLong.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    order.setStatus("COMPLETE");
    jdbcTemplateMapper.update(order, "status");

    order = jdbcTemplateMapper.findById(2L, OrderLong.class); // requery
    assertEquals("COMPLETE", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }

  @Test
  public void findById_LongTest(){
    OrderLong order = jdbcTemplateMapper.findById(1L, OrderLong.class);

    assertNotNull(order.getId());
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getCreatedBy());
    assertNotNull(order.getCreatedOn());
    assertNotNull(order.getUpdatedBy());
    assertNotNull(order.getUpdatedOn());
    assertNotNull(order.getVersion());
  }

  @Test
  public void findAll_LongTest(){
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class);
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
  public void findAll_WithOrderByClauseLongTest(){
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class, "order by id");

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
  public void deleteByObject_LongTest(){
    ProductLong product = jdbcTemplateMapper.findById(4L, ProductLong.class);

    int cnt = jdbcTemplateMapper.delete(product);

    assertTrue(cnt == 1);

    ProductLong product1 = jdbcTemplateMapper.findById(4L, ProductLong.class);

    assertNull(product1);
  }

  @Test
  public void deleteById_LongTest(){
    int cnt = jdbcTemplateMapper.deleteById(5L, ProductLong.class);

    assertTrue(cnt == 1);

    ProductLong product1 = jdbcTemplateMapper.findById(5L, ProductLong.class);

    assertNull(product1);
  }

  @Test
  public void toOneForObject_LongTest(){
    OrderLong order = jdbcTemplateMapper.findById(1L, OrderLong.class);
    // this method issues a query behind the scenes to populate customer
    jdbcTemplateMapper.toOneForObject(order,  "customer", "customerLongId");

    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }
  
  @Test
  public void toOneForList_LongTest(){
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class, "order by id");

    // this method issues a query behind the scenes to populate customer
    jdbcTemplateMapper.toOneForList(orders, "customer", "customerLongId");

    assertTrue(orders.size() >= 2);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
  }
  
  @Test
  public void toOneMapperForObject_LongTest(){
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_long_id o_customer_long_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order_long o"
            + " join customer_long c on o.customer_long_id = c.id"
            + " where o.id = ?";

    Long orderId = 1L;

    OrderLong order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.BIGINT}, // arg types
            rs -> {
              return jdbcTemplateMapper.toOneMapperForObject(
                  rs,
                  new SelectMapper<OrderLong>(OrderLong.class, "o_"),    
                  new SelectMapper<CustomerLong>(CustomerLong.class, "c_"),
                  "customer",
                  "customerLongId");
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertEquals("tony", order.getCustomer().getFirstName());
    assertEquals("joe", order.getCustomer().getLastName());
  }

  @Test
  public void toOneMapperForList_LongTest(){
    String sql =
        "select o.id o_id, o.order_date o_order_date, o.customer_long_id o_customer_long_id,"
            + " c.id c_id, c.first_name c_first_name, c.last_name c_last_name"
            + " from jdbctemplatemapper.order_long o"
            + " join customer_long c on o.customer_long_id = c.id"
            + " where o.id in (1,2)"
            + " order by o.id";

    List<OrderLong> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.toOneMapperForList(
                  rs,
                  new SelectMapper<OrderLong>(OrderLong.class, "o_"),
                  new SelectMapper<CustomerLong>(CustomerLong.class, "c_"),
                  "customer",
                  "customerLongId");
            });

    assertEquals(2, orders.size());
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
  }


  
  @Test
  public void toOneMerge_LongTest(){
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class, "order by id");

    List<Long> customerIds =
        orders.stream().map(OrderLong::getCustomerLongId).collect(Collectors.toList());

    String sql = "select * from customer where id in (:customerIds)";

    RowMapper<CustomerLong> mapper = BeanPropertyRowMapper.newInstance(CustomerLong.class);
    MapSqlParameterSource params = new MapSqlParameterSource("customerIds", customerIds);
    List<CustomerLong> customers = npJdbcTemplate.query(sql, params, mapper);

    jdbcTemplateMapper.toOneMerge(orders, customers, "customer", "customerLongId");

    assertTrue(orders.size() >= 2);
    assertEquals("tony", orders.get(0).getCustomer().getFirstName());
    assertEquals("doe", orders.get(1).getCustomer().getLastName());
  }
  
  @Test
  public void toManyForObject_LongTest(){
    OrderLong order = jdbcTemplateMapper.findById(1L, OrderLong.class);

    // This issues a query to get the orderlines
    jdbcTemplateMapper.toMany(order, "orderLines", "orderLongId", "order by id");

    assertEquals(1, order.getOrderLines().get(0).getProductLongId());
    assertEquals(2, order.getOrderLines().get(1).getProductLongId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }
  
  @Test
  public void toManyForList_LongTest(){
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class, "order by id");

    // This issues a query to get the orderlines
    jdbcTemplateMapper.toMany(orders, "orderLines", "orderLongId", "order by id");

    assertTrue(orders.size() >= 2);
    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProductLongId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProductLongId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }

  @Test
  public void toManyMapperForObject_LongTest(){

    // Query to get order and related orderLines
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_long_id ol_order_long_id, ol.product_long_id ol_product_long_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order_long o"
            + " join order_line_long ol on o.id = ol.order_long_id"
            + " where o.id = ?"
            + " order by ol.id";

    Long orderId = 1L;

    OrderLong order =
        jdbcTemplate.query(
            sql,
            new Object[] {orderId}, // args
            new int[] {java.sql.Types.BIGINT}, // arg types
            rs -> {
              return jdbcTemplateMapper.toManyMapperForObject(
                  rs,
                  new SelectMapper<OrderLong>(OrderLong.class, "o_"),
                  new SelectMapper<OrderLineLong>(OrderLineLong.class, "ol_"),
                  "orderLines",
                  "orderLongId");
            });

    assertNotNull(order);
    assertNotNull(order.getOrderDate());
    assertEquals(1, order.getOrderLines().get(0).getProductLongId());
    assertEquals(2, order.getOrderLines().get(1).getProductLongId());
    assertEquals(10, order.getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, order.getOrderLines().get(1).getNumOfUnits());
  }

  @Test
  public void toManyMapperForList_LongTest(){

    // Query to get list of orders and their related orderLines
    String sql =
        "select o.id o_id, o.order_date o_order_date,"
            + " ol.id ol_id, ol.order_long_id ol_order_long_id, ol.product_long_id ol_product_long_id, ol.num_of_units ol_num_of_units"
            + " from jdbctemplatemapper.order_long o"
            + " join order_line_long ol on o.id = ol.order_long_id"
            + " order by ol.id";

    List<OrderLong> orders =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.toManyMapperForList(
                  rs,
                  new SelectMapper<OrderLong>(OrderLong.class, "o_"),
                  new SelectMapper<OrderLineLong>(OrderLineLong.class, "ol_"),
                  "orderLines",
                  "orderLongId");
            });

    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }



  @Test
  public void toManyMerge_LongTest(){
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class, "order by id");

    List<Long> orderIds =
        orders.stream().map(OrderLong::getCustomerLongId).collect(Collectors.toList());

    String sql = "select * from order_line_long where order_long_id in (:orderIds)";

    RowMapper<OrderLineLong> mapper = BeanPropertyRowMapper.newInstance(OrderLineLong.class);
    MapSqlParameterSource params = new MapSqlParameterSource("orderIds", orderIds);
    List<OrderLineLong> orderLines = npJdbcTemplate.query(sql, params, mapper);

    jdbcTemplateMapper.toManyMerge(orders, orderLines, "orderLines", "orderLongId");

    assertTrue(orders.size() >= 2);
    assertEquals(2, orders.get(0).getOrderLines().size());
    assertEquals(1, orders.get(1).getOrderLines().size());

    assertEquals(1, orders.get(0).getOrderLines().get(0).getProductLongId());
    assertEquals(2, orders.get(0).getOrderLines().get(1).getProductLongId());
    assertEquals(10, orders.get(0).getOrderLines().get(0).getNumOfUnits());
    assertEquals(5, orders.get(0).getOrderLines().get(1).getNumOfUnits());
    assertEquals(1, orders.get(1).getOrderLines().get(0).getNumOfUnits());
  }

  @Test
  @SuppressWarnings("all")
  public void multipleModelMapper_LongTest(){

    
     // query gets:
     // 1) order toMany orderLines
     // 2) order toOne customer
     // 3) orderline toOne product

    String sql =
        "select "
            + jdbcTemplateMapper.selectAllCols("order_long", "o")
            + jdbcTemplateMapper.selectAllCols("order_line_long", "ol")
            + jdbcTemplateMapper.selectAllCols("customer_long", "c")
            + jdbcTemplateMapper.selectAllCols("product_long", "p", false)
            + " from jdbctemplatemapper.order_long o"
            + " left join order_line_long ol on o.id = ol.order_long_id"
            + " join customer_long c on o.customer_long_id = c.id"
            + " join product_long p on ol.product_long_id = p.id"
            + " where o.id in (1, 2)"
            + " order by o.id, ol.id";


    Map<String, List> resultMap =
        jdbcTemplate.query(
            sql,
            rs -> {
              return jdbcTemplateMapper.multipleModelMapper(
                  rs,
                  new SelectMapper<OrderLong>(OrderLong.class, "o_"),
                  new SelectMapper<OrderLineLong>(OrderLineLong.class, "ol_"),
                  new SelectMapper<CustomerLong>(CustomerLong.class, "c_"),
                  new SelectMapper<ProductLong>(ProductLong.class, "p_"));
            });

    List<OrderLong> orders = resultMap.get("o_");
    List<OrderLineLong> orderLines = resultMap.get("ol_");
    List<CustomerLong> customers = resultMap.get("c_");
    List<ProductLong> products = resultMap.get("p_");

    assertEquals(2, orders.size());
    assertEquals(3, orderLines.size());
    assertEquals(2, customers.size());
    assertEquals(3, products.size());

    // Stitch together the objects to get a fully populated order object

    // tie orders to orderlines (toMany relationship)
    jdbcTemplateMapper.toManyMerge(orders, orderLines, "orderLines", "orderLongId");
    // tie orders to customer (toOne relationship)
    jdbcTemplateMapper.toOneMerge(orders, customers, "customer", "customerLongId");
    // tie orderLines to product (toOne relationship)
    jdbcTemplateMapper.toOneMerge(orderLines, products, "product", "productLongId");

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
