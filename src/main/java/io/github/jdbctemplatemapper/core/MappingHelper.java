package io.github.jdbctemplatemapper.core;

import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedBy;
import io.github.jdbctemplatemapper.annotation.CreatedOn;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedBy;
import io.github.jdbctemplatemapper.annotation.UpdatedOn;
import io.github.jdbctemplatemapper.annotation.Version;
import io.github.jdbctemplatemapper.exception.MapperException;

class MappingHelper {

	// Convert camel case to underscore case regex pattern. Pattern is thread safe
	private static Pattern TO_UNDERSCORE_NAME_PATTERN = Pattern.compile("(.)(\\p{Upper})");

	// Map key - table name,
	// value - the list of database column names
	private Map<String, List<ColumnInfo>> tableColumnInfoCache = new ConcurrentHashMap<>();

	// Map key - object class
	// value - the table mapping
	private Map<Class<?>, TableMapping> objectToTableMappingCache = new ConcurrentHashMap<>();

	// workaround for postgres driver bug.
	private boolean forcePostgresTimestampWithTimezone = false;

	private final JdbcTemplate jdbcTemplate;
	private final String schemaName;
	private final String catalogName;

	// For most jdbc drivers when getting column metadata using jdbc, the
	// columnPattern argument null
	// returns all the columns (which is the default for JdbcTemplateMapper). Some
	// jdbc drivers may require to pass something like '%'.
	private final String metaDataColumnNamePattern;

	/**
	 * Constructor.
	 *
	 * @param jdbcTemplate              - The jdbcTemplate
	 * @param schemaName                - database schema name.
	 * @param catalogName               - database catalog name.
	 * @param metaDataColumnNamePattern - For most jdbc drivers getting column
	 *                                  metadata from database the
	 *                                  metaDataColumnNamePattern argument of null
	 *                                  returns all the columns (which is the
	 *                                  default for JdbcTemplateMapper). Some jdbc
	 *                                  drivers may require to pass something like
	 *                                  '%'.
	 */
	public MappingHelper(JdbcTemplate jdbcTemplate, String schemaName, String catalogName,
			String metaDataColumnNamePattern) {
		this.jdbcTemplate = jdbcTemplate;
		this.schemaName = schemaName;
		this.catalogName = catalogName;
		this.metaDataColumnNamePattern = metaDataColumnNamePattern;
	}

