package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

/**
 *
 * @author ajoseph
 */
public class SelectMapper<T> {
	private final MappingHelper mappingHelper;

	private final Class<T> clazz;

	private final DefaultConversionService defaultConversionService;

	private final String alias;

	public SelectMapper(Class<T> clazz, String alias, MappingHelper mappingHelper,
			DefaultConversionService defaultConversionService) {
		Assert.hasLength(alias, " alias cannot be empty");
		this.clazz = clazz;
		this.alias = alias;
		this.mappingHelper = mappingHelper;
		this.defaultConversionService = defaultConversionService;

	}

	public String getColumnsSql() {

		String str = "";
		TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
		StringBuilder sb = new StringBuilder(" ");
		String colPrefix = alias + ".";
		String aliasPrefix = " " + alias + "_";
		for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
			sb.append(colPrefix).append(propMapping.getColumnName()).append(aliasPrefix)
					.append(propMapping.getColumnName()).append(",");
		}
		str = sb.toString();
		// remove the last comma.
		if (mappingHelper.isNotEmpty(str)) {
			str = " " + str.substring(0, str.length() - 1) + " ";
		}
		return str;
	}

	public T getModel(ResultSet rs) {
		try {
			Object obj = clazz.newInstance();

			TableMapping tableMapping = mappingHelper.getTableMapping(clazz);

			BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
			// need below for java.sql.Timestamp to java.time.LocalDateTime conversion etc
			bw.setConversionService(defaultConversionService);

			String colPrefix = alias + "_";
			ResultSetMetaData rsMetaData = rs.getMetaData();
			int count = rsMetaData.getColumnCount();
			for (int i = 1; i <= count; i++) {
				String columnName = rsMetaData.getColumnName(i);
				if (columnName.startsWith(colPrefix)) {
					String propertyName = tableMapping.getProperyName(columnName.substring(colPrefix.length()));
					bw.setPropertyValue(propertyName,
							JdbcUtils.getResultSetValue(rs, i, tableMapping.getPropertyType(propertyName)));
				}
			}
			return clazz.cast(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
