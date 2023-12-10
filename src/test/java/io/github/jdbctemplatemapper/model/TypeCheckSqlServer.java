package io.github.jdbctemplatemapper.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "type_check")
public class TypeCheckSqlServer {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  @Column
  private LocalDate localDateData;

  @Column
  private java.util.Date javaUtilDateData;

  @Column
  private LocalDateTime localDateTimeData;


  @Column
  private java.util.Date javaUtilDateDtData; // SqlServer

  @Column
  private BigDecimal bigDecimalData;

  @Column(name = "string_enum")
  private StatusEnum status;


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

  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

}

