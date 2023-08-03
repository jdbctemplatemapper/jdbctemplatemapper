package io.github.jdbctemplatemapper.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.Assert;

/**
 * <pre>
 * Spring's JdbcTemplate provides data access using JDBC/SQL. It is a better option for complex enterprise applications 
 * than an ORM (ORM magic/nuances get in the way for large/complex applications). Even though JdbcTemplate abstracts 
 * away a lot of the JDBC boiler plate code, it still is verbose.
 * 
 * JdbcTemplateMapper makes CRUD with JdbcTemplate simpler. Use it for one line CRUD operations and for other database 
 * access operations use JdbcTemplate as you normally would.
 *
 * 
 * <strong>Features</strong>
 * 1. One liners for CRUD. To keep the library as simple possible it only has 2 annotations.
 * 2. Can be configured for the following (optional):
 *      auto assign created on, updated on.
 *      auto assign created by, updated by using an implementation of IRecordOperatorResolver.
 *     optimistic locking functionality for updates by configuring a version property.
 * 3. Thread safe so just needs a single instance (similar to JdbcTemplate)
 * 4. To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section.
 * 5. Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with 
 *    other relational databases.
 *
 * <Strong>JdbcTemplateMapper is opinionated</strong> 
 * Projects have to meet the following 2 criteria to use it:
 * 
 * 1. Camel case object property names are mapped to snake case table column names. Properties of a model like 'firstName',
 *    'lastName' will be mapped to corresponding columns 'first\_name' and 'last\_name' in the database table
 *    If for a model property a column match is not found, those properties will be ignored during CRUD operations.  
 * 2. The model properties map to table columns and have no concept of relationships. So foreign keys in tables will need a corresponding **extra** property in the model. For example if an 'Order' is tied to a 'Customer', to match the 'customer\_id' column in the 'order' table you will need to have the 'customerId' property in the 'Order' model. 
 *
 * <strong>Examples code</strong>
 * // Product class below maps to 'product' table by default.
 * // Use annotation {@literal @}Table(name="some_tablename") to override the default
 * public class Product {
 *    //{@literal @}Id annotation is required.
 *    // For a auto increment database id use @Id(type=IdType.AUTO_INCREMENT)
 *    // For a non auto increment id use @Id. In this case you will have to manually set id value before insert.
 *
 *    {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *     private Integer id;
 *     private String productName;
 *     private Double price;
 *     private LocalDateTime availableDate;
 *
 *     // insert/update/find.. methods will ignore properties which do not
 *     // have a corresponding snake case columns in database table
 *     private String someNonDatabaseProperty;
 *
 *     // getters and setters ...
 * }
 *
 * Product product = new Product();
 * product.setProductName("some product name");
 * product.setPrice(10.25);
 * product.setAvailableDate(LocalDateTime.now());
 * jdbcTemplateMapper.insert(product);
 *
 * product = jdbcTemplateMapper.findById(1, Product.class);
 * product.setPrice(11.50);
 * jdbcTemplateMapper.update(product);
 *
 * List{@literal <Product>} products = jdbcTemplateMapper.findAll(Product.class);
 *
 * jdbcTemplateMapper.delete(product);
 *
 * <strong>Maven coordinates</strong> 
 *{@code
 *  <dependency>
 *   <groupId>io.github.jdbctemplatemapper</groupId>
 *   <artifactId>jdbctemplatemapper</artifactId>
 *   <version>0.5.1-SNAPSHOT</version>
 * </dependency>
 * }
 * Make sure the Spring dependency for JdbcTempate is in your pom.xml. It will look something like below:
 * {@code
 *  <dependency>
 *    <groupId>org.springframework.boot</groupId>
 *    <artifactId>spring-boot-starter-jdbc</artifactId>
 * </dependency>
 * }
 *
 * <strong>Spring bean configuration for JdbcTemplateMapper</strong>
 * 1) Configure JdbcTemplate bean as per Spring documentation
 * 2) Configure the JdbcTemplateMapper bean:
 * {@literal @}Bean
 *  public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
 *
 *    return new JdbcTemplateMapper(jdbcTemplate);
 *
 *    // JdbcTemplateMapper needs to get database metadata to generate the SQL statements.
 *    // Databases may differ on what criteria is needed to retrieve this information. JdbcTemplateMapper
 *    // has multiple constructors so use the appropriate one. For example if you are using oracle and tables
 *    // are not aliased the SQL will need schemaName.tableName to access the table. In this case 
 *    // use the constructor new JdbcTemplateMapper(jdbcTemplate, schemaName);
 * }
 *
 * <strong>Annotations:</strong>
 * {@literal @}Table - This is a class level annotation. Use it when when the camel case class name does not have a corresponding 
 *  snake case table name in the database
 *  For example if you want to map 'Product' to the 'products' table (note plural) use
 * 
 * {@literal @}Table(name="products")
 *  class Product {
 *   ...
 *  }
 *
 * {@literal @}Id - This is a required annotation. There are 2 forms of usage for this.
 * 
 * auto incremented id usage:
 *  class Product {
 * {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *    private Integer productId;
 *    ...
 *  }
 *
 * After a successful insert() operation the productId property will be populated with the new id.
 * 
 * NON auto incremented id usage:
 *  class Customer {
 * {@literal @}Id
 *    private Integer id;
 *    ...
 *  }
 *
 * In this case you will have to manually set the id value before calling insert()
 * 
 * <strong>Configuration to auto assign created on, created by, updated on, updated by, version (optimistic locking)</strong>
 *
 * All these auto assign configurations are optional.
 *
 *  JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate);
 *   jdbcTemplateMapper
 *       .withRecordOperatorResolver(new ConcreteImplementationOfIRecordOperatorResolver())
 *       .withCreatedOnPropertyName("createdOn")
 *       .withCreatedByPropertyName("createdBy")
 *       .withUpdatedOnPropertyName("updatedOn")
 *       .withUpdatedByPropertyName("updatedBy")
 *       .withVersionPropertyName("version");
 *       
 * Example model:
 *
 * class Product {
 *  {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *   private Integer productId;
 *   ...
 *   private LocalDateTime createdOn;
 *   private String createdBy;
 * 
 *   private LocalDateTime updatedOn;
 *   private String updatedBy;
 *
 *   private Integer version;
 * }
 *
 * The following will be the effect of the configuration:
 *
 * created on:
 * For insert the matching property value on the model will be set to the current datetime. Property should be of type LocalDateTime
 * update on:
 * For update the matching property value on the model will be set to the current datetime. Property should be of type LocalDateTime
 * created by:
 * For insert the matching property value on the model will be set to value returned by implementation of IRecordOperatorResolver
 * updated by:
 * For update the matching property value on the model will be set to value returned by implementation of IRecordOperatorResolver
 * version:
 * For update the matching property value on the model will be incremented if successful. If version is stale, an 
 * OptimisticLockingException will be thrown. For an insert this value will be set to 1. The version property should be of type Integer.
 * 
 * <strong>Logging</strong>
 * # log the sql
 * logging.level.org.springframework.jdbc.core.JdbcTemplate=TRACE
 *
 * # log the parameters of sql statement
 * logging.level.org.springframework.jdbc.core.StatementCreatorUtils=TRACE
 * 
 * <strong>Notes</strong>
 1. If insert/update fails do not reuse the object since it could be in an inconsistent state.
 2. Database changes will require a restart of the application since JdbcTemplateMapper caches table metadata.
 *
 * </pre>
 *
 * @author ajoseph
 */
