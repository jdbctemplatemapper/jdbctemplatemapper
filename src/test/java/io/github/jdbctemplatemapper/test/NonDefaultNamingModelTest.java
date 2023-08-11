package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
import io.github.jdbctemplatemapper.model.NonDefaultNamingProduct;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.TypeCheck;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class NonDefaultNamingModelTest {

	@Value("${spring.datasource.driver-class-name}")
	private String jdbcDriver;

	@Autowired
	private JdbcTemplateMapper jtm;
	
	
	@Test
	public void findById_Test() {
		NonDefaultNamingProduct product = jtm.findById(1, NonDefaultNamingProduct.class); 
	}

	@Test
	public void insert_Test() {
		NonDefaultNamingProduct product = new NonDefaultNamingProduct();
		product.setId(1005);
		product.setProductName("hat");
		product.setCost(12.25);

		jtm.insert(product);
	}
	
	@Test
	public void update_Test() throws Exception {
		NonDefaultNamingProduct product = new NonDefaultNamingProduct();
		product.setId(1010);
		product.setProductName("hat");
		product.setCost(12.25);

		jtm.insert(product);

		NonDefaultNamingProduct product1 = jtm.findById(product.getId(), NonDefaultNamingProduct.class); 
		
		product1.setProductName("cap");
		jtm.update(product1);

	}
	
	@Test
	public void selectMapper_test() {

		NonDefaultNamingProduct product = new NonDefaultNamingProduct();
		product.setId(1015);
		product.setProductName("hat");
		product.setCost(50.00);

		jtm.insert(product);

		SelectMapper<NonDefaultNamingProduct> selMapper = jtm.getSelectMapper(NonDefaultNamingProduct.class, "p");

		String sql = "SELECT" + selMapper.getColumnsSql() + " FROM product p " + " WHERE p.product_id = ?";

		ResultSetExtractor<List<NonDefaultNamingProduct>> rsExtractor = new ResultSetExtractor<List<NonDefaultNamingProduct>>() {
			@Override
			public List<NonDefaultNamingProduct> extractData(ResultSet rs) throws SQLException, DataAccessException {
				List<NonDefaultNamingProduct> list = new ArrayList<>();
				while (rs.next()) {
					list.add(selMapper.buildModel(rs));
				}
				return list;
			}
		};

		List<NonDefaultNamingProduct> list = jtm.getJdbcTemplate().query(sql, rsExtractor, product.getId());

        
		assertTrue(list.size() == 1);
		
		NonDefaultNamingProduct prod = list.get(0);
		assertEquals(product.getId(), prod.getId());
        assertEquals(product.getProductName(), prod.getProductName());	
        assertEquals("tester", prod.getWhoCreated());
        assertEquals("tester", prod.getWhoUpdated());
        assertEquals(1, prod.getLock());
        assertNotNull(prod.getCreatedAt());
        assertNotNull(prod.getUpdatedAt());
        
        
	}
	
}
