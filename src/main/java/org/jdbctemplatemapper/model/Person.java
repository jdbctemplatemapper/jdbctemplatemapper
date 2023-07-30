package org.jdbctemplatemapper.model;

import org.jdbctemplatemapper.annotation.Id;
import org.jdbctemplatemapper.annotation.IdType;

public class Person {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  private String lastName;
  private String firstName;

  private String someNonDatabaseProperty;

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

  public String getSomeNonDatabaseProperty() {
    return someNonDatabaseProperty;
  }

  public void setSomeNonDatabaseProperty(String someNonDatabaseProperty) {
    this.someNonDatabaseProperty = someNonDatabaseProperty;
  }
}
