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
 * CRUD methods and configuration for JdbcTemplateMapper
 * 
 * See <a href=
"https://github.com/jdbctemplatemapper/jdbctemplatemapper#jdbctemplatemapper">JdbcTemplateMapper documentation</a> for more info
 * </pre>
 * 
 * @author ajoseph
 * 
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

	private boolean useColumnLabelForResultSetMetaData = true;

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
	 * Support for old/non standard jdbc drivers. For these drivers
	 * resultSetMetaData,getcolumnLabel(int) info is in
	 * resultSetMetaData.getColumnName(int). When this is the case set this value to
	 * false. default is true
	 * 
	 * @param val boolean value
	 */
	public void useColumnLabelForResultSetMetaData(boolean val) {
		this.useColumnLabelForResultSetMetaData = val;
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
	 * 
	 * @param <T>        the class for the SelectMapper
	 * @param clazz      the class
	 * @param tableAlias the table alias used in the query.
	 * @return the SelectMapper
	 */
	public <T> SelectMapper<T> getSelectMapper(Class<T> clazz, String tableAlias) {
		return new SelectMapper<T>(clazz, tableAlias, mappingHelper, defaultConversionService,
				useColumnLabelForResultSetMetaData);
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
