package org.jdbctemplatemapper.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TypeCheck {
	private Integer id;
	private LocalDate localDateData;
	private java.util.Date javaUtilDateData;
	private LocalDateTime localDateTimeData;

	private java.util.Date javaUtilDateTsData; // postgres/mysql/oracle
    private java.util.Date javaUtilDateDtData; // SqlServer
    private BigDecimal bigDecimalData;
    
  

}
