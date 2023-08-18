package io.github.jdbctemplatemapper.core;

import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import io.github.jdbctemplatemapper.exception.AnnotationException;

class MappingHelper {
	// Map key - object class
	// value - the table mapping
	private Map<Class<?>, TableMapping> objectToTableMappingCache = new ConcurrentHashMap<>();

	// workaround for postgres driver bug for ResultSetMetaData
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
	 * @param jdbcTemplate              The jdbcTemplate
	 * @param schemaName                database schema name.
	 * @param catalogName               database catalog name.
	 * @param metaDataColumnNamePattern For most jdbc drivers getting column
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
			TableColumnInfo tableColumnInfo = getTableColumnInfo(clazz);			
			String tableName = tableColumnInfo.getTableName();
			
            IdPropertyInfo idPropertyInfo = getIdPropertyInfo(clazz);
					
			// key:column name, value: ColumnInfo
			Map<String, ColumnInfo> columnNameToColumnInfo = tableColumnInfo.getColumnInfos().stream()
					.collect(Collectors.toMap(o -> o.getColumnName(), o -> o));

			// key:propertyName, value:PropertyMapping. LinkedHashMap to maintain order of properties
			Map<String, PropertyMapping> propNameToPropertyMapping = new LinkedHashMap<>();
			for (Field field : clazz.getDeclaredFields()) {
				String propertyName = field.getName();

				Column colAnnotation = AnnotationUtils.findAnnotation(field, Column.class);
				if (colAnnotation != null) {
					String colName = colAnnotation.name();
					if ("[DEFAULT]".equals(colName)) {
						colName = AppUtils.toUnderscoreName(propertyName);
					}
					colName = AppUtils.toLowerCase(colName);
					if (!columnNameToColumnInfo.containsKey(colName)) {
						throw new AnnotationException("column " + colName + " not found in table " + tableName
								+ " for model property " + clazz.getSimpleName() + "." + propertyName);
					}
					propNameToPropertyMapping.put(propertyName, new PropertyMapping(propertyName, field.getType(),
							colName, columnNameToColumnInfo.get(colName).getColumnSqlDataType()));
				}

				Id idAnno = AnnotationUtils.findAnnotation(field, Id.class);
				if (idAnno != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						propMapping = getPropertyMapping(field, tableName, columnNameToColumnInfo);
						propMapping.setIdAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					} else {
						propMapping.setIdAnnotation(true);
					}
				}

