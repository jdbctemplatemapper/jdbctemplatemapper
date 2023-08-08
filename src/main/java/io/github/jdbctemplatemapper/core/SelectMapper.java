package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.exception.MapperException;

/**
 * <pre>
 * Allows generating the select columns string for the model and population of the model from a ResultSet.
 * 
 * selectMapper.getColumnsSql() will provide a string of columns which can be used in the sql Select statement
 * selectMapper.buildModel(resultSet) will return a model populated from the resultSet data.
 * 
 * Makes the code for writing and retrieving relationships less verbose.
 *
 * An example for querying the following relationship: An 'Order' has many 'OrderLine' and each 'OrderLine' has one product
 * using Spring's ResultSetExtractor  
 * </pre>
 * 
 * <pre>
 * {@code
 * // Querying the following relationship: An 'Order' has many 'OrderLine' and each 'OrderLine' has one product.  
 * // The second argument to getSelectMapper() is the table alias in the query.
 * // For the query belwo the 'orders' table alias is 'o', the 'order_line' table alias is 'ol' and the product
 * // table alias is 'p'.
 * SelectMapper<Order> orderSelectMapper = jdbcTemplateMapper.getSelectMapper(Order.class, "o");
 * SelectMapper<OrderLine> orderLineSelectMapper = jdbcTemplateMapper.getSelectMapper(OrderLine.class, "ol");
 * SelectMapper<Product> productSelectMapper = jdbcTemplateMapper.getSelectMapper(Product.class, "p");
 *
 * // no need to type all those column names so we can concentrate on where and join clauses
 * String sql = "select" 
 *              + orderSelectMapper.getColumnsSql() 
 *              + ","
 *              + orderLineSelectMapper.getColumnsSql() 
 *              + "," 
 *              + productSelectMapper.getColumnsSql()
 *              + " from orders o" 
 *              + " left join order_line ol on o.order_id = ol.order_id"
 *              + " join product p on p.product_id = ol.product_id"
 *              + " order by o.order_id, ol.order_line_id";
 *              
 * // Using Spring's ResultSetExtractor		
 * ResultSetExtractor<List<Order>> rsExtractor = new ResultSetExtractor<List<Order>>() {
 *    {@literal @}Override
 *    public List<Order> extractData(ResultSet rs) throws SQLException, DataAccessException {	
 *    
 *      Map<Long, Order> orderByIdMap = new LinkedHashMap<>(); // LinkedHashMap to retain result order	
 *		Map<Integer, Product> productByIdMap = new HashMap<>();
 *		
 *      while (rs.next()) {				
 *        // selectMapper.buildModel(rs) will return the model fully populated from the resultSet
 *        // Everything below is just logic to populate the relationships.
 *        // Doing some checks to make sure unwanted objects are not created.
 *        // In this use case Order has many OrderLine and an OrderLine has one product
 *					
 *        // orderSelectMapper.getResultSetModelIdColumnName() returns the id column alias which is 'o_order_id'
 *        // for the sql above. 
 *        Long orderId = rs.getLong(orderSelectMapper.getResultSetModelIdColumnName());					
 *        Order order = orderByIdMap.get(orderId);
 *        if (order == null) {
 *          order = orderSelectMapper.buildModel(rs);
 *          orderByIdMap.put(order.getOrderId(), order);
 *        }
 *				    
 *        // productSelectMapper.getResultSetModelIdColumnName() returns the id column alias which is 'p_product_id'
 *        // for the sql above.
 *        Integer productId = rs.getInt(productSelectMapper.getResultSetModelIdColumnName());
 *        Product product = productByIdMap.get(productId);
 *        if (product == null) {
 *          product = productSelectMapper.buildModel(rs);
 *          productByIdMap.put(product.getProductId(), product);
 *        }
 *				    
 *        OrderLine orderLine = orderLineSelectMapper.buildModel(rs);	
 *        if(orderLine != null) {
 *          orderLine.setProduct(product);
 *          order.addOrderLine(orderLine);
 *        }			
 *     }
 *     return new ArrayList<Order>(orderByIdMap.values());
 *   }
 * };
 *		
 * List<Order> orders = jdbcTemplateMapper.getJdbcTemplate().query(sql, rsExtractor);
 *
 * 
 *}
 * </pre>
 * 
 * @author ajoseph
 */
