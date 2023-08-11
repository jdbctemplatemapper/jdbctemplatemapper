package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.StringJoiner;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.exception.MapperException;

/**
 * <pre>
 * Allows generating the select columns string for the model and population of
 * the model from a ResultSet.
 * 
 * See <a href=
"https://github.com/jdbctemplatemapper/jdbctemplatemapper#querying-relationships">Querying relationships</a> for more info
 * </pre>
 * 
 * @author ajoseph
 */
public class SelectMapper<T> {
	private final MappingHelper mappingHelper;
	private final Class<T> clazz;
	private final ConversionService conversionService;
	private final boolean useColumnLabelForResultSetMetaData;
	private final String colPrefix; // tableAlias + "."
	private final String colAliasPrefix; // tableAlias + "_"

	SelectMapper(Class<T> clazz, String tableAlias, MappingHelper mappingHelper,
			ConversionService conversionService, boolean useColumnLabelForResultSetMetaData) {
		Assert.notNull(clazz, " clazz cannot be empty");
		Assert.hasLength(tableAlias, " tableAlias cannot be empty");
		if (tableAlias.trim().length() < 1) {
			throw new MapperException("tableAlias cannot be empty");
		}

		this.clazz = clazz;

		this.mappingHelper = mappingHelper;
		this.conversionService = conversionService;

		this.useColumnLabelForResultSetMetaData = useColumnLabelForResultSetMetaData;
		this.colPrefix = tableAlias + ".";
		this.colAliasPrefix = tableAlias + "_";

	}

	/**
	 * Generates a string which can be used in a sql select statement for all the
	 * properties which have corresponding database columns
	 *
	 * <pre>
	 * SelectMapper selectMapper = jdbcTemplateMapper.getSelectMapper(Employee.class, "emp");
	 * selectMapper.getColumnSql() will return something like below:
	 * 
	 * "emp.id emp_id, emp.last_name emp_last_name, emp.first_name emp_first_name"
	 * </pre>
	 *
	 * @return comma separated select column string
	 */
	public String getColumnsSql() {
		StringJoiner sj = new StringJoiner(", ", " ", " ");
		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			sj.add(colPrefix + propMapping.getColumnName() + " " + colAliasPrefix + propMapping.getColumnName());
		}
		return sj.toString();
	}

	/**
	 * gets column alias of the models id in sql statement
	 * 
	 * @return the column alias of the models id in sql statement
	 */
	public String getResultSetModelIdColumnLabel() {
		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		return colAliasPrefix + tableMapping.getIdColumnName();
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
			bw.setConversionService(conversionService);

			ResultSetMetaData rsMetaData = rs.getMetaData();
			int count = rsMetaData.getColumnCount();
			for (int i = 1; i <= count; i++) {
				// support for older drivers
				String columnLabel = useColumnLabelForResultSetMetaData ? rsMetaData.getColumnLabel(i)
						: rsMetaData.getColumnName(i);
				// case insensitive prefix match
				if (colAliasPrefix.regionMatches(true, 0, columnLabel, 0, colAliasPrefix.length())) {
					String propertyName = tableMapping.getProperyName(columnLabel.substring(colAliasPrefix.length()));
					if (propertyName != null) {
						bw.setPropertyValue(propertyName,
								JdbcUtils.getResultSetValue(rs, i, tableMapping.getPropertyType(propertyName)));
					}
				}
			}
			// if id is null return null. Does not matter if other fields have values.
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