				Version versionAnnotation = AnnotationUtils.findAnnotation(field, Version.class);
				if (versionAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						propMapping = getPropertyMapping(field, tableName, columnNameToColumnInfo);
						propMapping.setVersionAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					} else {
						propMapping.setVersionAnnotation(true);
					}
				}

				CreatedOn createdOnAnnotation = AnnotationUtils.findAnnotation(field, CreatedOn.class);
				if (createdOnAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						propMapping = getPropertyMapping(field, tableName, columnNameToColumnInfo);
						propMapping.setCreatedOnAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					} else {
						propMapping.setCreatedOnAnnotation(true);
					}

				}
				UpdatedOn updatedOnAnnotation = AnnotationUtils.findAnnotation(field, UpdatedOn.class);
				if (updatedOnAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						propMapping = getPropertyMapping(field, tableName, columnNameToColumnInfo);
						propMapping.setUpdatedOnAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					} else {
						propMapping.setUpdatedOnAnnotation(true);
					}
				}

				CreatedBy createdByAnnotation = AnnotationUtils.findAnnotation(field, CreatedBy.class);
				if (createdByAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						propMapping = getPropertyMapping(field, tableName, columnNameToColumnInfo);
						propMapping.setCreatedByAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					} else {
						propMapping.setCreatedByAnnotation(true);
					}
				}
				UpdatedBy updatedByAnnotation = AnnotationUtils.findAnnotation(field, UpdatedBy.class);
				if (updatedByAnnotation != null) {
					PropertyMapping propMapping = propNameToPropertyMapping.get(propertyName);
					if (propMapping == null) {
						propMapping = getPropertyMapping(field, tableName, columnNameToColumnInfo);
						propMapping.setUpdatedByAnnotation(true);
						propNameToPropertyMapping.put(propertyName, propMapping);
					} else {
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

			List<PropertyMapping> propertyMappings = new ArrayList<>(propNameToPropertyMapping.values());
			validateAnnotations(propertyMappings, clazz);

			tableMapping = new TableMapping(clazz, tableName, idPropertyInfo.getPropertyName(), propertyMappings);
			tableMapping.setIdAutoIncrement(idPropertyInfo.isIdAutoIncrement());
			
			objectToTableMappingCache.put(clazz, tableMapping);
		}
		return tableMapping;
	}
	
	private IdPropertyInfo getIdPropertyInfo(Class<?> clazz) {
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
			throw new AnnotationException(
					"@Id annotation not found in class " + clazz.getSimpleName() + " . It is required");
		}
		return new IdPropertyInfo(clazz, idPropertyName,isIdAutoIncrement);
	}

	private TableColumnInfo getTableColumnInfo(Class<?> clazz) {
		Table tableAnnotation = AnnotationUtils.findAnnotation(clazz, Table.class);
		validateTableAnnotation(tableAnnotation, clazz);
		
		String annotationTableName = tableAnnotation.name();
		String tableName = annotationTableName;
		List<ColumnInfo> columnInfoList = getColumnInfoFromDatabaseMetadata(tableName);
		if (AppUtils.isEmpty(columnInfoList)) {
			tableName = tableName.toUpperCase();
			// try again with upper case table name
			columnInfoList = getColumnInfoFromDatabaseMetadata(tableName);

			if (AppUtils.isEmpty(columnInfoList)) {
				tableName = tableName.toLowerCase();
				// try again with lower case table name
				columnInfoList = getColumnInfoFromDatabaseMetadata(tableName);

				if (AppUtils.isEmpty(columnInfoList)) {
					throw new AnnotationException(
							"Could not find table " + annotationTableName + " for class " + clazz.getSimpleName());
				}		
		    }
		}
		return new TableColumnInfo(tableName, columnInfoList);
	}

	private List<ColumnInfo> getColumnInfoFromDatabaseMetadata(String tableName) {
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
		if (AppUtils.isNotEmpty(getSchemaName())) {
			return getSchemaName() + "." + tableName;
		}
		return tableName;
	}

	private PropertyMapping getPropertyMapping(Field field, String tableName,
			Map<String, ColumnInfo> columnNameToColumnInfo) {
		String propertyName = field.getName();
		String colName = AppUtils.toUnderscoreName(field.getName());
		if (!columnNameToColumnInfo.containsKey(colName)) {
			throw new AnnotationException("column " + colName + " not found in table " + tableName
					+ " for model property " + field.getDeclaringClass().getSimpleName() + "." + field.getName());
		}
		return new PropertyMapping(propertyName, field.getType(), colName,
				columnNameToColumnInfo.get(colName).getColumnSqlDataType());
	}

	private void validateTableAnnotation(Table tableAnnotation, Class<?> clazz) {
		if (tableAnnotation == null) {
			throw new AnnotationException(
					clazz.getSimpleName() + " does not have the @Table annotation. It is required");
		}

		if (AppUtils.isEmpty(tableAnnotation.name().trim())) {
			throw new AnnotationException("For " + clazz.getSimpleName() + " the @Table annotation has a blank name");
		}
	}

	private void validateAnnotations(List<PropertyMapping> propertyMappings, Class<?> clazz) {
		int idCnt = 0;
		int versionCnt = 0;
		int createdByCnt = 0;
		int createdOnCnt = 0;
		int updatedOnCnt = 0;
		int updatedByCnt = 0;

		for (PropertyMapping propMapping : propertyMappings) {
			int conflictCnt = 0;

			if (propMapping.isIdAnnotation()) {
				idCnt++;
				conflictCnt++;
			}
			if (propMapping.isVersionAnnotation()) {
				versionCnt++;
				conflictCnt++;
			}
			if (propMapping.isCreatedOnAnnotation()) {
				createdOnCnt++;
				conflictCnt++;
			}
			if (propMapping.isCreatedByAnnotation()) {
				createdByCnt++;
				conflictCnt++;
			}
			if (propMapping.isUpdatedOnAnnotation()) {
				updatedOnCnt++;
				conflictCnt++;
			}
			if (propMapping.isUpdatedByAnnotation()) {
				updatedByCnt++;
				conflictCnt++;
			}

			if (propMapping.isVersionAnnotation() && Integer.class != propMapping.getPropertyType()) {
				throw new AnnotationException("@Version requires the type of property " + clazz.getSimpleName() + "."
						+ propMapping.getPropertyName() + " to be Integer");
			}

			if (propMapping.isCreatedOnAnnotation() && LocalDateTime.class != propMapping.getPropertyType()) {
				throw new AnnotationException("@CreatedOn requires the type of property " + clazz.getSimpleName() + "."
						+ propMapping.getPropertyName() + " to be LocalDateTime");
			}

			if (propMapping.isUpdatedOnAnnotation() && LocalDateTime.class != propMapping.getPropertyType()) {
				throw new AnnotationException("@UpdatedOn requires the type of property " + clazz.getSimpleName() + "."
						+ propMapping.getPropertyName() + " to be LocalDateTime");
			}

			if (conflictCnt > 1) {
				throw new AnnotationException(clazz.getSimpleName() + "." + propMapping.getPropertyName()
						+ " has multiple annotations that conflict");
			}
		}

		if (idCnt > 1) {
			throw new AnnotationException(" model " + clazz.getSimpleName() + " has multiple @Id annotations");
		}
		if (versionCnt > 1) {
			throw new AnnotationException(" model " + clazz.getSimpleName() + " has multiple @Version annotations");
		}
		if (createdOnCnt > 1) {
			throw new AnnotationException(" model " + clazz.getSimpleName() + " has multiple @CreatedOn annotations");
		}
		if (createdByCnt > 1) {
			throw new AnnotationException(" model " + clazz.getSimpleName() + " has multiple @CreatedBy annotations");
		}
		if (updatedOnCnt > 1) {
			throw new AnnotationException(" model " + clazz.getSimpleName() + " has multiple @UpdatedOn annotations");
		}
		if (updatedByCnt > 1) {
			throw new AnnotationException(" model " + clazz.getSimpleName() + " has multiple @UpdatedBy annotations");
		}
	}
}
