package io.github.jdbctemplatemapper.model;

import java.util.ArrayList;
import java.util.List;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "employee")
public class Employee {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  @Column
  private String lastName;
  @Column
  private String firstName;

  private List<Skill> skills = new ArrayList<>();

  public Employee() {}

  public Employee(String firstName, String lastName) {
    this.lastName = lastName;
    this.firstName = firstName;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public List<Skill> getSkills() {
    return skills;
  }

  public void setSkills(List<Skill> skills) {
    this.skills = skills;
  }
}
