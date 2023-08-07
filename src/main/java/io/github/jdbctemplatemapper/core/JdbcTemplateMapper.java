package io.github.jdbctemplatemapper.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.exception.OptimisticLockingException;

/**
 * <pre>
 * Spring's JdbcTemplate provides data access using SQL/JDBC for relational databases. 
 * JdbcTemplate is a good option for complex enterprise applications where an ORMs magic/nuances become challenging.
 * JdbcTemplate simplifies the use of JDBC but is verbose.
 *
 * JdbcTemplateMapper makes CRUD with Spring's JdbcTemplate simpler. It provides one liners for CRUD and features that help querying of 
 * relationships less verbose.
 *
 * <strong>Features</strong>
 * 1. One liners for CRUD. To keep the library as simple possible it only has 2 annotations.
 * 2. Features that make querying of relationships less verbose
 * 3. Can be configured for the following (optional):
 *      auto assign created on, updated on.
 *      auto assign created by, updated by using an implementation of IRecordOperatorResolver.
 *     optimistic locking functionality for updates by configuring a version property.
 * 4. Thread safe so just needs a single instance (similar to JdbcTemplate)
 * 5. To log the SQL statements it uses the same logging configurations as JdbcTemplate. See the logging section.
 * 6. Tested against PostgreSQL, MySQL, Oracle, SQLServer (Unit tests are run against these databases). Should work with 
 *    other relational databases.  
 *
 * <Strong>JdbcTemplateMapper is opinionated</strong> 
 * Projects have to meet the following criteria for use:
 * 
 * 1. Camel case object property names are mapped to underscore case table column names. Properties of a model like 'firstName', 
 * 'lastName' will be mapped to corresponding columns 'first_name' and 'last_name' in the database table. Properties which 
 * don't have a column match will be ignored during CRUD operations
 * 2. The model properties map to table columns and have no concept of relationships. Foreign keys in tables will need a corresponding 
 * property in the model. For example if an 'Employee' belongs to a 'Department', to match the 'department_id' column in the 'employee' 
 * table there should be a 'departmentId' property in the 'Employee' model. 
 * 
 * {@literal @}Table(name="employee")
 *  public class Employee {
 *  {@literal @}Id(type
 *   private Integer id;
 *   private String name;
 *
 *   ...
 *   private Integer departmentId; // this property is needed for CRUD because the mapper has no concept of relationships.
 *   private Department department;
 * }
 *
 * <strong>Example code</strong>
 * //{@literal @}Table annotation is required and should match a table name in database
 * 
 * {@literal @}Table(name="product")
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
 *     // have a corresponding underscore case columns in database table
 *     private String someNonDatabaseProperty;
 *
 *     // getters and setters ...
 * }
 *
 * Product product = new Product();
 * product.setProductName("some product name");
 * product.setPrice(10.25);
 * product.setAvailableDate(LocalDateTime.now());
 * jdbcTemplateMapper.insert(product); // because id type is auto increment id value will be set after insert.
 *
 * product = jdbcTemplateMapper.findById(1, Product.class);
 * product.setPrice(11.50);
 * jdbcTemplateMapper.update(product);
 *
 * List{@literal <Product>} products = jdbcTemplateMapper.findAll(Product.class);
 *
 * jdbcTemplateMapper.delete(product);
 * 
 * jdbcTemplateMapper.delete(5, Product.class); // deleting just using id
 * 
 * // for methods which help make querying relationships less verbose @see <a href="SelectMapper.html">SelectMapper</a>
 *
 * <strong>Maven coordinates</strong> 
 *{@code
 *  <dependency>
 *   <groupId>io.github.jdbctemplatemapper</groupId>
 *   <artifactId>jdbctemplatemapper</artifactId>
 *   <version>1.1.2</version>
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
 *    // new JdbcTemplateMapper(jdbcTemplate, schemaName);
 *    // new JdbcTemplateMapper(jdbcTemplate, schemaName, catalogName);
 *    // see javadocs for all constructors
 * }
 *
 * <strong>Annotations:</strong>
 * {@literal @}Table - This is a class level annotation and is required. It can be any name and should match a table in the database
 * 
 * {@literal @}Table(name="product")
 *  class Product {
 *   ...
 *  }
 *
 * {@literal @}Id - This is a required annotation. There are 2 forms of usage for this.
 * 
 * auto incremented id usage:
 * {@literal @}Table(name="product")
 *  class Product {
 * {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *    private Integer productId;
 *    ...
 *  }
 *
 * After a successful insert() operation the productId property will be populated with the new id.
 * 
 * NON auto incremented id usage:
 * {@literal @}Table(name="customer")
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
 * Auto configuration is optional and each property configuration is optional.
 * Once configured matching properties of models will get auto assigned (Models don't need to have these properties but if they do they will get auto assigned).
 *
 *{@literal @}Bean
 *public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
 *  JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate);
 *   jdbcTemplateMapper
 *       .withRecordOperatorResolver(new ConcreteImplementationOfIRecordOperatorResolver())
 *       .withCreatedOnPropertyName("createdOn")
 *       .withCreatedByPropertyName("createdBy")
 *       .withUpdatedOnPropertyName("updatedOn")
 *       .withUpdatedByPropertyName("updatedBy")
 *       .withVersionPropertyName("version");
 * 
 * }
 * 
 * Example model:
 *
 *{@literal @}Table(name="product")
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
 * Uses the same logging configurations as JdbcTemplate to log the SQL. In applications.properties:
 * 
 * # log the SQL
 * logging.level.org.springframework.jdbc.core.JdbcTemplate=TRACE
 *
 * # need this to log the INSERT statements
 * logging.level.org.springframework.jdbc.core.simple.SimpleJdbcInsert=TRACE
 * 
 * # log the parameters of sql statement
 * logging.level.org.springframework.jdbc.core.StatementCreatorUtils=TRACE
 * 
 * <strong>Notes</strong>
 * 1. If insert/update fails do not reuse the object since it could be in an inconsistent state.
 * 2. Database changes will require a restart of the application since JdbcTemplateMapper caches table metadata.
 * 
 * <strong>TroubleShooting</strong>
 * Make sure you can connect to your database and issue a simple query using Spring's JdbcTemplate without the JdbcTemplateMapper.
 * 
 * <strong>Known issues</strong>
 * 1. For Oracle/SqlServer no support for blobs.
 * 2. Could have issues with old/non standard jdbc drivers.
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
	// Map key - object class
	// value - insert sql details
	private Map<Class<?>, SimpleJdbcInsert> simpleJdbcInsertCache = new ConcurrentHashMap<>();

	// Need this for type conversions like java.sql.Timestamp to
	// java.time.LocalDateTime etc
	private DefaultConversionService defaultConversionService = new DefaultConversionService();

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
	 * Inserts an object. Objects with auto increment id will have the id set to the
	 * new from database. For non auto increment ids the id has to be manually set
	 * before call insert.
	 *
	 * <p>
	 * Will assign created by, created on, updated by, updated on, version if the
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
				throw new MapperException("For insert() the property " + obj.getClass().getSimpleName() + "."
						+ tableMapping.getIdPropertyName()
						+ " has to be null since this insert is for an object whose id is auto increment.");
			}
		} else {
			if (idValue == null) {
				throw new MapperException("For insert() the property " + obj.getClass().getSimpleName() + "."
						+ tableMapping.getIdPropertyName() + " cannot be null since it is not an auto increment id");
			}
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

		Map<String, Object> attributes = new HashMap<>();
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			attributes.put(propMapping.getColumnName(), bw.getPropertyValue(propMapping.getPropertyName()));
		}

		SimpleJdbcInsert jdbcInsert = simpleJdbcInsertCache.get(obj.getClass());
		if (jdbcInsert == null) {
			if (tableMapping.isIdAutoIncrement()) {
				jdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withCatalogName(mappingHelper.getCatalogName())
						.withSchemaName(mappingHelper.getSchemaName()).withTableName(tableMapping.getTableName())
						.usingGeneratedKeyColumns(tableMapping.getIdColumnName());
			} else {
				jdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withCatalogName(mappingHelper.getCatalogName())
						.withSchemaName(mappingHelper.getSchemaName()).withTableName(tableMapping.getTableName());
			}
			simpleJdbcInsertCache.put(obj.getClass(), jdbcInsert);
		}

		if (tableMapping.isIdAutoIncrement()) {
			Number idNumber = jdbcInsert.executeAndReturnKey(attributes);
			bw.setPropertyValue(tableMapping.getIdPropertyName(), idNumber); // set object id value
		} else {
			jdbcInsert.execute(attributes);
		}
	}

	/**
	 * Update the object.
	 * 
	 * Assigns updated by, updated on if the properties exist for the object and the
	 * jdbcTemplateMapper is configured for these fields. if optimistic locking
	 * 'version' property exists for the object an OptimisticLockingException will
	 * be thrown if object is stale.
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
					throw new MapperException("JdbcTemplateMapper is configured for versioning so "
							+ versionPropertyName + " cannot be null when updating " + obj.getClass().getSimpleName());
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
				throw new OptimisticLockingException(
						"Update failed for " + obj.getClass().getSimpleName() + " . " + tableMapping.getIdPropertyName()
								+ ": " + bw.getPropertyValue(tableMapping.getIdPropertyName()) + " and "
								+ versionPropertyName + ": " + bw.getPropertyValue(versionPropertyName));
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
    
	/**
	 * Returns the SelectMapper 
	 * @param <T>  the class for the SelectMapper
	 * @param clazz the class 
	 * @param tableAlias the table alias used in the query.
	 * @return the SelectMapper
	 */
	public <T> SelectMapper<T> getSelectMapper(Class<T> clazz, String tableAlias) {
		return new SelectMapper<T>(clazz, tableAlias, mappingHelper, defaultConversionService);
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