public class JdbcTemplateMapper {
	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate npJdbcTemplate;

	private final MappingHelper mappingHelper;

	private IRecordOperatorResolver recordOperatorResolver;

	private String createdByPropertyName;
	private String createdOnPropertyName;
	private String updatedByPropertyName;
	private String updatedOnPropertyName;
	private String versionPropertyName;

	// update sql cache
	// Map key - object class
	// value - the update sql details
	private Map<Class<?>, SqlAndParams> updateSqlAndParamsCache = new ConcurrentHashMap<>();

	// insert sql cache
	// Map key - object class name
	// value - insert sql details
	private Map<Class<?>, SqlAndParams> insertSqlAndParamsCache = new ConcurrentHashMap<>();

	/**
	 * @param jdbcTemplate - The jdbcTemplate
	 */
	public JdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
		this(jdbcTemplate, null, null, null);
	}

	/**
	 * @param jdbcTemplate - The jdbcTemplate
	 * @param schemaName   database schema name.
	 */
	public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName) {
		this(jdbcTemplate, schemaName, null, null);
	}

	/**
	 * @param jdbcTemplate - The jdbcTemplate
	 * @param schemaName   database schema name.
	 * @param catalogName  database catalog name.
	 */
	public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName) {
		this(jdbcTemplate, schemaName, catalogName, null);
	}

	/**
	 * @param jdbcTemplate              - The jdbcTemplate
	 * @param schemaName                - database schema name.
	 * @param catalogName               - database catalog name.
	 * @param metaDataColumnNamePattern - For most jdbc drivers getting column
	 *                                  metadata from database the
	 *                                  metaDataColumnNamePattern argument of null
	 *                                  will return all the columns (which is the
	 *                                  default for JdbcTemplateMapper). Some jdbc
	 *                                  drivers may require to pass something like
	 *                                  '%'.
	 */
	public JdbcTemplateMapper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName,
			String metaDataColumnNamePattern) {

		Assert.notNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.jdbcTemplate = jdbcTemplate;

		npJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

		mappingHelper = new MappingHelper(jdbcTemplate, schemaName, catalogName, metaDataColumnNamePattern);
	}

	/**
	 * Gets the JdbcTemplate of the jdbcTemplateMapper
	 *
	 * @return the JdbcTemplate
	 */
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	/**
	 * Gets the NamedParameterJdbcTemplate of the jdbcTemplateMapper
	 *
	 * @return the NamedParameterJdbcTemplate
	 */
	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return npJdbcTemplate;
	}

	/**
	 * Assign this to identify the property name of created on field. This property
	 * has to be of type LocalDateTime. When an object is inserted into the database
	 * the value of this field will be set to current. Assign this while
	 * initializing jdbcTemplateMapper.
	 *
	 * @param propName : the created on property name.
	 * @return The jdbcTemplateMapper
	 */
	public JdbcTemplateMapper withCreatedOnPropertyName(String propName) {
		this.createdOnPropertyName = propName;
		return this;
	}

	/**
	 * An implementation of IRecordOperatorResolver is used to populate the created
	 * by and updated by fields. Assign this while initializing the
	 * jdbcTemplateMapper
	 *
	 * @param recordOperatorResolver The implement for interface
	 *                               IRecordOperatorResolver
	 * @return The jdbcTemplateMapper The jdbcTemplateMapper
	 */
	public JdbcTemplateMapper withRecordOperatorResolver(IRecordOperatorResolver recordOperatorResolver) {
		this.recordOperatorResolver = recordOperatorResolver;
		return this;
	}

	/**
	 * Assign this to identify the property name of the created by field. The
	 * created by property will be assigned the value from
	 * IRecordOperatorResolver.getRecordOperator() when the object is inserted into
	 * the database. Assign this while initializing the jdbcTemplateMapper
	 *
	 * @param propName : the created by property name.
	 * @return The jdbcTemplateMapper
	 */
	public JdbcTemplateMapper withCreatedByPropertyName(String propName) {
		this.createdByPropertyName = propName;
		return this;
	}

	/**
	 * Assign this to identify the property name of updated on field. This property
	 * has to be of type LocalDateTime. When an object is updated in the database
	 * the value of this field will be set to current. Assign this while
	 * initializing jdbcTemplateMapper.
	 *
	 * @param propName : the updated on property name.
	 * @return The jdbcTemplateMapper
	 */
	public JdbcTemplateMapper withUpdatedOnPropertyName(String propName) {
		this.updatedOnPropertyName = propName;
		return this;
	}

	/**
	 * Assign this to identify the property name of updated by field. The updated by
	 * property will be assigned the value from
	 * IRecordOperatorResolver.getRecordOperator when the object is updated in the
	 * database. Assign this while initializing the jdbcTemplateMapper
	 *
	 * @param propName : the update by property name.
	 * @return The jdbcTemplateMapper
	 */
	public JdbcTemplateMapper withUpdatedByPropertyName(String propName) {
		this.updatedByPropertyName = propName;
		return this;
	}

	/**
	 * The property used for optimistic locking. The property has to be of type
	 * Integer. If the object has the version property name, on inserts it will be
	 * set to 1 and on updates it will incremented by 1. Assign this while
	 * initializing jdbcTemplateMapper.
	 *
	 * @param propName The version propertyName
	 * @return The jdbcTemplateMapper
	 */
	public JdbcTemplateMapper withVersionPropertyName(String propName) {
		this.versionPropertyName = propName;
		return this;
	}

	/**
	 * Returns the object by Id. Return null if not found
	 *
	 * @param id    Id of object
	 * @param clazz Class of object
	 * @param <T>   the type of the object
	 * @return the object of the specific type
	 */
	public <T> T findById(Object id, Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		String sql = "SELECT * FROM " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " WHERE "
				+ tableMapping.getIdColumnName() + " = ?";
		RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
		try {
			Object obj = jdbcTemplate.queryForObject(sql, mapper, id);
			return clazz.cast(obj);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/**
	 * Find all objects
	 *
	 * @param clazz Type of object
	 * @param <T>   the type of the objects
	 * @return List of objects
	 */
	public <T> List<T> findAll(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		String tableName = mappingHelper.getTableMapping(clazz).getTableName();
		String sql = "SELECT * FROM " + mappingHelper.fullyQualifiedTableName(tableName);
		RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);
		return jdbcTemplate.query(sql, mapper);
	}

	/**
	 * Inserts an object into the database. If id is of the auto incremented after
	 * the insert the object id will be assigned.
	 *
	 * <p>
	 * Also assigns created by, created on, updated by, updated on, version if these
	 * properties exist for the object and the JdbcTemplateMapper is configured for
	 * them.
	 *
	 * @param obj The object to be saved
	 */
	public void insert(Object obj) {
		Assert.notNull(obj, "Object must not be null");

		TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
		Object idValue = bw.getPropertyValue(tableMapping.getIdPropertyName());

		if (tableMapping.isIdAutoIncrement()) {
			if (idValue != null) {
				throw new RuntimeException("For insert() the identifier property " + obj.getClass().getSimpleName()
						+ "." + tableMapping.getIdPropertyName()
						+ " has to be null since this insert is for an object whose id is auto increment.");
			}
		} else {
			if (idValue == null) {
				throw new RuntimeException("For insert() identifier property " + obj.getClass().getSimpleName() + "."
						+ tableMapping.getIdPropertyName() + " cannot be null since it is not an auto increment id.");
			}
		}

		SqlAndParams sqlAndParams = insertSqlAndParamsCache.get(obj.getClass());
		if (sqlAndParams == null) {
			sqlAndParams = buildSqlAndParamsForInsert(tableMapping);
			insertSqlAndParamsCache.put(obj.getClass(), sqlAndParams);
		}

		LocalDateTime now = LocalDateTime.now();

		if (createdOnPropertyName != null && tableMapping.getColumnName(createdOnPropertyName) != null) {
			bw.setPropertyValue(createdOnPropertyName, now);
		}

		if (createdByPropertyName != null && recordOperatorResolver != null
				&& tableMapping.getColumnName(createdByPropertyName) != null) {
			bw.setPropertyValue(createdByPropertyName, recordOperatorResolver.getRecordOperator());
		}
		if (updatedOnPropertyName != null && tableMapping.getColumnName(updatedOnPropertyName) != null) {
			bw.setPropertyValue(updatedOnPropertyName, now);
		}
		if (updatedByPropertyName != null && recordOperatorResolver != null
				&& tableMapping.getColumnName(updatedByPropertyName) != null) {
			bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
		}
		if (versionPropertyName != null && tableMapping.getColumnName(versionPropertyName) != null) {
			bw.setPropertyValue(versionPropertyName, 1);
		}

		KeyHolder holder = new GeneratedKeyHolder();

		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		for (String paramName : sqlAndParams.getParams()) {
			mapSqlParameterSource.addValue(paramName, bw.getPropertyValue(paramName),
					tableMapping.getPropertySqlType(paramName));
		}

		if (tableMapping.isIdAutoIncrement()) {
			npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource, holder);
			bw.setPropertyValue(tableMapping.getIdPropertyName(), holder.getKey());
		} else {
			npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
		}

	}



	/**
	 * Updates object. Assigns updated by, updated on if these properties exist for
	 * the object and the jdbcTemplateMapper is configured for these fields. if
	 * optimistic locking 'version' property exists for object throws an
	 * OptimisticLockingException if object is stale
	 *
	 * @param obj object to be updated
	 * @return number of records updated
	 */
	public Integer update(Object obj) {
		Assert.notNull(obj, "Object must not be null");

		TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

		SqlAndParams sqlAndParams = updateSqlAndParamsCache.get(obj.getClass());

		if (sqlAndParams == null) {
			sqlAndParams = buildSqlAndParamsForUpdate(tableMapping);
			updateSqlAndParamsCache.put(obj.getClass(), sqlAndParams);
		}

		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
		Set<String> parameters = sqlAndParams.getParams();
		if (updatedOnPropertyName != null && parameters.contains(updatedOnPropertyName)) {
			bw.setPropertyValue(updatedOnPropertyName, LocalDateTime.now());
		}
		if (updatedByPropertyName != null && recordOperatorResolver != null
				&& parameters.contains(updatedByPropertyName)) {
			bw.setPropertyValue(updatedByPropertyName, recordOperatorResolver.getRecordOperator());
		}

		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		for (String paramName : parameters) {
			if (paramName.equals("incrementedVersion")) {
				Integer versionVal = (Integer) bw.getPropertyValue(versionPropertyName);
				if (versionVal == null) {
					throw new RuntimeException(
							versionPropertyName + " cannot be null when updating " + obj.getClass().getSimpleName());
				} else {
					mapSqlParameterSource.addValue("incrementedVersion", versionVal + 1, java.sql.Types.INTEGER);
				}
			} else {
				mapSqlParameterSource.addValue(paramName, bw.getPropertyValue(paramName),
						tableMapping.getPropertySqlType(paramName));
			}
		}

		// if object has property version the version gets incremented on update.
		// throws OptimisticLockingException when update fails.
		if (sqlAndParams.getParams().contains("incrementedVersion")) {
			int cnt = npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
			if (cnt == 0) {
				throw new OptimisticLockingException("Update failed for " + obj.getClass().getSimpleName() + " for "
						+ tableMapping.getIdPropertyName() + ":" + bw.getPropertyValue(tableMapping.getIdPropertyName())
						+ " and " + versionPropertyName + ":" + bw.getPropertyValue(versionPropertyName));
			}
			// update the version in object with new version
			bw.setPropertyValue(versionPropertyName, mapSqlParameterSource.getValue("incrementedVersion"));
			return cnt;
		} else {
			return npJdbcTemplate.update(sqlAndParams.getSql(), mapSqlParameterSource);
		}

	}

	/**
	 * Physically Deletes the object from the database
	 *
	 * @param obj Object to be deleted
	 * @return number of records were deleted (1 or 0)
	 */
	public Integer delete(Object obj) {
		Assert.notNull(obj, "Object must not be null");

		TableMapping tableMapping = mappingHelper.getTableMapping(obj.getClass());

		String sql = "delete from " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " where "
				+ tableMapping.getIdColumnName() + "= ?";
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
		Object id = bw.getPropertyValue(tableMapping.getIdPropertyName());
		return jdbcTemplate.update(sql, id);
	}

	/**
	 * Physically Deletes the object from the database by id
	 *
	 * @param <T>   This describes class type
	 * @param id    Id of object to be deleted
	 * @param clazz Type of object to be deleted.
	 * @return number records were deleted (1 or 0)
	 */
	public <T> Integer deleteById(Object id, Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(id, "id must not be null");

		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		String sql = "delete from " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " where "
				+ tableMapping.getIdColumnName() + " = ?";
		return jdbcTemplate.update(sql, id);
	}
	
	private SqlAndParams buildSqlAndParamsForInsert(TableMapping tableMapping) {
		Assert.notNull(tableMapping, "tableMapping must not be null");

		Set<String> params = new HashSet<>();
		StringBuilder sqlIntoPart = new StringBuilder("INSERT INTO ");
		sqlIntoPart.append(mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()));
		sqlIntoPart.append(" ( ");

		StringBuilder sqlValuePart = new StringBuilder(" VALUES (");

		boolean first = true;
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			if (tableMapping.isIdAutoIncrement()) {
				if (tableMapping.getIdPropertyName().equals(propMapping.getPropertyName())) {
					continue;
				}
			}
			if (!first) {
				sqlIntoPart.append(", ");
				sqlValuePart.append(", ");
			} else {
				first = false;
			}
			sqlIntoPart.append(propMapping.getColumnName());
			sqlValuePart.append(":" + propMapping.getPropertyName());

			params.add(propMapping.getPropertyName());
		}

		sqlIntoPart.append(") ");
		sqlValuePart.append(")");

		return new SqlAndParams(sqlIntoPart.toString() + sqlValuePart.toString(), params);
	}

	private SqlAndParams buildSqlAndParamsForUpdate(TableMapping tableMapping) {
		Assert.notNull(tableMapping, "tableMapping must not be null");

		// ignore these attributes when generating the sql 'SET' command
		List<String> ignoreAttrs = new ArrayList<>();
		ignoreAttrs.add(tableMapping.getIdPropertyName());
		if (createdByPropertyName != null) {
			ignoreAttrs.add(createdByPropertyName);
		}
		if (createdOnPropertyName != null) {
			ignoreAttrs.add(createdOnPropertyName);
		}

		Set<String> params = new HashSet<>();
		StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
		sqlBuilder.append(mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()));
		sqlBuilder.append(" SET ");

		String versionColumnName = tableMapping.getColumnName(versionPropertyName);
		boolean first = true;
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			if (ignoreAttrs.contains(propMapping.getPropertyName())) {
				continue;
			}
			if (!first) {
				sqlBuilder.append(", ");
			} else {
				first = false;
			}
			sqlBuilder.append(propMapping.getColumnName());
			sqlBuilder.append(" = :");

			if (versionPropertyName != null && propMapping.getColumnName().equals(versionColumnName)) {
				sqlBuilder.append("incrementedVersion");
				params.add("incrementedVersion");
			} else {
				sqlBuilder.append(propMapping.getPropertyName());
				params.add(propMapping.getPropertyName());
			}
		}

		// the where clause
		sqlBuilder.append(" WHERE " + tableMapping.getIdColumnName() + " = :" + tableMapping.getIdPropertyName());
		params.add(tableMapping.getIdPropertyName());

		if (versionPropertyName != null && versionColumnName != null) {
			sqlBuilder.append(" AND ").append(versionColumnName).append(" = :").append(versionPropertyName);
			params.add(versionPropertyName);
		}

		String updateSql = sqlBuilder.toString();
		SqlAndParams updateSqlAndParams = new SqlAndParams(updateSql, params);

		return updateSqlAndParams;
	}
}
