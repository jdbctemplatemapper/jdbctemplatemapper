package org.jdbctemplatemapper.model;

import org.jdbctemplatemapper.core.Id;
import org.jdbctemplatemapper.core.IdType;

public class Person {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer personId;

  private String lastName;
  private String firstName;

  private String someNonDatabaseProperty;

  public Integer getPersonId() {
    return personId;
  }

  public void setPersonId(Integer id) {
    this.personId = id;
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
