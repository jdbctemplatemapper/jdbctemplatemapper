package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.model.TypeCheck;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcTemplateMapperTypeTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired private JdbcTemplateMapper jdbcTemplateMapper;
  
  
  @Test
  public void insert_TypeCheckTest() {
    TypeCheck obj = new TypeCheck();
    
    obj.setLocalDateData(LocalDate.now());   
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    
    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    }
    else {
    	obj.setJavaUtilDateTsData(new Date());
    }

    jdbcTemplateMapper.insert(obj);
    
    TypeCheck tc = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());
    assertTrue(tc.getBigDecimalData().compareTo(obj.getBigDecimalData()) == 0);
    
    //oracle and sqlserver do not support boolean
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
    	assertTrue(tc.getBooleanVal());
    }
    

    if (jdbcDriver.contains("sqlserver")) {
      assertNotNull(tc.getJavaUtilDateDtData());
    }
    else {
    	assertNotNull(tc.getJavaUtilDateTsData());
    }
  }
  
  @Test
  public void update_TypeCheckTest(){
    TypeCheck obj = new TypeCheck();
    
    obj.setLocalDateData(LocalDate.now());   
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    
    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    }
    else {
    	obj.setJavaUtilDateTsData(new Date());
    }

    jdbcTemplateMapper.insert(obj);
    
  
    TypeCheck tc = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);    
    TypeCheck tc1 = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
    
    
    Instant instant = LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDay = Date.from(instant);
    
    Instant instant1 = LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDayDateTime = Date.from(instant1);
    
    tc1.setLocalDateData(LocalDate.now().plusDays(1));   
    tc1.setJavaUtilDateData(nextDay);
    tc1.setLocalDateTimeData(LocalDateTime.now().plusDays(1));
    tc1.setBigDecimalData(new BigDecimal("11.34"));
    tc1.setBooleanVal(false);
    
    if (jdbcDriver.contains("sqlserver")) {
        tc1.setJavaUtilDateDtData(nextDayDateTime);
      }
      else {
      	tc1.setJavaUtilDateTsData(nextDayDateTime);
      }
    
    jdbcTemplateMapper.update(tc1);  
    
    TypeCheck tc2 = jdbcTemplateMapper.findById(obj.getId(), TypeCheck.class);
   
    assertTrue(tc2.getLocalDateData().isAfter(tc.getLocalDateData()));
    assertTrue(tc2.getJavaUtilDateData().getTime() > tc.getJavaUtilDateData().getTime());
    assertTrue(tc2.getLocalDateTimeData().isAfter(tc.getLocalDateTimeData()));
    assertTrue(tc2.getBigDecimalData().compareTo(new BigDecimal("11.34")) == 0);
    
    //oracle and sqlserver do not support boolean
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
    	assertTrue(!tc2.getBooleanVal());
    }
    
    
    if (jdbcDriver.contains("sqlserver")) {
    assertTrue(tc2.getJavaUtilDateDtData().getTime() > tc.getJavaUtilDateDtData().getTime());
    }
    else {
    	assertTrue(tc2.getJavaUtilDateTsData().getTime() > tc.getJavaUtilDateTsData().getTime());
    }
  }
  
  
}
