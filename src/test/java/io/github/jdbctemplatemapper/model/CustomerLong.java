package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;

public class CustomerLong {
	@Id(type=IdType.AUTO_INCREMENT)
  private Long customerId;
  private String firstName;
  private String lastName;

  public Long getCustomerId() {
    return customerId;
  }

  public void setId(Long id) {
    this.customerId = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
}
