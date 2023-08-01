package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import org.jdbctemplatemapper.core.Id;

public class ProductLong {
  @Id private Long productId;
  private String name;
  private Double cost;
  private LocalDateTime createdOn;
  private String createdBy;
  private LocalDateTime updatedOn;
  private String updatedBy;
  private Integer version;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long id) {
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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
