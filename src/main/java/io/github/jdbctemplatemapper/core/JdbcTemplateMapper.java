package io.github.jdbctemplatemapper.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
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

	// used for an attempt to support for old/non standard jdbc drivers.
	private boolean useColumnLabelForResultSetMetaData = true;

	// update sql cache
	// Map key - object class
	// value - the update sql details
	private Map<Class<?>, SqlAndParams> updateSqlAndParamsCache = new ConcurrentHashMap<>();

	// insert sql cache
	// Map key - object class
	// value - insert sql details
	private Map<Class<?>, SimpleJdbcInsert> simpleJdbcInsertCache = new ConcurrentHashMap<>();

	// Map key - object class
	// value - the column string for all properties which can be be used in a select
	// statement
	private Map<Class<?>, String> findColumnsSqlCache = new ConcurrentHashMap<>();

	// Need this for type conversions like java.sql.Timestamp to
	// java.time.LocalDateTime etc
	// JdbcTemplate uses this converter for BeanPropertyRowMapper.
	private DefaultConversionService conversionService = (DefaultConversionService) DefaultConversionService
			.getSharedInstance();

	final static String DEFAULT_TABLE_ALIAS = "t";

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
	 * Exposing the conversion service used so if necessary new converters can be
	 * added. DefaultConversionService conversionService = getConversionService()
	 * conversionService.addConverterFactory(SomeImplementation of Springs
	 * ConverterFactoryj);
	 * 
	 * @return the default conversion service.
	 */
	public DefaultConversionService getConversionService() {
		return (DefaultConversionService) conversionService;
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

	public void forcePostgresTimestampWithTimezone(boolean val) {
		mappingHelper.forcePostgresTimestampWithTimezone(val);
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
		String columnsSql = getFindColumnsSql(tableMapping, clazz);

		String sql = "SELECT " + columnsSql + " FROM "
				+ mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " " + DEFAULT_TABLE_ALIAS
				+ " WHERE " + DEFAULT_TABLE_ALIAS + "." + tableMapping.getIdColumnName() + " = ?";

		RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);

		try {
			Object obj = jdbcTemplate.queryForObject(sql, mapper, id);
			return clazz.cast(obj);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/**
	 * Returns list of object by the property
	 *
	 * @param propertyName  the property name
	 * @param propertyValue the value of property to query by
	 * @param clazz         Class of List of objects returned
	 * @param <T>           the type of the object
	 * @return the object of the specific type
	 */
	public <T> List<T> findByProperty(String propertyName, Object propertyValue, Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(clazz, "propertyName must not be null");

		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		String columnName = tableMapping.getColumnName(propertyName);
		if (columnName == null) {
			throw new MapperException("property " + clazz.getSimpleName() + "." + propertyName
					+ " is either invalid or invalid does not have a corresponding column in database.");
		}
		
		String columnsSql = getFindColumnsSql(tableMapping, clazz);
		String sql = "SELECT " + columnsSql + " FROM "
				+ mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " " + DEFAULT_TABLE_ALIAS
				+ " WHERE " + DEFAULT_TABLE_ALIAS + "." + columnName + " = ?";

		RowMapper<T> mapper = BeanPropertyRowMapper.newInstance(clazz);

		return jdbcTemplate.query(sql, mapper, propertyValue);
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

		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		String columnsSql = getFindColumnsSql(tableMapping, clazz);

		String sql = "SELECT " + columnsSql + " FROM "
				+ mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " " + DEFAULT_TABLE_ALIAS;
		
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

		PropertyMapping createdOnPropMapping = tableMapping.getCreatedOnPropertyMapping();
		if (createdOnPropMapping != null) {
			bw.setPropertyValue(createdOnPropMapping.getPropertyName(), now);
		}

		PropertyMapping updatedOnPropMapping = tableMapping.getUpdatedOnPropertyMapping();
		if (updatedOnPropMapping != null) {
			bw.setPropertyValue(updatedOnPropMapping.getPropertyName(), now);
		}

		PropertyMapping createdByPropMapping = tableMapping.getCreatedByPropertyMapping();
		if (createdByPropMapping != null && recordOperatorResolver != null) {
			bw.setPropertyValue(createdByPropMapping.getPropertyName(), recordOperatorResolver.getRecordOperator());
		}

		PropertyMapping updatedByPropMapping = tableMapping.getUpdatedByPropertyMapping();
		if (updatedByPropMapping != null && recordOperatorResolver != null) {
			bw.setPropertyValue(updatedByPropMapping.getPropertyName(), recordOperatorResolver.getRecordOperator());
		}

		PropertyMapping versionPropMapping = tableMapping.getVersionPropertyMapping();
		if (versionPropMapping != null) {
			bw.setPropertyValue(versionPropMapping.getPropertyName(), 1);
		}

		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			mapSqlParameterSource.addValue(propMapping.getColumnName(),
					bw.getPropertyValue(propMapping.getPropertyName()),
					tableMapping.getPropertySqlType(propMapping.getPropertyName()));

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
			Number idNumber = jdbcInsert.executeAndReturnKey(mapSqlParameterSource);
			bw.setPropertyValue(tableMapping.getIdPropertyName(), idNumber); // set object id value
		} else {
			jdbcInsert.execute(mapSqlParameterSource);
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

		PropertyMapping updatedByPropMapping = tableMapping.getUpdatedByPropertyMapping();
		if (updatedByPropMapping != null && recordOperatorResolver != null
				&& parameters.contains(updatedByPropMapping.getPropertyName())) {
			bw.setPropertyValue(updatedByPropMapping.getPropertyName(), recordOperatorResolver.getRecordOperator());
		}

		PropertyMapping updatedOnPropMapping = tableMapping.getUpdatedOnPropertyMapping();
		if (updatedOnPropMapping != null && parameters.contains(updatedOnPropMapping.getPropertyName())) {
			bw.setPropertyValue(updatedOnPropMapping.getPropertyName(), LocalDateTime.now());
		}

		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		for (String paramName : parameters) {
			if (paramName.equals("incrementedVersion")) {
				Integer versionVal = (Integer) bw
						.getPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName());
				if (versionVal == null) {
					throw new MapperException("JdbcTemplateMapper is configured for versioning so "
							+ tableMapping.getVersionPropertyMapping().getPropertyName()
							+ " cannot be null when updating " + obj.getClass().getSimpleName());
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
						"update failed for " + obj.getClass().getSimpleName() + " . " + tableMapping.getIdPropertyName()
								+ ": " + bw.getPropertyValue(tableMapping.getIdPropertyName()) + " and "
								+ tableMapping.getVersionPropertyMapping().getPropertyName() + ": "
								+ bw.getPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName()));
			}
			// update the version in object with new version
			bw.setPropertyValue(tableMapping.getVersionPropertyMapping().getPropertyName(),
					mapSqlParameterSource.getValue("incrementedVersion"));
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

		String sql = "DELETE FROM " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " WHERE "
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
		String sql = "DELETE FROM " + mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()) + " WHERE "
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
		return new SelectMapper<T>(clazz, tableAlias, mappingHelper, conversionService,
				useColumnLabelForResultSetMetaData);
	}

	private SqlAndParams buildSqlAndParamsForUpdate(TableMapping tableMapping) {
		Assert.notNull(tableMapping, "tableMapping must not be null");

		// ignore these attributes when generating the sql 'SET' command
		List<String> ignoreAttrs = new ArrayList<>();
		ignoreAttrs.add(tableMapping.getIdPropertyName());
		PropertyMapping createdOnPropMapping = tableMapping.getCreatedOnPropertyMapping();
		if (createdOnPropMapping != null) {
			ignoreAttrs.add(createdOnPropMapping.getPropertyName());
		}
		PropertyMapping createdByPropMapping = tableMapping.getCreatedByPropertyMapping();
		if (createdByPropMapping != null) {
			ignoreAttrs.add(createdByPropMapping.getPropertyName());
		}

		Set<String> params = new HashSet<>();
		StringBuilder sqlBuilder = new StringBuilder("UPDATE ");
		sqlBuilder.append(mappingHelper.fullyQualifiedTableName(tableMapping.getTableName()));
		sqlBuilder.append(" SET ");

		PropertyMapping versionPropMapping = tableMapping.getVersionPropertyMapping();
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

			if (versionPropMapping != null
					&& propMapping.getPropertyName().equals(versionPropMapping.getPropertyName())) {
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
		if (versionPropMapping != null) {
			sqlBuilder.append(" AND ").append(versionPropMapping.getColumnName()).append(" = :")
					.append(versionPropMapping.getPropertyName());
			params.add(versionPropMapping.getPropertyName());
		}

		String updateSql = sqlBuilder.toString();
		SqlAndParams updateSqlAndParams = new SqlAndParams(updateSql, params);

		return updateSqlAndParams;
	}

	private <T> String getFindColumnsSql(TableMapping tableMapping, Class<T> clazz) {
		String columnsSql = findColumnsSqlCache.get(clazz);
		if (columnsSql == null) {
			StringJoiner sj = new StringJoiner(", ", " ", " ");
			for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
				sj.add(DEFAULT_TABLE_ALIAS + "." + propMapping.getColumnName() + " AS "
						+ AppUtils.convertPropertyNameToUnderscoreName(propMapping.getPropertyName()));
			}
			columnsSql = sj.toString();
			findColumnsSqlCache.put(clazz, columnsSql);
		}
		return columnsSql;
	}

}
