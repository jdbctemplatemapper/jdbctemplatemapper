package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedBy;
import io.github.jdbctemplatemapper.annotation.CreatedOn;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedBy;
import io.github.jdbctemplatemapper.annotation.UpdatedOn;
import io.github.jdbctemplatemapper.annotation.Version;

// has Customer2
@Table(name = "orders")
public class Order6 {
  @Id(type = IdType.AUTO_INCREMENT)
  private Long orderId;

  @Column
  private LocalDateTime orderDate;

  @Column
  private Integer customerId;

  @Column
  private String status;

  @CreatedOn
  private LocalDateTime createdOn;

  @CreatedBy
  private String createdBy;

  @UpdatedOn
  private LocalDateTime updatedOn;

  @UpdatedBy
  private String updatedBy;

  @Version
  private Integer version;

  private Customer2 customer;

  private List<OrderLine> orderLines = new ArrayList<>();

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long id) {
    this.orderId = id;
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

  public Customer2 getCustomer() {
    return customer;
  }

  public void setCustomer(Customer2 customer) {
    this.customer = customer;
  }

  public List<OrderLine> getOrderLines() {
    return orderLines;
  }

  public void setOrderLines(List<OrderLine> orderLines) {
    this.orderLines = orderLines;
  }

  public void addOrderLine(OrderLine orderLine) {
    orderLines.add(orderLine);
  }

  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  private Person person;
}
