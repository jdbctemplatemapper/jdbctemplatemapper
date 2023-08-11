package io.github.jdbctemplatemapper.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "type_check")
public class TypeCheck {
	@Id(type = IdType.AUTO_INCREMENT)
	private Integer id;

	private LocalDate localDateData;
	private java.util.Date javaUtilDateData;
	private LocalDateTime localDateTimeData;

	private java.util.Date javaUtilDateTsData; // postgres/mysql/oracle
	private java.util.Date javaUtilDateDtData; // SqlServer
	private BigDecimal bigDecimalData;
	private OffsetDateTime offsetDateTimeData;
	
	private Boolean booleanVal;
	
	private byte[] image;

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

	public Boolean getBooleanVal() {
		return booleanVal;
	}

	public void setBooleanVal(Boolean booleanVal) {
		this.booleanVal = booleanVal;
	}

	public byte[] getImage() {
		return image;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}

	public OffsetDateTime getOffsetDateTimeData() {
		return offsetDateTimeData;
	}

	public void setOffsetDateTimeData(OffsetDateTime offsetDateTimeData) {
		this.offsetDateTimeData = offsetDateTimeData;
	}


}
