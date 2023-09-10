package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.SelectMapper;
import io.github.jdbctemplatemapper.model.TypeCheck;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TypeCheckTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired private JdbcTemplateMapper jtm;

  @Test
  public void insert_TypeCheckTest() {
    TypeCheck obj = new TypeCheck();

    obj.setLocalDateData(LocalDate.now());
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    obj.setImage(new byte[] {10, 20, 30});
    obj.setOffsetDateTimeData(OffsetDateTime.now());

    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    } else {
      obj.setJavaUtilDateTsData(new Date());
    }

    jtm.insert(obj);

    TypeCheck tc = jtm.findById(TypeCheck.class, obj.getId());
    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());

    assertTrue(tc.getBigDecimalData().compareTo(obj.getBigDecimalData()) == 0);

    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("oracle")) {
      assertNotNull(tc.getOffsetDateTimeData());
    }

    // oracle and sqlserver need custom processing for blob so no support for blobs
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
      assertArrayEquals(obj.getImage(), tc.getImage());
    }

    // oracle and sqlserver do not support boolean
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
      assertTrue(tc.getBooleanVal());
    }

    if (jdbcDriver.contains("sqlserver")) {
      assertNotNull(tc.getJavaUtilDateDtData());
    } else {
      assertNotNull(tc.getJavaUtilDateTsData());
    }
  }

  @Test
  public void update_TypeCheckTest() {
    TypeCheck obj = new TypeCheck();

    obj.setLocalDateData(LocalDate.now());
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    obj.setImage(new byte[] {10, 20, 30});
    obj.setOffsetDateTimeData(OffsetDateTime.now());

    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    } else {
      obj.setJavaUtilDateTsData(new Date());
    }

    jtm.insert(obj);

    TypeCheck tc = jtm.findById(TypeCheck.class, obj.getId());
    TypeCheck tc1 = jtm.findById(TypeCheck.class, obj.getId());

    Instant instant =
        LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDay = Date.from(instant);

    Instant instant1 = LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDayDateTime = Date.from(instant1);

    tc1.setLocalDateData(LocalDate.now().plusDays(1));
    tc1.setJavaUtilDateData(nextDay);
    tc1.setLocalDateTimeData(LocalDateTime.now().plusDays(1));

    tc1.setOffsetDateTimeData(OffsetDateTime.now().plusDays(1));

    tc1.setBigDecimalData(new BigDecimal("11.34"));
    tc1.setBooleanVal(false);

    byte[] newImageVal = new byte[] {5};
    tc1.setImage(newImageVal);

    if (jdbcDriver.contains("sqlserver")) {
      tc1.setJavaUtilDateDtData(nextDayDateTime);
    } else {
      tc1.setJavaUtilDateTsData(nextDayDateTime);
    }

    jtm.update(tc1);

    TypeCheck tc2 = jtm.findById(TypeCheck.class, obj.getId());

    assertTrue(tc2.getLocalDateData().isAfter(tc.getLocalDateData()));
    assertTrue(tc2.getJavaUtilDateData().getTime() > tc.getJavaUtilDateData().getTime());
    assertTrue(tc2.getLocalDateTimeData().isAfter(tc.getLocalDateTimeData()));

    // OffsetDateTime currently only supported by oracle/mysql
    if (jdbcDriver.contains("oracle") || jdbcDriver.contains("mysql")) {
      assertTrue(tc2.getOffsetDateTimeData().isAfter(tc.getOffsetDateTimeData()));
    }

    assertTrue(tc2.getBigDecimalData().compareTo(new BigDecimal("11.34")) == 0);

    // oracle and sqlserver need custom processing for blob so no support for blobs.
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
      assertArrayEquals(newImageVal, tc2.getImage());
    }

    // oracle and sqlserver do not support boolean
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
      assertTrue(!tc2.getBooleanVal());
    }

    if (jdbcDriver.contains("sqlserver")) {
      assertTrue(tc2.getJavaUtilDateDtData().getTime() > tc.getJavaUtilDateDtData().getTime());
    } else {
      assertTrue(tc2.getJavaUtilDateTsData().getTime() > tc.getJavaUtilDateTsData().getTime());
    }
  }

  @Test
  public void selectMapper_test() {
    TypeCheck obj = new TypeCheck();

    obj.setLocalDateData(LocalDate.now());
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    obj.setImage(new byte[] {10, 20, 30});
    obj.setOffsetDateTimeData(OffsetDateTime.now());

    if (jdbcDriver.contains("sqlserver")) {
      obj.setJavaUtilDateDtData(new Date());
    } else {
      obj.setJavaUtilDateTsData(new Date());
    }

    jtm.insert(obj);

    SelectMapper<TypeCheck> typeCheckMapper = jtm.getSelectMapper(TypeCheck.class, "tc");

    String sql =
        "select" + typeCheckMapper.getColumnsSql() + " from type_check tc" + " where tc.id = ?";

    ResultSetExtractor<List<TypeCheck>> rsExtractor =
        new ResultSetExtractor<List<TypeCheck>>() {
          @Override
          public List<TypeCheck> extractData(ResultSet rs)
              throws SQLException, DataAccessException {
            List<TypeCheck> list = new ArrayList<>();
            while (rs.next()) {
              list.add(typeCheckMapper.buildModel(rs));
            }
            return list;
          }
        };

    List<TypeCheck> list = jtm.getJdbcTemplate().query(sql, rsExtractor, obj.getId());

    assertTrue(list.size() == 1);

    TypeCheck tc = list.get(0);

    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());

    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("oracle")) {
      assertNotNull(tc.getOffsetDateTimeData());
    }
    assertTrue(tc.getBigDecimalData().compareTo(obj.getBigDecimalData()) == 0);

    // oracle and sqlserver do not support boolean
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
      assertArrayEquals(obj.getImage(), tc.getImage());
    }

    // oracle and sqlserver do not support boolean
    if (jdbcDriver.contains("mysql") || jdbcDriver.contains("postgres")) {
      assertTrue(tc.getBooleanVal());
    }

    if (jdbcDriver.contains("sqlserver")) {
      assertNotNull(tc.getJavaUtilDateDtData());
    } else {
      assertNotNull(tc.getJavaUtilDateTsData());
    }
  }
}
