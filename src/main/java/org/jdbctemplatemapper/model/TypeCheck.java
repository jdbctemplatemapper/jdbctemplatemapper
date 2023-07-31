package org.jdbctemplatemapper.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jdbctemplatemapper.annotation.Id;
import org.jdbctemplatemapper.annotation.IdType;

public class TypeCheck {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  private LocalDate localDateData;
  private java.util.Date javaUtilDateData;
  private LocalDateTime localDateTimeData;

  private java.util.Date javaUtilDateTsData; // postgres/mysql/oracle
  private java.util.Date javaUtilDateDtData; // SqlServer
  private BigDecimal bigDecimalData;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public LocalDate getLocalDateData() {
    return localDateData;
  }

  public void setLocalDateData(LocalDate localDateData) {
    this.localDateData = localDateData;
  }

  public java.util.Date getJavaUtilDateData() {
    return javaUtilDateData;
  }

  public void setJavaUtilDateData(java.util.Date javaUtilDateData) {
    this.javaUtilDateData = javaUtilDateData;
  }

  public LocalDateTime getLocalDateTimeData() {
    return localDateTimeData;
  }

  public void setLocalDateTimeData(LocalDateTime localDateTimeData) {
    this.localDateTimeData = localDateTimeData;
  }

  public java.util.Date getJavaUtilDateTsData() {
    return javaUtilDateTsData;
  }

  public void setJavaUtilDateTsData(java.util.Date javaUtilDateTsData) {
    this.javaUtilDateTsData = javaUtilDateTsData;
  }

  public java.util.Date getJavaUtilDateDtData() {
    return javaUtilDateDtData;
  }

  public void setJavaUtilDateDtData(java.util.Date javaUtilDateDtData) {
    this.javaUtilDateDtData = javaUtilDateDtData;
  }

  public BigDecimal getBigDecimalData() {
    return bigDecimalData;
  }

  public void setBigDecimalData(BigDecimal bigDecimalData) {
    this.bigDecimalData = bigDecimalData;
  }
}
