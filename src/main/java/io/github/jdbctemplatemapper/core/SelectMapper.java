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
 * Allows population of the model from a ResultSet and generating the select columns string for the
 * model
 *
 * <pre>
 *
 * See <a href=
 * "https://github.com/jdbctemplatemapper/jdbctemplatemapper#querying-relationships">Querying relationships</a> for more info
 * </pre>
 *
 * @author ajoseph
 */
public class SelectMapper<T> {
  private final MappingHelper mappingHelper;
  private final Class<T> clazz;
  private final ConversionService conversionService;
  private final boolean useColumnLabelForResultSetMetaData;
  private String tableAlias;
  private String colPrefix; // tableAlias + "."
  private String colAliasPrefix; // tableAlias + "_"

  SelectMapper(
      Class<T> clazz,
      String tableAlias,
      MappingHelper mappingHelper,
      ConversionService conversionService,
      boolean useColumnLabelForResultSetMetaData) {
    Assert.notNull(clazz, " clazz cannot be empty");
    Assert.hasLength(tableAlias, " tableAlias cannot be empty");
    if (tableAlias.trim().length() < 1) {
      throw new MapperException("tableAlias cannot be empty");
    }

    this.clazz = clazz;
    this.mappingHelper = mappingHelper;
    this.conversionService = conversionService;

    this.useColumnLabelForResultSetMetaData = useColumnLabelForResultSetMetaData;
    this.tableAlias = tableAlias.trim();
    this.colPrefix = this.tableAlias + ".";
    this.colAliasPrefix = MapperUtils.toLowerCase(this.tableAlias + "_");
  }

  /**
   * returns a string which can be used in a sql select statement with all the properties which have
   * corresponding database columns. The column aliases will have a prefix of tableAlias
   * concatenated with "_"
   *
   * <pre>
   * SelectMapper selectMapper = jdbcTemplateMapper.getSelectMapper(Employee.class, "emp");
   * selectMapper.getColumnSql() will return something like below:
   *
   * "emp.id as emp_id, emp.last_name as emp_last_name, emp.first_name as emp_first_name"
   * </pre>
   *
   * @return comma separated select column string
   */
  public String getColumnsSql() {
    StringJoiner sj = new StringJoiner(", ", " ", " ");
    TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
    for (PropertyMapping propMapping : tableMapping.getPropertyMappings()) {
      sj.add(
          colPrefix
              + propMapping.getColumnName()
              + " as "
              + colAliasPrefix
              + propMapping.getColumnName());
    }
    return sj.toString();
  }

  /**
   * Get the type the SelectMapper is for
   *
   * @return the type
   */
  public Class<?> getType() {
    return clazz;
  }

  /**
   * Get the table alias of the SelectMapper
   *
   * @return the table alias
   */
  public String getTableAlias() {
    return tableAlias;
  }

  /**
   * gets column alias of the models id in sql statement
   *
   * @return the column alias of the models id in sql statement
   */
  public String getResultSetModelIdColumnLabel() {
    return colAliasPrefix + mappingHelper.getTableMapping(clazz).getIdColumnName();
  }
  
  /**
   * Get the models id type
   *
   * @return the type
   */
  public Class<?> getModelIdType() {
    return mappingHelper.getTableMapping(clazz).getIdPropertyMapping().getPropertyType();
  }

  /**
   * Builds the model from the resultSet
   *
   * @param rs The ResultSet from which to build the model.
   * @return the model. Will return null if the id property is null (even if other fields have some
   *     values)
   */
  public T buildModel(ResultSet rs) {
    BeanWrapper bw = buildBeanWrapperModel(rs);
    return bw == null ? null : clazz.cast(bw.getWrappedInstance());
  }

  // returns model object wrapped in BeanWrapper. Used also by Query and QueryMerge processing to
  // prevent
  // excessive creation of bean wrappers.
  BeanWrapper buildBeanWrapperModel(ResultSet rs) {
    try {
      Object obj = clazz.getConstructor().newInstance();
      TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
      // need below for java.sql.Timestamp to java.time.LocalDateTime conversion etc
      bw.setConversionService(conversionService);

      ResultSetMetaData rsMetaData = rs.getMetaData();
      int count = rsMetaData.getColumnCount();
      for (int i = 1; i <= count; i++) {
        // support for older drivers
        String columnLabel =
            useColumnLabelForResultSetMetaData
                ? rsMetaData.getColumnLabel(i)
                : rsMetaData.getColumnName(i);
        if (columnLabel != null) {
          columnLabel = MapperUtils.toLowerCase(columnLabel);
          if (columnLabel.startsWith(colAliasPrefix)) {
            String propertyName =
                tableMapping.getPropertyName(columnLabel.substring(colAliasPrefix.length()));
            if (propertyName != null) {
              bw.setPropertyValue(
                  propertyName,
                  JdbcUtils.getResultSetValue(rs, i, tableMapping.getPropertyType(propertyName)));
            }
          }
        }
      }
      // if id is null return null. Does not matter if other fields have values.
      if (bw.getPropertyValue(tableMapping.getIdPropertyName()) == null) {
        return null;
      } else {
        return bw;
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
