package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import org.jdbctemplatemapper.core.Id;
import org.jdbctemplatemapper.core.IdType;
import org.jdbctemplatemapper.core.Table;

@Table(name = "person")
public class Person2 {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer personId;

  private String lastName;
  private String firstName;

  private LocalDateTime createdOn;
  private String createdBy;
  private LocalDateTime updatedOn;
  private String updatedBy;

  private Integer version;

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

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }
}
