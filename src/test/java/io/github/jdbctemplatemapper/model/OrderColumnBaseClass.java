package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;

public class OrderColumnBaseClass {
  @Column
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
