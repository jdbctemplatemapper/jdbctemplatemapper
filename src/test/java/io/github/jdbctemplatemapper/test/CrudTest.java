package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.exception.OptimisticLockingException;
import io.github.jdbctemplatemapper.model.Customer;
import io.github.jdbctemplatemapper.model.NoTableAnnotationModel;
import io.github.jdbctemplatemapper.model.Order;
import io.github.jdbctemplatemapper.model.OrderLine;
import io.github.jdbctemplatemapper.model.Person;
import io.github.jdbctemplatemapper.model.Product;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class CrudTest {

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
		order = jtm.findById(Order.class, order.getOrderId());
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

		Customer customer1 = jtm.findById(Customer.class,customer.getCustomerId());

		assertNotNull(customer1.getCustomerId());
		assertEquals("aaa", customer1.getFirstName());
		assertEquals("bbb", customer1.getLastName());
	}

	@Test
	public void insert_withNonNullIdFailureTest() {
		Order order = new Order();
		order.setOrderId(2002L);
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
		product = jtm.findById(Product.class,1001);
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

		Person person1 = jtm.findById( Person.class,person.getPersonId());

		assertNotNull(person1);
	}

	@Test
	public void insert_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.insert(null);
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
	public void update_Test() throws Exception {
		Order order = jtm.findById(Order.class,1);
		LocalDateTime prevUpdatedOn = order.getUpdatedOn();

		Thread.sleep(1000); // avoid timing issue.

		order.setStatus("COMPLETE");

		jtm.update(order);

		// check if auto assigned properties have changed.
		assertEquals(2, order.getVersion());
		assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
		assertEquals("tester", order.getUpdatedBy());

		// requery and check
		order = jtm.findById(Order.class,1);
		assertEquals("COMPLETE", order.getStatus());
		assertEquals(2, order.getVersion()); // version incremented
		assertTrue(order.getUpdatedOn().isAfter(prevUpdatedOn));
		assertEquals("tester", order.getUpdatedBy());
	}

	@Test
	public void update_withIdOfTypeIntegerTest() {

		Product product = jtm.findById(Product.class,4);

		Product product1 = jtm.findById(Product.class,4);

		product1.setName("xyz");
		jtm.update(product1);

		assertEquals("xyz", product1.getName());
		assertTrue(product1.getVersion() > product.getVersion()); // version incremented
	}

	@Test
	public void update_withIdOfTypeStringTest() {

		Person person = jtm.findById(Person.class,"person101");

		person.setLastName("new name");
		jtm.update(person);

		Person person1 = jtm.findById(Person.class,"person101");

		assertEquals("new name", person1.getLastName());
	}

	@Test
	public void update_withNoVersionAndUpdateInfoTest() {
		Customer customer = jtm.findById(Customer.class,4);
		customer.setFirstName("xyz");
		jtm.update(customer);

		Customer customer1 = jtm.findById(Customer.class,4); // requery
		assertEquals("xyz", customer1.getFirstName());
	}

	@Test
	public void update_throwsOptimisticLockingExceptionTest() {
		Assertions.assertThrows(OptimisticLockingException.class, () -> {
			Order order = jtm.findById(Order.class,2);
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
	public void update_nonDatabasePropertyTest() {
		Person person = jtm.findById(Person.class,"person101");

		person.setSomeNonDatabaseProperty("xyz");
		jtm.update(person);

		// requery
		Person person2 = jtm.findById(Person.class,"person101");

		assertNotNull(person2);
		assertNull(person2.getSomeNonDatabaseProperty());
	}

	@Test
	public void findById_Test() {
		Order order = jtm.findById(Order.class,1);

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
		Product product = jtm.findById(Product.class,4);

		int cnt = jtm.delete(product);

		assertTrue(cnt == 1);

		Product product1 = jtm.findById(Product.class,4);

		assertNull(product1);
	}

	@Test
	public void delete_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.delete(null);
		});
	}

	@Test
	public void deleteById_Test() {
		int cnt = jtm.deleteById(Product.class, 5 );

		assertTrue(cnt == 1);

		Product product1 = jtm.findById(Product.class,5);

		assertNull(product1);
	}

	@Test
	public void deleteById_nullObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			jtm.deleteById(null, Product.class);
		});
	}

	@Test
	public void findByProperty_Test() {
		List<OrderLine> orderLines = jtm.findByProperty(OrderLine.class, "orderId", 1);
		assertTrue(orderLines.size() == 2);
	}

	@Test
	public void findByProperty_InvalidProperty_Test() {
		Assertions.assertThrows(MapperException.class, () -> {
			jtm.findByProperty(OrderLine.class, "x", 1 );
		});
	}

	@Test
	public void findByProperty_OrderBy_Test() {
		List<OrderLine> orderLines = jtm.findByProperty(OrderLine.class, "orderId", 1, "orderLineId");
		assertTrue(orderLines.size() == 2);
		assertTrue(orderLines.get(0).getProductId() == 1);
		assertTrue(orderLines.get(0).getNumOfUnits() == 10);
	}

	@Test
	public void findByProperty_InvalidOrderByProperty_Test() {
		Assertions.assertThrows(MapperException.class, () -> {
			jtm.findByProperty(OrderLine.class, "orderId", 1, "x");
		});
	}

	@Test
	public void loadMapping_uccess_test() {
		Assertions.assertDoesNotThrow(() -> {
			jtm.loadMapping(Order.class);
		});
	}

	@Test
	public void loadMapping_failure_test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.loadMapping(NoTableAnnotationModel.class);
		});
	}
	
	@Test
	public void getColumnName_Test() {
		String columnName = jtm.getColumnName(Order.class, "status");
		assertEquals("status", columnName);
	}
	
	@Test
	public void getColumnName_invalid_Test() {
		String columnName = jtm.getColumnName(Order.class, "x");
		assertNull(columnName);
	}

	@Test
	public void selectMapper_test() {

		// The second argument to getSelectMapper() is the table alias in the query.
		// In this query the 'order' tables alias is 'o' 'order o'. If you make a typo
		// you will get a bad SQL grammar exception. See api documentation for
		// getSelectMapper()
		// for details
		SelectMapper<Order> orderSelectMapper = jtm.getSelectMapper(Order.class, "o");
		SelectMapper<OrderLine> orderLineSelectMapper = jtm.getSelectMapper(OrderLine.class, "ol");
		SelectMapper<Product> productSelectMapper = jtm.getSelectMapper(Product.class, "p");

		// no need to type all those column names so you can concentrate on where and
		// join clauses :)
		String sql = "select" + orderSelectMapper.getColumnsSql() + "," + orderLineSelectMapper.getColumnsSql() + ","
				+ productSelectMapper.getColumnsSql() + " from orders o"
				+ " left join order_line ol on o.order_id = ol.order_id"
				+ " join product p on p.product_id = ol.product_id" + " order by o.order_id, ol.order_line_id";

		ResultSetExtractor<List<Order>> rsExtractor = new ResultSetExtractor<List<Order>>() {
			@Override
			public List<Order> extractData(ResultSet rs) throws SQLException, DataAccessException {

				Map<Long, Order> orderByIdMap = new LinkedHashMap<>(); // LinkedHashMap to retain result order
				Map<Integer, Product> productByIdMap = new HashMap<>();

				while (rs.next()) {

					// IMPORTANT thing to know is selectMapper.buildModel(rs) will return the model
					// fully populated from resultSet

					// the logic here is specific for this use case. Your logic will be different.
					// I am doing some checks to make sure unwanted objects are not created.
					// In this use case Order has many OrderLine and an OrderLine has a product

					// orderSelectMapper.getResultSetModelIdColumnName() returns the column alias
					// which is 'o_order_id'
					// for the sql above.
					Long orderId = rs.getLong(orderSelectMapper.getResultSetModelIdColumnLabel());

					Order order = orderByIdMap.get(orderId);
					if (order == null) {
						order = orderSelectMapper.buildModel(rs);
						orderByIdMap.put(order.getOrderId(), order);
					}

					// productSelectMapper.getResultSetModelIdColumnName() returns the column alias
					// which is 'p_product_id'
					// for the sql above.
					Integer productId = rs.getInt(productSelectMapper.getResultSetModelIdColumnLabel());
					Product product = productByIdMap.get(productId);
					if (product == null) {
						product = productSelectMapper.buildModel(rs);
						productByIdMap.put(product.getProductId(), product);
					}

					OrderLine orderLine = orderLineSelectMapper.buildModel(rs);
					if (orderLine != null) {
						orderLine.setProduct(product);
						order.addOrderLine(orderLine);
					}

				}
				return new ArrayList<Order>(orderByIdMap.values());
			}
		};

		List<Order> orders = jtm.getJdbcTemplate().query(sql, rsExtractor);

		assertTrue(orders.size() == 2);
		assertTrue(orders.get(0).getOrderLines().size() == 2);
		assertEquals("IN PROCESS", orders.get(1).getStatus());
		assertTrue(orders.get(1).getOrderLines().size() == 1);
		assertTrue(orders.get(0).getOrderLines().get(0).getProductId() == 1);
		assertEquals("shoes", orders.get(0).getOrderLines().get(0).getProduct().getName());
		assertEquals("socks", orders.get(0).getOrderLines().get(1).getProduct().getName());
		assertEquals("laces", orders.get(1).getOrderLines().get(0).getProduct().getName());

	}

}
