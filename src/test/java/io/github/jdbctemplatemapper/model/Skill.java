package io.github.jdbctemplatemapper.model;

import java.util.ArrayList;
import java.util.List;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "skill")
public class Skill {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  @Column
  private String name;

  private List<Employee> employees = new ArrayList<>();

  public Skill() {}

  public Skill(String name) {
    this.name = name;
  }

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

  public List<Employee> getEmployees() {
    return employees;
  }

  public void setEmployees(List<Employee> employees) {
    this.employees = employees;
  }
}
