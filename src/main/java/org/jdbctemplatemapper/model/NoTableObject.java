package org.jdbctemplatemapper.model;

import org.jdbctemplatemapper.core.Id;

public class NoTableObject {
  @Id private Integer id;
  private String something;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getSomething() {
    return something;
  }

  public void setSomething(String something) {
    this.something = something;
  }
}
