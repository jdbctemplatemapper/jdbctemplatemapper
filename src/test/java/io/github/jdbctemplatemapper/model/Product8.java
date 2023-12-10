package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.Table;

// No audit fields. Used to test updateProperties and its cache

@Table(name = "product")
public class Product8 {
  @Id
  private Integer productId;
  @Column
  private String name;
  @Column
  private Double cost;

  @Column
  private LocalDateTime createdOn;

  @Column
  private String createdBy;

  @Column
  private LocalDateTime updatedOn;

  @Column
  private String updatedBy;

  @Column
  private int version;

  public Integer getProductId() {
    return productId;
  }

  public void setProductId(Integer id) {
    this.productId = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getCost() {
    return cost;
  }

  public void setCost(Double cost) {
    this.cost = cost;
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

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}

