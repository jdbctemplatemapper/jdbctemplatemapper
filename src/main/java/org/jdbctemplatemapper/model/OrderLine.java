package org.jdbctemplatemapper.model;

import org.jdbctemplatemapper.annotation.Id;
import org.jdbctemplatemapper.annotation.IdType;

public class OrderLine {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  private Integer orderId;
  private Integer productId;
  private Integer numOfUnits;

  /** *********************************** */
  private Order order;

  private Product product;

  private String status;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getOrderId() {
    return orderId;
  }

  public void setOrderId(Integer orderId) {
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
