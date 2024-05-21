package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "orders")
public class OrderInheritedOverridenId extends OrderIdBaseClass {

  // The annotation in on super class and since this hides it annotation validtion should throw
  // exception
  private Long orderId;

  @Column
  private LocalDateTime orderDate;

  @Column
  private Integer customerId;

  @Column
  private String status;

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Integer getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Integer customerId) {
    this.customerId = customerId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setOrderDate(LocalDateTime orderDate) {
    this.orderDate = orderDate;
  }


}
