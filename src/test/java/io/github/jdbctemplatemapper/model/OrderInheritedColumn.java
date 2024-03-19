package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "orders")
public class OrderInheritedColumn extends OrderColumnBaseClass {
  @Id(type = IdType.AUTO_INCREMENT)
  private Long orderId;

  @Column
  private LocalDateTime orderDate;

  @Column
  private Integer customerId;

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDateTime orderDate) {
    this.orderDate = orderDate;
  }

  public Integer getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Integer customerId) {
    this.customerId = customerId;
  }


}
