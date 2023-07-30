package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import org.jdbctemplatemapper.annotation.Id;
import org.jdbctemplatemapper.annotation.IdType;

public class Customer {
	@Id(type=IdType.AUTO_INCREMENT)
  private Integer id;
  private String firstName;
  private String lastName;

  // Non table fields below to test out case were database table 'customer'
  // does NOT have these auto assigned fields.
  private LocalDateTime createdOn;
  private String createdBy;
  private LocalDateTime updatedOn;
  private String updatedBy;
  private String version;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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

  public LocalDateTime getCreatedOn() {
    return createdOn;
  }

  public void setCreatedOn(LocalDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getUpdatedOn() {
    return updatedOn;
  }

  public void setUpdatedOn(LocalDateTime updatedOn) {
    this.updatedOn = updatedOn;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
