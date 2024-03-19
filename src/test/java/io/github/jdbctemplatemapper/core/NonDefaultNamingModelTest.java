package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import io.github.jdbctemplatemapper.model.NonDefaultNamingProduct;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class NonDefaultNamingModelTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void findById_Test() {
    NonDefaultNamingProduct prod = jtm.findById(NonDefaultNamingProduct.class, 1);

    assertEquals(1, prod.getId());
    assertEquals("shoes", prod.getProductName());
    assertEquals("system", prod.getWhoCreated());
    assertEquals("system", prod.getWhoUpdated());
    assertEquals(1, prod.getOptiLock());
    assertNotNull(prod.getCreatedAt());
    assertNotNull(prod.getUpdatedAt());
  }

  @Test
  public void findAll_Test() {
    List<NonDefaultNamingProduct> list = jtm.findAll(NonDefaultNamingProduct.class);

    NonDefaultNamingProduct prod = list.get(0);

    assertNotNull(prod.getProductName());
    assertNotNull(prod.getWhoCreated());
    assertNotNull(prod.getCreatedAt());
    assertNotNull(prod.getUpdatedAt());
  }

  @Test
  public void insert_Test() {
    NonDefaultNamingProduct prod = new NonDefaultNamingProduct();
    prod.setId(1005);
    prod.setProductName("hat");
    prod.setCost(12.25);

    jtm.insert(prod);

    NonDefaultNamingProduct prod2 = jtm.findById(NonDefaultNamingProduct.class, 1005);

    assertEquals(1005, prod2.getId());
    assertEquals("hat", prod2.getProductName());
    assertEquals(12.25, prod2.getCost());
    assertEquals(1, prod2.getOptiLock());
  }

  @Test
  public void update_Test() throws Exception {
    NonDefaultNamingProduct product = new NonDefaultNamingProduct();
    product.setId(1010);
    product.setProductName("hat");
    product.setCost(12.25);

    jtm.insert(product);

    NonDefaultNamingProduct prod1 = jtm.findById(NonDefaultNamingProduct.class, product.getId());

    prod1.setProductName("cap");
    jtm.update(prod1);

    NonDefaultNamingProduct prod2 = jtm.findById(NonDefaultNamingProduct.class, prod1.getId());

    assertEquals(1010, prod2.getId());
    assertEquals("cap", prod2.getProductName());
    assertTrue(product.getOptiLock() < prod2.getOptiLock());
  }

  @Test
  public void getColumnName_Test() {
    String columnName = jtm.getColumnName(NonDefaultNamingProduct.class, "id");
    assertEquals("product_id", columnName);
  }

  @Test
  public void selectMapper_test() {

    NonDefaultNamingProduct product = new NonDefaultNamingProduct();
    product.setId(1015);
    product.setProductName("hat");
    product.setCost(50.00);

    jtm.insert(product);

    SelectMapper<NonDefaultNamingProduct> selMapper =
        jtm.getSelectMapper(NonDefaultNamingProduct.class, "p");

    String sql =
        "SELECT" + selMapper.getColumnsSql() + " FROM product p " + " WHERE p.product_id = ?";

    ResultSetExtractor<List<NonDefaultNamingProduct>> rsExtractor =
        new ResultSetExtractor<List<NonDefaultNamingProduct>>() {
          @Override
          public List<NonDefaultNamingProduct> extractData(ResultSet rs)
              throws SQLException, DataAccessException {
            List<NonDefaultNamingProduct> list = new ArrayList<>();
            while (rs.next()) {
              list.add(selMapper.buildModel(rs));
            }
            return list;
          }
        };

    List<NonDefaultNamingProduct> list =
        jtm.getJdbcTemplate().query(sql, rsExtractor, product.getId());

    assertTrue(list.size() == 1);

    NonDefaultNamingProduct prod = list.get(0);
    assertEquals(product.getId(), prod.getId());
    assertEquals(product.getProductName(), prod.getProductName());
    assertEquals("tester", prod.getWhoCreated());
    assertEquals("tester", prod.getWhoUpdated());
    assertEquals(1, prod.getOptiLock());
    assertNotNull(prod.getCreatedAt());
    assertNotNull(prod.getUpdatedAt());
  }
}
