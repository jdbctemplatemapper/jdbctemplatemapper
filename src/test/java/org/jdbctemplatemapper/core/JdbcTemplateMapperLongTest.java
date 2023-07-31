package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.jdbctemplatemapper.model.OrderLong;
import org.jdbctemplatemapper.model.ProductLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
/**
 * Tests where the id of object is of type Long
 * 
 * @author ajoseph
 */

public class JdbcTemplateMapperLongTest {
  @Autowired 
  private JdbcTemplateMapper jdbcTemplateMapper;
  
  @Test
  public void insert_LongTest(){
    OrderLong order = new OrderLong();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerLongId(2L);

    jdbcTemplateMapper.insert(order);

    assertNotNull(order.getOrderId());
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
    product.setProductId(1000000000001L);
    product.setName("hat");
    product.setCost(12.25);

    jdbcTemplateMapper.insert(product);

    product = jdbcTemplateMapper.findById(1000000000001L, ProductLong.class);
    assertNotNull(product.getProductId());
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

    assertNotNull(order.getOrderId());
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
      assertNotNull(orders.get(idx).getOrderId());
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
    List<OrderLong> orders = jdbcTemplateMapper.findAll(OrderLong.class, "order by order_id");

    assertTrue(orders.size() >= 2);

    for (int idx = 0; idx < orders.size(); idx++) {
      assertNotNull(orders.get(idx).getOrderId());
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


}
