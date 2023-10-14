package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

// int auto increment id. Used to test for failure. Auto increment ids have to be Number objects.
@Table(name = "person")
public class Person2 {
  @Id(type=IdType.AUTO_INCREMENT) private int personId;
  @Column private String lastName;
  @Column private String firstName;

  private String someNonDatabaseProperty;

  public int getPersonId() {
    return personId;
  }

  public void setPersonId(int id) {
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

