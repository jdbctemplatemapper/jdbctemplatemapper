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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
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
import io.github.jdbctemplatemapper.model.StatusEnum;
import io.github.jdbctemplatemapper.model.TypeCheckPostgres;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TypeCheckPostgresTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @BeforeEach
  public void beforeMethod() {
    // tests will only run if postgres
    if (!jdbcDriver.contains("postgres")) {
      Assumptions.assumeTrue(false);
    }
  }

  @Test
  public void insert_TypeCheckPostgresTest() {
    TypeCheckPostgres obj = new TypeCheckPostgres();

    obj.setLocalDateData(LocalDate.now());
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    obj.setImage(new byte[] {10, 20, 30});

    obj.setJavaUtilDateTsData(new Date());
    obj.setStatus(StatusEnum.OPEN);

    jtm.insert(obj);

    TypeCheckPostgres tc = jtm.findById(TypeCheckPostgres.class, obj.getId());
    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());

    assertTrue(tc.getBigDecimalData().compareTo(obj.getBigDecimalData()) == 0);

    assertArrayEquals(obj.getImage(), tc.getImage());


    assertTrue(tc.getBooleanVal());

    assertNotNull(tc.getJavaUtilDateTsData());

    assertTrue(StatusEnum.OPEN == tc.getStatus());
  }

  @Test
  public void update_TypeCheckPostgresTest() {
    TypeCheckPostgres obj = new TypeCheckPostgres();

    obj.setLocalDateData(LocalDate.now());
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    obj.setImage(new byte[] {10, 20, 30});

    obj.setJavaUtilDateTsData(new Date());


    jtm.insert(obj);

    TypeCheckPostgres tc = jtm.findById(TypeCheckPostgres.class, obj.getId());
    TypeCheckPostgres tc1 = jtm.findById(TypeCheckPostgres.class, obj.getId());

    Instant instant =
        LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDay = Date.from(instant);

    Instant instant1 = LocalDateTime.now().plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
    java.util.Date nextDayDateTime = Date.from(instant1);

    tc1.setLocalDateData(LocalDate.now().plusDays(1));
    tc1.setJavaUtilDateData(nextDay);
    tc1.setLocalDateTimeData(LocalDateTime.now().plusDays(1));


    tc1.setBigDecimalData(new BigDecimal("11.34"));
    tc1.setBooleanVal(false);

    byte[] newImageVal = new byte[] {5};
    tc1.setImage(newImageVal);


    tc1.setJavaUtilDateTsData(nextDayDateTime);

    tc1.setStatus(StatusEnum.CLOSED);

    jtm.update(tc1);

    TypeCheckPostgres tc2 = jtm.findById(TypeCheckPostgres.class, obj.getId());

    assertTrue(tc2.getLocalDateData().isAfter(tc.getLocalDateData()));
    assertTrue(tc2.getJavaUtilDateData().getTime() > tc.getJavaUtilDateData().getTime());
    assertTrue(tc2.getLocalDateTimeData().isAfter(tc.getLocalDateTimeData()));


    assertTrue(tc2.getBigDecimalData().compareTo(new BigDecimal("11.34")) == 0);

    assertArrayEquals(newImageVal, tc2.getImage());

    assertTrue(!tc2.getBooleanVal());

    assertTrue(tc2.getJavaUtilDateTsData().getTime() > tc.getJavaUtilDateTsData().getTime());

    assertTrue(StatusEnum.CLOSED == tc2.getStatus());
  }

  @Test
  public void selectMapper_test() {
    TypeCheckPostgres obj = new TypeCheckPostgres();

    obj.setLocalDateData(LocalDate.now());
    obj.setJavaUtilDateData(new Date());
    obj.setLocalDateTimeData(LocalDateTime.now());
    obj.setBigDecimalData(new BigDecimal("10.23"));
    obj.setBooleanVal(true);
    obj.setImage(new byte[] {10, 20, 30});

    obj.setJavaUtilDateTsData(new Date());

    jtm.insert(obj);

    SelectMapper<TypeCheckPostgres> typeCheckMapper =
        jtm.getSelectMapper(TypeCheckPostgres.class, "tc");

    String sql =
        "select" + typeCheckMapper.getColumnsSql() + " from type_check tc" + " where tc.id = ?";

    ResultSetExtractor<List<TypeCheckPostgres>> rsExtractor =
        new ResultSetExtractor<List<TypeCheckPostgres>>() {
          @Override
          public List<TypeCheckPostgres> extractData(ResultSet rs)
              throws SQLException, DataAccessException {
            List<TypeCheckPostgres> list = new ArrayList<>();
            while (rs.next()) {
              list.add(typeCheckMapper.buildModel(rs));
            }
            return list;
          }
        };

    List<TypeCheckPostgres> list = jtm.getJdbcTemplate().query(sql, rsExtractor, obj.getId());

    assertTrue(list.size() == 1);

    TypeCheckPostgres tc = list.get(0);

    assertNotNull(tc.getLocalDateData());
    assertNotNull(tc.getJavaUtilDateData());
    assertNotNull(tc.getLocalDateTimeData());

    assertTrue(tc.getBigDecimalData().compareTo(obj.getBigDecimalData()) == 0);

    assertArrayEquals(obj.getImage(), tc.getImage());

    assertTrue(tc.getBooleanVal());


    assertNotNull(tc.getJavaUtilDateTsData());
  }
}

