/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jdbctemplatemapper.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Locale;
import java.util.StringJoiner;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import io.github.jdbctemplatemapper.exception.MapperException;

/**
 * Allows population of the model from a ResultSet and generating the select columns string for the
 * model.
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

  // This is same converter used by Spring BeanPropertyRowMapper
  private final ConversionService conversionService;
  private final boolean useColumnLabelForResultSetMetaData;
  private String tableAlias;
  private String colPrefix;
  private String colAliasPrefix;

  private boolean internal;

  SelectMapper(Class<T> clazz, String tableAlias, MappingHelper mappingHelper,
      ConversionService conversionService, boolean useColumnLabelForResultSetMetaData) {
    Assert.notNull(clazz, " clazz cannot be empty");
    if (MapperUtils.isBlank(tableAlias)) {
      throw new MapperException("tableAlias cannot be blank or empty");
    }

    this.clazz = clazz;
    this.mappingHelper = mappingHelper;
    this.conversionService = conversionService;

    this.useColumnLabelForResultSetMetaData = useColumnLabelForResultSetMetaData;
    this.tableAlias = tableAlias.trim();
    this.colPrefix = this.tableAlias + ".";
    this.colAliasPrefix = MapperUtils.toLowerCase(this.tableAlias + "_");
  }

  // internal use only
  SelectMapper(Class<T> clazz, String tableAlias, String columnAliasPrefix,
      MappingHelper mappingHelper, ConversionService conversionService,
      boolean useColumnLabelForResultSetMetaData) {
    Assert.notNull(clazz, " clazz cannot be null");
    Assert.notNull(tableAlias, " tableAlias cannot be null");
    Assert.notNull(columnAliasPrefix, " columnAliasPrefix cannot be null");
    this.clazz = clazz;
    this.mappingHelper = mappingHelper;
    this.conversionService = conversionService;
    this.useColumnLabelForResultSetMetaData = useColumnLabelForResultSetMetaData;

    this.colPrefix = tableAlias + ".";
    this.colAliasPrefix = columnAliasPrefix;
    this.internal = true;
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
      if (internal) {
        sj.add(colPrefix + propMapping.getColumnName() + " as " + colAliasPrefix
            + propMapping.getColumnAliasSuffix());
      } else {
        sj.add(colPrefix + propMapping.getColumnName() + " as " + colAliasPrefix
            + MapperUtils.TYPE_TABLE_COL_ALIAS_PREFIX + propMapping.getColumnAliasSuffix());
      }
    }
    return sj.toString();
  }

  /**
   * Get the type the SelectMapper is for.
   *
   * @return the type
   */
  public Class<?> getType() {
    return clazz;
  }

  /**
   * Get the table alias of the SelectMapper.
   *
   * @return the table alias
   */
  public String getTableAlias() {
    return tableAlias;
  }

  /**
   * gets column alias of the models id in sql statement.
   *
   * @return the column alias of the models id in sql statement
   */
  public String getResultSetModelIdColumnLabel() {
    if (internal) {
      // This is an internal call from Query, QueryMerge
      // returned values something like oc1 ... or rc1 ...
      return colAliasPrefix
          + mappingHelper.getTableMapping(clazz).getIdPropertyMapping().getColumnAliasSuffix();
    } else {
      // This is when user is using the the jtm.getSelectMapper(type, tableAlias) to write custom
      // queries. returned values something like tableAlias_oc1 ...
      return colAliasPrefix + MapperUtils.TYPE_TABLE_COL_ALIAS_PREFIX
          + mappingHelper.getTableMapping(clazz).getIdPropertyMapping().getColumnAliasSuffix();
    }
  }

  /**
   * Get the models id type.
   *
   * @return the type
   */
  public Class<?> getModelIdType() {
    return mappingHelper.getTableMapping(clazz).getIdPropertyMapping().getPropertyType();
  }

  /**
   * Builds the model from the resultSet.
   *
   * @param rs The ResultSet from which to build the model.
   * @return the model. Will return null if the id property is null (even if other fields have some
   *         values)
   */
  public T buildModel(ResultSet rs) {
    BeanWrapper bw = buildBeanWrapperModel(rs);
    return bw == null ? null : clazz.cast(bw.getWrappedInstance());
  }

  // returns model object wrapped in BeanWrapper. Used also by Query and QueryMerge processing to
  // prevent excessive creation of bean wrappers.
  BeanWrapper buildBeanWrapperModel(ResultSet rs) {
    Object obj = null;
    try {
      obj = clazz.getConstructor().newInstance();
    } catch (Exception e) {
      throw new MapperException(
          "Failed to instantiate " + clazz.getName() + "  No default constructor found.", e);
    }
    try {
      TableMapping tableMapping = mappingHelper.getTableMapping(clazz);
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
      // need this when jdbcUtils cannot convert
      bw.setConversionService(conversionService);

      ResultSetMetaData rsMetaData = rs.getMetaData();
      int count = rsMetaData.getColumnCount();
      for (int i = 1; i <= count; i++) {
        String columnLabel = rsMetaData.getColumnLabel(i);
        // attempted support for older drivers
        if (!useColumnLabelForResultSetMetaData) {
          if (StringUtils.hasLength(rsMetaData.getColumnName(i))) {
            columnLabel = rsMetaData.getColumnName(i);
          }
        }
        if (columnLabel != null) {
          columnLabel = columnLabel.toLowerCase(Locale.US);
          if (columnLabel.startsWith(colAliasPrefix)) {
            PropertyMapping propMapping = null;
            if (internal) {
              // This is an internal call from Query, QueryMerge
              // column alias would be something like oc1 ... or rc1 ...
              propMapping = tableMapping.getPropertyMappingByColumnAlias(columnLabel);
            } else {
              // This is when user is using the the jtm.getSelectMapper(type, tableAlias) to write
              // custom queries. Column alias would be something like colAliasPrefix_oc1,
              // colAliasPrefix_oc2 ...
              propMapping = tableMapping.getPropertyMappingByColumnAlias(
                  columnLabel.substring(colAliasPrefix.length()));
            }
            if (propMapping != null) {
              // JdbcUtils.getResultSetValue() assigns value using the specifically typed ResultSet
              // accessor methods (getString(), getInt() etc) for the specified propertyType.
              bw.setPropertyValue(propMapping.getPropertyName(),
                  JdbcUtils.getResultSetValue(rs, i, propMapping.getPropertyType()));
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
      throw new MapperException(e);
    }
  }
}
