package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "order_line")
public class OrderLine {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer orderLineId;

  @Column
  private Long orderId;
  @Column
  private Integer productId;
  @Column
  private int numOfUnits;

  private Order order;

  private Product product;

  private String status;

  public Integer getOrderLineId() {
    return orderLineId;
  }

  public void setOrderLineId(Integer id) {
    this.orderLineId = id;
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

  public int getNumOfUnits() {
    return numOfUnits;
  }

  public void setNumOfUnits(int numOfUnits) {
    this.numOfUnits = numOfUnits;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
