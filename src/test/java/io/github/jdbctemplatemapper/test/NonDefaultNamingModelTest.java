package io.github.jdbctemplatemapper.test;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.model.NonDefaultNamingProduct;
import io.github.jdbctemplatemapper.model.Order;

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
}
