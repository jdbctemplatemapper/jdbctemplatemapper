package org.jdbctemplatemapper.model;

import org.jdbctemplatemapper.annotation.Id;
import org.jdbctemplatemapper.annotation.IdType;

public class OrderLineLong {
  @Id(type = IdType.AUTO_INCREMENT)
  private Long id;

  private Long orderLongId;
  private Long productLongId;
  private Integer numOfUnits;

  /** *********************************** */
  private OrderLong order;

  private ProductLong product;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
