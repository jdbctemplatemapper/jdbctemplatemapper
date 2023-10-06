package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

// for oracle table is in SCHEMA1 and synonym in SCHEMA2
@Table(name = "testsynonym", schema = "SCHEMA2")
public class Testsynonym {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;
  @Column
  private String name;
  public Integer getId() {
    return id;
  }
  public void setId(Integer id) {
    this.id = id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  
  
}