	public void forcePostgresTimestampWithTimezone(boolean val) {
		this.forcePostgresTimestampWithTimezone = val;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getCatalogName() {
		return catalogName;
	}

	public String getMetaDataColumnNamePattern() {
		return metaDataColumnNamePattern;
	}

	/**
	 * Gets the table mapping for the Object. The table mapping has the table name
	 * and and object property to database column mapping.
	 *
	 * <p>
	 * Table name is either from the @Tabel annotation or the underscore case
	 * conversion of the Object name.
	 *
	 * @param clazz The object class
	 * @return The table mapping.
	 */
	public TableMapping getTableMapping(Class<?> clazz) {
		Assert.notNull(clazz, "clazz must not be null");
		TableMapping tableMapping = objectToTableMappingCache.get(clazz);

		if (tableMapping == null) {
			Table tableAnnotation = AnnotationUtils.findAnnotation(clazz, Table.class);
			if (tableAnnotation == null) {
				throw new MapperException(clazz.getName() + " does not have the @Table annotation");
			}
			String tableName = tableAnnotation.name();
			Id idAnnotation = null;
			String idPropertyName = null;
			boolean isIdAutoIncrement = false;
			for (Field field : clazz.getDeclaredFields()) {
				idAnnotation = AnnotationUtils.findAnnotation(field, Id.class);
				if (idAnnotation != null) {
					idPropertyName = field.getName();
					if (idAnnotation.type() == IdType.AUTO_INCREMENT) {
						isIdAutoIncrement = true;
					}
					break;
				}
			}
			if (idAnnotation == null) {
				throw new RuntimeException("@Id annotation not found in class " + clazz.getName());
			}

			List<ColumnInfo> columnInfoList = getTableColumnInfo(tableName);
			if (isEmpty(columnInfoList)) {
				// try again with upper case table name
				tableName = tableName.toUpperCase();
				columnInfoList = getTableColumnInfo(tableName);
				if (isEmpty(columnInfoList)) {
					throw new MapperException("Could not find corresponding table for class " + clazz.getSimpleName());
				}
			}

			Map<String, ColumnInfo> columnNameToColumnInfo = columnInfoList.stream()
					.collect(Collectors.toMap(o -> o.getColumnName(), o -> o));

			Map<String, PropertyMapping> propNameToPropertyMapping = new LinkedHashMap<>();
			for (Field field : clazz.getDeclaredFields()) {
				String propertyName = field.getName();
				Column colAnnotation = AnnotationUtils.findAnnotation(field, Column.class);
				if (colAnnotation != null) {
					String colName = colAnnotation.name();
					if ("[DEFAULT]".equals(colName)) {
						colName = convertPropertyNameToUnderscoreName(propertyName);
					}
					if (!columnNameToColumnInfo.containsKey(colName)) {
						throw new MapperException("column " + colName + " not found in table " + tableName
								+ " for model property " + clazz.getSimpleName() + "." + propertyName);
					}
					propNameToPropertyMapping.put(propertyName, new PropertyMapping(propertyName, field.getType(),
							colName, columnNameToColumnInfo.get(colName).getColumnSqlDataType()));
				}

				Id idAnno = AnnotationUtils.findAnnotation(field, Id.class);
				if (idAnno != null) {
					if (propNameToPropertyMapping.get(propertyName) == null) {
						String colName = convertPropertyNameToUnderscoreName(propertyName);
						PropertyMapping propMapping = new PropertyMapping(propertyName, field.getType(), colName,
								columnNameToColumnInfo.get(colName).getColumnSqlDataType());
						propNameToPropertyMapping.put(propertyName, propMapping);
					}
				}

				Version versionAnnotation = AnnotationUtils.findAnnotation(field, Version.class);
				if (versionAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						String colName = convertPropertyNameToUnderscoreName(propertyName);
						propMapping = new PropertyMapping(propertyName, field.getType(), colName,
								columnNameToColumnInfo.get(colName).getColumnSqlDataType());
						propMapping.setVersionAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					}
					else {
						propMapping.setVersionAnnotation(true);
					}
				}

				CreatedOn createdOnAnnotation = AnnotationUtils.findAnnotation(field, CreatedOn.class);
				if (createdOnAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						String colName = convertPropertyNameToUnderscoreName(propertyName);
						propMapping = new PropertyMapping(propertyName, field.getType(), colName,
								columnNameToColumnInfo.get(colName).getColumnSqlDataType());
						propMapping.setCreatedOnAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					}
					else {
						propMapping.setCreatedOnAnnotation(true);
					}
				}
				UpdatedOn updatedOnAnnotation = AnnotationUtils.findAnnotation(field, UpdatedOn.class);
				if (updatedOnAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						String colName = convertPropertyNameToUnderscoreName(propertyName);
						propMapping = new PropertyMapping(propertyName, field.getType(), colName,
								columnNameToColumnInfo.get(colName).getColumnSqlDataType());
						propMapping.setUpdatedOnAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					}
					else {
						propMapping.setUpdatedOnAnnotation(true);
					}
				}

				CreatedBy createdByAnnotation = AnnotationUtils.findAnnotation(field, CreatedBy.class);
				if (createdByAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						String colName = convertPropertyNameToUnderscoreName(propertyName);
						propMapping = new PropertyMapping(propertyName, field.getType(), colName,
								columnNameToColumnInfo.get(colName).getColumnSqlDataType());
						propMapping.setCreatedByAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					}
					else {
						propMapping.setCreatedByAnnotation(true);
					}
				}
				UpdatedBy updatedByAnnotation = AnnotationUtils.findAnnotation(field, UpdatedBy.class);
				if (updatedByAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						String colName = convertPropertyNameToUnderscoreName(propertyName);
						propMapping = new PropertyMapping(propertyName, field.getType(), colName,
								columnNameToColumnInfo.get(colName).getColumnSqlDataType());
						propMapping.setUpdatedByAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					}
					else {
						propMapping.setUpdatedByAnnotation(true);
					}
				}

				// postgres driver bug where the database metadata returns TIMESTAMP instead of
				// TIMESTAMP_WITH_TIMEZONE for columns timestamptz.
				if (forcePostgresTimestampWithTimezone) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping != null) {
						if (OffsetDateTime.class == propMapping.getPropertyType()
								&& propMapping.getColumnSqlDataType() == Types.TIMESTAMP) {
							propMapping.setColumnSqlDataType(Types.TIMESTAMP_WITH_TIMEZONE);
						}
					}
				}
			}

			tableMapping = new TableMapping(clazz, tableName, idPropertyName,
					new ArrayList<PropertyMapping>(propNameToPropertyMapping.values()));
			tableMapping.setIdAutoIncrement(isIdAutoIncrement);
		}

		objectToTableMappingCache.put(clazz, tableMapping);
		return tableMapping;
	}

	public List<ColumnInfo> getTableColumnInfo(String tableName) {
		Assert.hasLength(tableName, "tableName must not be empty");
		try {
			return JdbcUtils.extractDatabaseMetaData(jdbcTemplate.getDataSource(),
					new DatabaseMetaDataCallback<List<ColumnInfo>>() {
						public List<ColumnInfo> processMetaData(DatabaseMetaData dbMetadata)
								throws SQLException, MetaDataAccessException {
							ResultSet rs = null;
							try {
								List<ColumnInfo> columnInfoList = new ArrayList<>();
								rs = dbMetadata.getColumns(catalogName, schemaName, tableName,
										metaDataColumnNamePattern);
								while (rs.next()) {
									columnInfoList
											.add(new ColumnInfo(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE")));
								}
								if (isNotEmpty(columnInfoList)) {
									tableColumnInfoCache.put(tableName, columnInfoList);
								}
								return columnInfoList;
							} finally {
								JdbcUtils.closeResultSet(rs);
							}
						}
					});
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	/**
	 * Get the fully qualified table name. If schema is present then will be
	 * schemaName.tableName otherwise just tableName
	 *
	 * @param tableName The table name
	 * @return The fully qualified table name
	 */
	public String fullyQualifiedTableName(String tableName) {
		Assert.hasLength(tableName, "tableName must not be empty");
		if (isNotEmpty(getSchemaName())) {
			return getSchemaName() + "." + tableName;
		}
		return tableName;
	}

	public boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	public boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	@SuppressWarnings("all")
	public boolean isEmpty(Collection coll) {
		return (coll == null || coll.isEmpty());
	}

	@SuppressWarnings("all")
	public boolean isNotEmpty(Collection coll) {
		return !isEmpty(coll);
	}

	/**
	 * Converts underscore case to camel case. Ex: user_last_name gets converted to
	 * userLastName.
	 *
	 * @param str snake case string
	 * @return the camel case string
	 */
	public String convertSnakeToCamelCase(String str) {
		return JdbcUtils.convertUnderscoreNameToPropertyName(str);
	}

	/**
	 * Converts camel case to underscore case. Ex: userLastName gets converted to
	 * user_last_name.
	 *
	 * @param str underscore case string
	 * @return the camel case string
	 */
	public String convertPropertyNameToUnderscoreName(String str) {
		return TO_UNDERSCORE_NAME_PATTERN.matcher(str).replaceAll("$1_$2").toLowerCase();
	}

}
