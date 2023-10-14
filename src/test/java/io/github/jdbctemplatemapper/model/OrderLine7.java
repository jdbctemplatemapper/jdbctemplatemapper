package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

// non default naming
@Table(name = "order_line")
public class OrderLine7 {
  @Id(type = IdType.AUTO_INCREMENT)
  @Column(name = "order_line_id")
  private Long id;

  @Column
  private Long orderId;
  @Column
  private Integer productId;
  @Column
  private Integer numOfUnits;

  private Order order;

  private Product7 product;

  private String status;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public Integer getProductId() {
    return productId;
  }

  public void setProductId(Integer productId) {
    this.productId = productId;
  }

  public Integer getNumOfUnits() {
    return numOfUnits;
  }

  public void setNumOfUnits(Integer numOfUnits) {
    this.numOfUnits = numOfUnits;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Product7 getProduct() {
    return product;
  }

  public void setProduct(Product7 product) {
    this.product = product;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
