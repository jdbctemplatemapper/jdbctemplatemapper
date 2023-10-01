package io.github.jdbctemplatemapper.model;

import java.util.ArrayList;
import java.util.List;
import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "company", schema="SCHEMA2")
public class CompanySchema2 {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;
    @Column private String name;
    
    List<OfficeSchema2> offices = new ArrayList<>();

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

    public List<OfficeSchema2> getOffices() {
      return offices;
    }

    public void setOffices(List<OfficeSchema2> offices) {
      this.offices = offices;
    }

}
