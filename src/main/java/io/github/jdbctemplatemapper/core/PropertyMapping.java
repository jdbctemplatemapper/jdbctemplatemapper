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
	
	private boolean idAnnotation = false;
	private boolean createdOnAnnotation = false;
	private boolean updatedOnAnnotation = false;
	private boolean versionAnnotation = false;
	private boolean createdByAnnotation = false;
	private boolean updatedByAnnotation = false;

	public PropertyMapping(String propertyName, Class<?> propertyType, String columnName, int columnSqlDataType) {
		if (propertyName == null || propertyType == null || columnName == null) {
			throw new IllegalArgumentException("propertyName, propertyType, columnName must not be null");
		}
		this.propertyName = propertyName;
		this.propertyType = propertyType;
		this.columnName = AppUtils.toLowerCase(columnName); // column names stored in lower case always
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
	
	public boolean isIdAnnotation() {
		return idAnnotation;
	}

	public void setIdAnnotation(boolean idAnnotation) {
		this.idAnnotation = idAnnotation;
	}

	public boolean isCreatedOnAnnotation() {
		return createdOnAnnotation;
	}

	public void setCreatedOnAnnotation(boolean createdOnAnnotation) {
		this.createdOnAnnotation = createdOnAnnotation;
	}

	public boolean isUpdatedOnAnnotation() {
		return updatedOnAnnotation;
	}

	public void setUpdatedOnAnnotation(boolean updatedOnAnnotation) {
		this.updatedOnAnnotation = updatedOnAnnotation;
	}

	public boolean isVersionAnnotation() {
		return versionAnnotation;
	}

	public void setVersionAnnotation(boolean versionAnnotation) {
		this.versionAnnotation = versionAnnotation;
	}

	public boolean isCreatedByAnnotation() {
		return createdByAnnotation;
	}

	public void setCreatedByAnnotation(boolean createdByAnnotation) {
		this.createdByAnnotation = createdByAnnotation;
	}

	public boolean isUpdatedByAnnotation() {
		return updatedByAnnotation;
	}

	public void setUpdatedByAnnotation(boolean updatedByAnnotation) {
		this.updatedByAnnotation = updatedByAnnotation;
	}	
}
