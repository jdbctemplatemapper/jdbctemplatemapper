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
import java.util.Arrays;
import java.util.Collection;
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
    
    // check if auto assigned properties have been assigned.
    assertNotNull(order.getCreatedOn());
    assertNotNull(order.getUpdatedOn());
    assertEquals(1, order.getVersion());
    assertEquals("tester", order.getCreatedBy());
    assertEquals("tester", order.getUpdatedBy());
    
    // requery and test.
    order = jdbcTemplateMapper.findById(order.getId(), Order.class);
    assertNotNull(order.getId());
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getCreatedOn());
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

    jdbcTemplateMapper.insert(product);

    // check if auto assigned properties are assigned.
    assertNotNull(product.getCreatedOn());
    assertNotNull(product.getUpdatedOn());
    assertEquals(1, product.getVersion());
    assertEquals("tester", product.getCreatedBy());
    assertEquals("tester", product.getUpdatedBy());
    
    // requery and check
    product = jdbcTemplateMapper.findById(1001, Product.class);
    assertNotNull(product.getId());
    assertEquals("hat", product.getName());
    assertEquals(12.25, product.getCost());
    assertNotNull(product.getCreatedOn());
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
          jdbcTemplateMapper.insert(product);
        });
  }

  @Test
  public void insertWithId_nullObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.insert(null);
        });
  }

  @Test
  public void insertWithId_noIdObjectFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          NoIdObject pojo = new NoIdObject();
          pojo.setSomething("abc");
          jdbcTemplateMapper.insert(pojo);
        });
  }

  @Test
  public void update_Test() throws Exception {
    Order order = jdbcTemplateMapper.findById(1, Order.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    Thread.sleep(1000); // avoid timing issue.

    order.setStatus("IN PROCESS");

    jdbcTemplateMapper.update(order);

    // check if auto assigned properties have changed.
    assertEquals(2, order.getVersion()); 
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
    
    
    // requery and check
    order = jdbcTemplateMapper.findById(1, Order.class); 
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
    
    // check if auto assigned values of object changes
    assertTrue(order.getVersion() > prevVersion); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
    
    // requery and check
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
  @SuppressWarnings("all")
  public void multipleModelMapper_NoRecordsTest() {

    // query returns no records
    String sql =
        "select "
            + jdbcTemplateMapper.selectColumns("orders", "o")
            +","
            + jdbcTemplateMapper.selectColumns("order_line", "ol")
            +","
            + jdbcTemplateMapper.selectColumns("customer", "c")
            +","
            + jdbcTemplateMapper.selectColumns("product", "p")
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
  public void selectColumns_InvalidTableNameFailureTest() {
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          jdbcTemplateMapper.selectColumns("aaaaaaaaa", "a");
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
  @SuppressWarnings("all")
  public void selectColumns_TypeCheckQueryTest(){
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
    
    String sql = "select" + jdbcTemplateMapper.selectColumns("type_check", "tc")
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