public class SelectMapper<T> {
	private final MappingHelper mappingHelper;

	private final Class<T> clazz;

	private final DefaultConversionService defaultConversionService;

	private final String tableAlias;

	SelectMapper(Class<T> clazz, String tableAlias, MappingHelper mappingHelper,
			DefaultConversionService defaultConversionService) {
		Assert.notNull(clazz, " clazz cannot be empty");
		Assert.hasLength(tableAlias, " tableAlias cannot be empty");
		if (tableAlias.trim().length() < 1) {
			throw new MapperException("tableAlias cannot be empty");
		}

		this.clazz = clazz;
		this.tableAlias = tableAlias;
		this.mappingHelper = mappingHelper;
		this.defaultConversionService = defaultConversionService;

	}

	/**
	 * Generates a string which can be used in a sql select statement for all the
	 * properties which have corresponding database columns
	 *
	 * <pre>
	 * SelectMapper selectMapper = jdbcTemplateMapper(Employee, "emp");
	 * selectMapper.getColumnSql() will return something line below:
	 * 
	 * "emp.id emp_id, emp.last_name emp_last_name, emp.first_name emp_first_name"
	 * </pre>
	 *
	 * @return comma separated select column string
	 */
	public String getColumnsSql() {
		String str = "";
		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		StringBuilder sb = new StringBuilder(" ");
		String colPrefix = tableAlias + ".";
		String aliasPrefix = " " + tableAlias + "_";
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			sb.append(colPrefix).append(propMapping.getColumnName()).append(aliasPrefix)
					.append(propMapping.getColumnName()).append(",");
		}
		str = sb.toString();
		// remove the last comma.
		if (mappingHelper.isNotEmpty(str)) {
			str = " " + str.substring(0, str.length() - 1) + " ";
		}
		return str;
	}

	/**
	 * gets column alias name of the models id in sql statement
	 * @return the column alias name of the models id in sql statement
	 */
	public String getResultSetModelIdColumnName() {
		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		return tableAlias + "_" + tableMapping.getIdColumnName();
	}

	/**
	 * Builds the model from the resultSet
	 * 
	 * @param rs The ResultSet from which to build the model.
	 * @return the model. Will return null if the id property is null (even if other
	 *         fields have some values)
	 */
	public T buildModel(ResultSet rs) {
		try {
			Object obj = clazz.newInstance();

			TableMapping tableMapping = mappingHelper.getTableMapping(clazz);

			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
			// need below for java.sql.Timestamp to java.time.LocalDateTime conversion etc
			bw.setConversionService(defaultConversionService);

			String colPrefix = tableAlias + "_";
			ResultSetMetaData rsMetaData = rs.getMetaData();
			int count = rsMetaData.getColumnCount();
			for (int i = 1; i <= count; i++) {
				String columnLabel = rsMetaData.getColumnLabel(i);
				// case insensitive prefix match
				if (colPrefix.regionMatches(true, 0, columnLabel, 0, colPrefix.length())) {
					String propertyName = tableMapping.getProperyName(columnLabel.substring(colPrefix.length()));
					if (propertyName != null) {
						// Using JdbcUtils since it handles different jdbc drivers
						bw.setPropertyValue(propertyName,
								JdbcUtils.getResultSetValue(rs, i, tableMapping.getPropertyType(propertyName)));
					}
				}
			}
			// if id is is null return null. Does not matter if other fields have values.
			if (bw.getPropertyValue(tableMapping.getIdPropertyName()) == null) {
				return null;
			} else {
				return clazz.cast(obj);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
