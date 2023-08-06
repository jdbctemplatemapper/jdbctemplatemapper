package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * The database table mapping details on an object
 *
 * @author ajoseph
 */
class TableMapping {
	private Class<?> tableClass;
	private String tableName;
	private String idPropertyName;
	private boolean idAutoIncrement = false;

	// object property to database column mapping.
	// Only properties which have corresponding database column will be in this
	// list.
	private List<PropertyMapping> propertyMappings = new ArrayList<>();

	// these 2 maps used for performance
	private Map<String, PropertyMapping> columnNameMap = new HashMap<>();
	private Map<String, PropertyMapping> propertyNameMap = new HashMap<>();

	public TableMapping(Class<?> tableClass, String tableName, String idPropertyName,
			List<PropertyMapping> propertyMappings) {
		Assert.notNull(tableClass, "tableClass must not be null");
		Assert.notNull(tableName, "tableName must not be null");
		Assert.notNull(idPropertyName, "idPropertyName must not be null");
		this.tableClass = tableClass;
		this.tableName = tableName;
		this.idPropertyName = idPropertyName;
		this.propertyMappings = propertyMappings;
		if (propertyMappings != null) {
			for (PropertyMapping propMapping : propertyMappings) {
				// these 2 maps used for performance
				columnNameMap.put(propMapping.getColumnName(), propMapping);
				propertyNameMap.put(propMapping.getPropertyName(), propMapping);
			}
		}
	}

	public String getColumnName(String propertyName) {
		PropertyMapping propMapping = propertyNameMap.get(propertyName);
		return propMapping == null ? null : propMapping.getColumnName();
	}

	public String getProperyName(String columnName) {
		PropertyMapping propMapping = columnNameMap.get(columnName.toLowerCase());
		return propMapping == null ? null : propMapping.getPropertyName();
	}

	public Class<?> getPropertyType(String propertyName) {
		PropertyMapping propMapping = propertyNameMap.get(propertyName);
		return propMapping == null ? null : propMapping.getPropertyType();
	}

	public int getPropertySqlType(String propertyName) {
		PropertyMapping propMapping = propertyNameMap.get(propertyName);
		return propMapping == null ? 0 : propMapping.getColumnSqlDataType();
	}

	public Class<?> getTableClass() {
		return tableClass;
	}

	public String getTableName() {
		return tableName;
	}

	public String getIdPropertyName() {
		return getIdPropertyMapping().getPropertyName();
	}

	public String getIdColumnName() {
		return getIdPropertyMapping().getColumnName();
	}

	public void setIdAutoIncrement(boolean val) {
		this.idAutoIncrement = val;
	}

	public boolean isIdAutoIncrement() {
		return idAutoIncrement;
	}

	public PropertyMapping getIdPropertyMapping() {
		PropertyMapping propMapping = propertyNameMap.get(idPropertyName);
		if (propMapping != null) {
			return propMapping;
		} else {
			throw new RuntimeException("For @Id property " + idPropertyName
					+ " could not find corresponding column in database table " + tableName);
		}
	}

	public List<PropertyMapping> getPropertyMappings() {
		return propertyMappings;
	}

}
