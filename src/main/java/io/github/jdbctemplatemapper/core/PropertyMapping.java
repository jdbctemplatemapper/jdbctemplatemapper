package io.github.jdbctemplatemapper.core;

/**
 * object property to database column mapping.
 *
 * @author ajoseph
 */
class PropertyMapping {
	private String propertyName;
	private Class<?> propertyType;
	private String columnName;
	private int columnSqlDataType; // see java.sql.Types

	public PropertyMapping(String propertyName, Class<?> propertyType, String columnName, int columnSqlDataType) {
		if (propertyName == null || propertyType == null || columnName == null) {
			throw new IllegalArgumentException("propertyName, propertyType, columnName must not be null");
		}
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.columnName = columnName.toLowerCase(); // column names stored in lower case always
		this.columnSqlDataType = columnSqlDataType;
	}

	public void setColumnSqlDataType(int columnSqlDataType) {
		this.columnSqlDataType = columnSqlDataType;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Class<?> getPropertyType() {
		return propertyType;
	}

	public String getColumnName() {
		return columnName;
	}

	public int getColumnSqlDataType() {
		return columnSqlDataType;
	}

}
