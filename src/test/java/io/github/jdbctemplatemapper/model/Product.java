package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedBy;
import io.github.jdbctemplatemapper.annotation.CreatedOn;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedBy;
import io.github.jdbctemplatemapper.annotation.UpdatedOn;
import io.github.jdbctemplatemapper.annotation.Version;

@Table(name = "product")
public class Product {
  @Id
  private Integer productId;
  @Column
  private String name;
  @Column
  private Double cost;

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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
