package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import java.util.List;

public class OrderLong {
  private Long id;
  private LocalDateTime orderDate;
  private Long customerLongId;
  private String status;
  private LocalDateTime createdOn;
  private String createdBy;
  private LocalDateTime updatedOn;
  private String updatedBy;

  private Integer version;

  private CustomerLong customer;
  private List<OrderLineLong> orderLines;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public LocalDateTime getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(LocalDateTime orderDate) {
    this.orderDate = orderDate;
  }

  public Long getCustomerLongId() {
    return customerLongId;
  }

  public void setCustomerLongId(Long customerLongId) {
    this.customerLongId = customerLongId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(LocalDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(LocalDateTime updatedOn) {
    this.updatedOn = updatedOn;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public CustomerLong getCustomer() {
    return customer;
  }

  public void setCustomer(CustomerLong customer) {
    this.customer = customer;
  }

  public List<OrderLineLong> getOrderLines() {
    return orderLines;
  }

  public void setOrderLines(List<OrderLineLong> orderLines) {
    this.orderLines = orderLines;
  }
}
