package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Table;

@Table(name="no_id_object")
public class NoIdObject {
  private String something;

  public String getSomething() {
    return something;
  }

  public void setSomething(String something) {
    this.something = something;
  }
}
