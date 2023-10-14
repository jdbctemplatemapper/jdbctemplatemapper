package io.github.jdbctemplatemapper.model;

import java.util.ArrayList;
import java.util.List;
import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "company", catalog = "schema2")
public class CompanyCatalogSchema2 {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;
  @Column
  private String name;

  List<OfficeCatalogSchema2> offices = new ArrayList<>();

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

  public List<OfficeCatalogSchema2> getOffices() {
    return offices;
  }

  public void setOffices(List<OfficeCatalogSchema2> offices) {
    this.offices = offices;
  }


}
