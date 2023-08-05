package io.github.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;

import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.exception.MapperException;

class MappingHelper {

	// Convert camel case to snake case regex pattern. Pattern is thread safe
	private static Pattern TO_SNAKE_CASE_PATTERN = Pattern.compile("(.)(\\p{Upper})");

	// Map key - table name,
	// value - the list of database column names
	private Map<String, List<ColumnInfo>> tableColumnInfoCache = new ConcurrentHashMap<>();

	// Map key - object class
	// value - the table mapping
	private Map<Class<?>, TableMapping> objectToTableMappingCache = new ConcurrentHashMap<>();

	// Map key - camel case string,
	// value - snake case string
	private Map<String, String> camelToSnakeCache = new ConcurrentHashMap<>();

	// Map key - object class
	// value - list of property names
	private Map<Class<?>, List<PropertyInfo>> objectPropertyInfoCache = new ConcurrentHashMap<>();

	private final JdbcTemplate jdbcTemplate;
	private final String schemaName;
	private final String catalogName;

	// For most jdbc drivers when getting column metadata using jdbc, the
	// columnPattern argument null
	// returns all the columns (which is the default for JdbcTemplateMapper). Some
	// jdbc drivers may
	// require to pass something like '%'.
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
	 * Table name is either from the @Tabel annotation or the snake case conversion
	 * of the Object name.
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

			// if code reaches here table exists and class has @Id annotation
			try {
				List<PropertyInfo> propertyInfoList = getObjectPropertyInfo(clazz.newInstance());
				List<PropertyMapping> propertyMappings = new ArrayList<>();
				// Match database table columns to the Object properties
				for (ColumnInfo columnInfo : columnInfoList) {
					// property name corresponding to column name
					String propertyName = convertSnakeToCamelCase(columnInfo.getColumnName());
					// check if the property exists for the Object
					PropertyInfo propertyInfo = propertyInfoList.stream()
							.filter(pi -> propertyName.equals(pi.getPropertyName())).findAny().orElse(null);

					// add matched object property info and table column info to mappings
					if (propertyInfo != null) {
						PropertyMapping propertyMapping = new PropertyMapping(propertyInfo.getPropertyName(),
								propertyInfo.getPropertyType(), columnInfo.getColumnName(),
								columnInfo.getColumnSqlDataType());
						propertyMappings.add(propertyMapping);
					}
				}

				tableMapping = new TableMapping(clazz, tableName, idPropertyName, propertyMappings);
				tableMapping.setIdAutoIncrement(isIdAutoIncrement);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		objectToTableMappingCache.put(clazz, tableMapping);
		return tableMapping;
	}

	public List<ColumnInfo> getTableColumnInfo(String tableName) {
		Assert.hasLength(tableName, "tableName must not be empty");
		try {
			List<ColumnInfo> columnInfos = tableColumnInfoCache.get(tableName);
			if (isEmpty(columnInfos)) {
				// Using Spring JdbcUtils.extractDatabaseMetaData() since it has some robust
				// processing for
				// connection access
				columnInfos = JdbcUtils.extractDatabaseMetaData(jdbcTemplate.getDataSource(),
						new DatabaseMetaDataCallback<List<ColumnInfo>>() {
							public List<ColumnInfo> processMetaData(DatabaseMetaData dbMetadata)
									throws SQLException, MetaDataAccessException {
								ResultSet rs = null;
								try {
									List<ColumnInfo> columnInfoList = new ArrayList<>();
									rs = dbMetadata.getColumns(catalogName, schemaName, tableName,
											metaDataColumnNamePattern);
									while (rs.next()) {
										columnInfoList.add(
												new ColumnInfo(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE")));
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
			}
			return columnInfos;
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	/**
	 * Gets the resultSet lower case column names ie the column names in the
	 * 'select' statement of the sql
	 *
	 * @param rs The jdbc ResultSet
	 * @return List of select column names in lower case
	 */
	public List<String> getResultSetColumnNames(ResultSet rs) {
		List<String> rsColNames = new ArrayList<>();
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			int numberOfColumns = rsmd.getColumnCount();
			// jdbc indexes start at 1
			for (int i = 1; i <= numberOfColumns; i++) {
				rsColNames.add(rsmd.getColumnLabel(i).toLowerCase());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return rsColNames;
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
	 * Get property information of an object. The property infos are cached by the
	 * object class name
	 *
	 * @param obj The object
	 * @return List of PropertyInfo
	 */
	public List<PropertyInfo> getObjectPropertyInfo(Object obj) {
		Assert.notNull(obj, "Object must not be null");
		List<PropertyInfo> propertyInfoList = objectPropertyInfoCache.get(obj.getClass());
		if (propertyInfoList == null) {
			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
			propertyInfoList = new ArrayList<>();
			PropertyDescriptor[] propertyDescriptors = bw.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors) {
				String propName = pd.getName();
				// log.debug("Property name:{}" + propName);
				if ("class".equals(propName)) {
					continue;
				} else {
					propertyInfoList.add(new PropertyInfo(propName, pd.getPropertyType()));
				}
			}
			objectPropertyInfoCache.put(obj.getClass(), propertyInfoList);
		}
		return propertyInfoList;
	}

	/**
	 * Converts camel case to snake case. Ex: userLastName gets converted to
	 * user_last_name.
	 *
	 * @param str camel case String
	 * @return the snake case string to lower case.
	 */
	public String convertCamelToSnakeCase(String str) {
		String snakeCase = camelToSnakeCache.get(str);
		if (snakeCase == null) {
			if (str != null) {
				snakeCase = TO_SNAKE_CASE_PATTERN.matcher(str).replaceAll("$1_$2").toLowerCase();
				camelToSnakeCache.put(str, snakeCase);
			}
		}
		return snakeCase;
	}

	/**
	 * Converts snake case to camel case. Ex: user_last_name gets converted to
	 * userLastName.
	 *
	 * @param str snake case string
	 * @return the camel case string
	 */
	public String convertSnakeToCamelCase(String str) {
		return JdbcUtils.convertUnderscoreNameToPropertyName(str);
	}

}
