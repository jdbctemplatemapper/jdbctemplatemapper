package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;

public class OrderLineLong {
  @Id(type = IdType.AUTO_INCREMENT)
  private Long orderLineId;

  private Long orderLongId;
  private Long productLongId;
  private Integer numOfUnits;

  /** *********************************** */
  private OrderLong order;

  private ProductLong product;

  public Long getOrderLineId() {
    return orderLineId;
  }

  public void setOrderLineId(Long id) {
    this.orderLineId= id;
  }

  public Long getOrderLongId() {
    return orderLongId;
  }

  public void setOrderLongId(Long orderLongId) {
    this.orderLongId = orderLongId;
  }

  public Long getProductLongId() {
    return productLongId;
  }

  public void setProductLongId(Long productLongId) {
    this.productLongId = productLongId;
  }

  public Integer getNumOfUnits() {
    return numOfUnits;
  }

  public void setNumOfUnits(Integer numOfUnits) {
    this.numOfUnits = numOfUnits;
  }

  public OrderLong getOrder() {
    return order;
  }

  public void setOrder(OrderLong order) {
    this.order = order;
  }

  public ProductLong getProduct() {
    return product;
  }

  public void setProduct(ProductLong product) {
    this.product = product;
  }
}