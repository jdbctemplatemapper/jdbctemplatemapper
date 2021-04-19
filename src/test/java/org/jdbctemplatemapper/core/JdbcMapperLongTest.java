package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.jdbctemplatemapper.model.Order;
import org.jdbctemplatemapper.model.OrderLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcMapperLongTest {
  @Autowired private JdbcMapper jdbcMapper;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private NamedParameterJdbcTemplate npJdbcTemplate;
	
  
  @Test
  public void insertLongTest() throws Exception {
    OrderLong order = new OrderLong();
    order.setOrderDate(LocalDateTime.now());
    order.setCustomerId(2L);

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
    OrderLong order = jdbcMapper.findById(1L, OrderLong.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    order.setStatus("IN PROCESS");

    jdbcMapper.update(order);

    order = jdbcMapper.findById(1L, OrderLong.class); // requery
    assertEquals("IN PROCESS", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }
  
  
  @Test
  public void updatePropertyTest() throws Exception {
    OrderLong order = jdbcMapper.findById(2L, OrderLong.class);
    LocalDateTime prevUpdatedOn = order.getUpdatedOn();

    order.setStatus("COMPLETE");
    jdbcMapper.update(order, "status");

    order = jdbcMapper.findById(2L, OrderLong.class); // requery
    assertEquals("COMPLETE", order.getStatus());
    assertEquals(2, order.getVersion()); // version incremented
    assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
    assertEquals("tester", order.getUpdatedBy());
  }
  
  @Test
  public void findByIdLongTest() throws Exception {
    OrderLong order = jdbcMapper.findById(1L, OrderLong.class);

    assertNotNull(order.getId());
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getCreatedBy());
    assertNotNull(order.getCreatedOn());
    assertNotNull(order.getUpdatedBy());
    assertNotNull(order.getUpdatedOn());
    assertNotNull(order.getVersion());
  }
  
  @Test
  public void findAllLongTest() throws Exception {
    List<OrderLong> orders = jdbcMapper.findAll(OrderLong.class);
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
  
}
