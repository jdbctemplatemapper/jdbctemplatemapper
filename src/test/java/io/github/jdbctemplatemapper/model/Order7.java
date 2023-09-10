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

// non default naming
@Table(name = "orders")
public class Order7 {
  @Id(type = IdType.AUTO_INCREMENT)
  @Column(name = "order_id")
  private Long id;

  @Column private LocalDateTime orderDate;

  @Column(name = "customer_id")
  private Integer custId;

  @Column private String status;

  @CreatedOn private LocalDateTime createdOn;

  @CreatedBy private String createdBy;

  @UpdatedOn private LocalDateTime updatedOn;

  @UpdatedBy private String updatedBy;

  @Version private Integer version;

  private Customer7 customer;

  private List<OrderLine7> orderLines = new ArrayList<>();

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

  public Integer getCustId() {
    return custId;
  }

  public void setCustId(Integer custId) {
    this.custId = custId;
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

  public Customer7 getCustomer() {
    return customer;
  }

  public void setCustomer(Customer7 customer) {
    this.customer = customer;
  }

  public List<OrderLine7> getOrderLines() {
    return orderLines;
  }

  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  private Person person;
}
