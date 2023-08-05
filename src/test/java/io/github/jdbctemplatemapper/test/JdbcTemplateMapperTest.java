package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
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
import io.github.jdbctemplatemapper.exception.OptimisticLockingException;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.NoIdObject;
import io.github.jdbctemplatemapper.model.NoTableObject;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Person;
import io.github.jdbctemplatemapper.model.Product;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcTemplateMapperTest {

	@Value("${spring.datasource.driver-class-name}")
	private String jdbcDriver;

	@Autowired
	private JdbcTemplateMapper jtm;

	@Test
	public void insert_longAutoIncrementId_Test() {
		Order order = new Order();
		order.setOrderDate(LocalDateTime.now());
		order.setCustomerId(2);

		jtm.insert(order);

		// check if auto assigned properties have been assigned.
		assertNotNull(order.getCreatedOn());
		assertNotNull(order.getUpdatedOn());
		assertEquals(1, order.getVersion());
		assertEquals("tester", order.getCreatedBy());
		assertEquals("tester", order.getUpdatedBy());

		// requery and test.
		order = jtm.findById(order.getOrderId(), Order.class);
		assertNotNull(order.getOrderId());
		assertNotNull(order.getOrderDate());
		assertNotNull(order.getCreatedOn());
		assertNotNull(order.getUpdatedOn());
		assertEquals(1, order.getVersion());
		assertEquals("tester", order.getCreatedBy());
		assertEquals("tester", order.getUpdatedBy());
	}

	@Test
	public void insert_integerAutoIncrementId_withNoVersionAndCreatedInfoTest() {

		// Customer table does not have version, create_on, created_by etc
		Customer customer = new Customer();
		customer.setFirstName("aaa");
		customer.setLastName("bbb");

		jtm.insert(customer);

		Customer customer1 = jtm.findById(customer.getCustomerId(), Customer.class);

		assertNotNull(customer1.getCustomerId());
		assertEquals("aaa", customer1.getFirstName());
		assertEquals("bbb", customer1.getLastName());
	}

	@Test
	public void insert_withNonNullIdFailureTest() {
		Order order = new Order();
		order.setOrderId(2002);
		order.setOrderDate(LocalDateTime.now());
		order.setCustomerId(2);

		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.insert(order);
		});
	}

	@Test
	public void insertWithManualIntegerId_Test() {
		Product product = new Product();
		product.setProductId(1001);
		product.setName("hat");
		product.setCost(12.25);

		jtm.insert(product);

		// check if auto assigned properties are assigned.
		assertNotNull(product.getCreatedOn());
		assertNotNull(product.getUpdatedOn());
		assertEquals(1, product.getVersion());
		assertEquals("tester", product.getCreatedBy());
		assertEquals("tester", product.getUpdatedBy());

		// requery and check
		product = jtm.findById(1001, Product.class);
		assertNotNull(product.getProductId());
		assertEquals("hat", product.getName());
		assertEquals(12.25, product.getCost());
		assertNotNull(product.getCreatedOn());
		assertNotNull(product.getUpdatedOn());
		assertEquals(1, product.getVersion());
		assertEquals("tester", product.getCreatedBy());
		assertEquals("tester", product.getUpdatedBy());
	}

	@Test
	public void insert_withManualStringId() {

		Person person = new Person();
		person.setPersonId("p1");

		person.setFirstName("xxx");
		person.setLastName("yyy");

		jtm.insert(person);

		Person person1 = jtm.findById(person.getPersonId(), Person.class);

		assertNotNull(person1);
	}

	@Test
	public void insert_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.insert(null);
		});
	}

	@Test
	public void insert_noIdObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			NoIdObject pojo = new NoIdObject();
			pojo.setSomething("abc");
			jtm.insert(pojo);
		});
	}

	@Test
	public void insertWithId_withNullIdFailureTest() {
		Product product = new Product();
		product.setName("hat");
		product.setCost(12.25);
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.insert(product);
		});
	}

	@Test
	public void insertWithId_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.insert(null);
		});
	}

	@Test
	public void insertWithId_noIdObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			NoIdObject pojo = new NoIdObject();
			pojo.setSomething("abc");
			jtm.insert(pojo);
		});
	}

	@Test
	public void update_Test() throws Exception {
		Order order = jtm.findById(1, Order.class);
		LocalDateTime prevUpdatedOn = order.getUpdatedOn();

		Thread.sleep(1000); // avoid timing issue.

		order.setStatus("IN PROCESS");

		jtm.update(order);

		// check if auto assigned properties have changed.
		assertEquals(2, order.getVersion());
		assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
		assertEquals("tester", order.getUpdatedBy());

		// requery and check
		order = jtm.findById(1, Order.class);
		assertEquals("IN PROCESS", order.getStatus());
		assertEquals(2, order.getVersion()); // version incremented
		assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
		assertEquals("tester", order.getUpdatedBy());
	}

	@Test
	public void update_withIdOfTypeIntegerTest() {

		Product product = jtm.findById(4, Product.class);

		Product product1 = jtm.findById(4, Product.class);

		product1.setName("xyz");
		jtm.update(product1);

		assertEquals("xyz", product1.getName());
		assertTrue(product1.getVersion() > product.getVersion()); // version incremented
	}

	@Test
	public void update_withIdOfTypeStringTest() {

		Person person = jtm.findById("person101", Person.class);

		person.setLastName("new name");
		jtm.update(person);

		Person person1 = jtm.findById("person101", Person.class);

		assertEquals("new name", person1.getLastName());
	}

	@Test
	public void update_withNoVersionAndUpdateInfoTest() {
		Customer customer = jtm.findById(4, Customer.class);
		customer.setFirstName("xyz");
		jtm.update(customer);

		Customer customer1 = jtm.findById(4, Customer.class); // requery
		assertEquals("xyz", customer1.getFirstName());
	}

	@Test
	public void update_throwsOptimisticLockingExceptionTest() {
		Assertions.assertThrows(OptimisticLockingException.class, () -> {
			Order order = jtm.findById(2, Order.class);
			order.setVersion(order.getVersion() - 1);
			jtm.update(order);
		});
	}

	@Test
	public void update_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.update(null);
		});
	}

	@Test
	public void update_noTableObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			NoTableObject pojo = new NoTableObject();
			pojo.setSomething("abc");
			jtm.update(pojo);
		});
	}

	@Test
	public void update_nonDatabasePropertyTest() {
		Person person = jtm.findById("person101", Person.class);

		person.setSomeNonDatabaseProperty("xyz");
		jtm.update(person);

		// requery
		Person person2 = jtm.findById("person101", Person.class);

		assertNotNull(person2);
		assertNull(person2.getSomeNonDatabaseProperty());
	}

	@Test
	public void findById_Test() {
		Order order = jtm.findById(1, Order.class);

		assertNotNull(order.getOrderId());
		assertNotNull(order.getOrderDate());
		assertNotNull(order.getCreatedBy());
		assertNotNull(order.getCreatedOn());
		assertNotNull(order.getUpdatedBy());
		assertNotNull(order.getUpdatedOn());
		assertNotNull(order.getVersion());
	}

	@Test
	public void findAll_Test() {
		List<Order> orders = jtm.findAll(Order.class);
		assertTrue(orders.size() >= 2);

		for (int idx = 0; idx < orders.size(); idx++) {
			assertNotNull(orders.get(idx).getOrderId());
			assertNotNull(orders.get(idx).getOrderDate());
			assertNotNull(orders.get(idx).getCreatedBy());
			assertNotNull(orders.get(idx).getCreatedOn());
			assertNotNull(orders.get(idx).getUpdatedBy());
			assertNotNull(orders.get(idx).getUpdatedOn());
			assertNotNull(orders.get(idx).getVersion());
		}
	}

	@Test
	public void deleteByObject_Test() {
		Product product = jtm.findById(4, Product.class);

		int cnt = jtm.delete(product);

		assertTrue(cnt == 1);

		Product product1 = jtm.findById(4, Product.class);

		assertNull(product1);
	}

	@Test
	public void delete_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.delete(null);
		});
	}

	@Test
	public void delete_noIdObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			NoIdObject pojo = new NoIdObject();
			pojo.setSomething("abc");
			jtm.delete(pojo);
		});
	}

	@Test
	public void deleteById_Test() {
		int cnt = jtm.deleteById(5, Product.class);

		assertTrue(cnt == 1);

		Product product1 = jtm.findById(5, Product.class);

		assertNull(product1);
	}

	@Test
	public void deleteById_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.deleteById(null, Product.class);
		});
	}

	@Test
	public void selectMapper_test() {

		SelectMapper<Order> orderSelectHelper = jtm.getSelectMapper(Order.class, "o");
		SelectMapper<OrderLine> orderLineSelectHelper = jtm.getSelectMapper(OrderLine.class, "ol");
		SelectMapper<Product> productSelectHelper = jtm.getSelectMapper(Product.class, "p");

		String sql = "select" 
		              + orderSelectHelper.getColumnsSql() 
		              + ","
				      + orderLineSelectHelper.getColumnsSql() 
				      + "," 
		              + productSelectHelper.getColumnsSql()
				      + " from orders o" 
				      + " left join order_line ol on o.order_id = ol.order_id"
				      + " join product p on p.product_id = ol.product_id";

		ResultSetExtractor<List<Order>> rsExtractor = new ResultSetExtractor<List<Order>>() {
			@Override
			public List<Order> extractData(ResultSet rs) throws SQLException, DataAccessException {

				List<Order> list = new ArrayList<>();
				while (rs.next()) {
					
					Order order = orderSelectHelper.buildModel(rs);
					OrderLine orderLine = orderLineSelectHelper.buildModel(rs);
					Product product = productSelectHelper.buildModel(rs);
					
					list.add(order);
					
					// stitch the objects. In this case  Order has many OrderLine and an OrderLine has a product
					
					
					
					
					
					
				}
				return list;
			}
		};
		
		List<Order> orders = jtm.getJdbcTemplate().query(sql, rsExtractor);
		
		assertTrue(orders.size() > 0);

	}

}
