package org.jdbctemplatemapper.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbctemplatemapper.annotation.Id;
import org.jdbctemplatemapper.annotation.IdType;
import org.jdbctemplatemapper.annotation.Table;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

public class MappingUtils {

  // Map key - object class name
  //     value - the table name
  private Map<String, TableMapping> objectToTableMappingCache = new ConcurrentHashMap<>();

  private DatabaseUtils dbUtils;

  public MappingUtils(DatabaseUtils dbUtils) {
    this.dbUtils = dbUtils;
  }

  /**
   * Gets the table mapping for the Object. The table mapping has the table name and and object
   * property to database column mapping.
   *
   * <p>Table name is either from the @Tabel annotation or the snake case conversion of the Object
   * name.
   *
   * @param clazz The object class
   * @return The table mapping.
   */
  public TableMapping getTableMapping(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    TableMapping tableMapping = objectToTableMappingCache.get(clazz.getName());

    if (tableMapping == null) {
      String tableName = null;
      Table tableAnnotation = AnnotationUtils.findAnnotation(clazz, Table.class);
      if (tableAnnotation != null) {
        tableName = tableAnnotation.name();
      } else {
        tableName = CommonUtils.convertCamelToSnakeCase(clazz.getSimpleName());
      }

      Id idAnnotation = null;
      String idPropertyName = null;
      boolean isAutoIncrement = false;
      for (Field field : clazz.getDeclaredFields()) {
        idAnnotation = AnnotationUtils.findAnnotation(field, Id.class);
        if (idAnnotation != null) {
          idPropertyName = field.getName();
          if (idAnnotation.type() == IdType.AUTO_INCREMENT) {
        	  isAutoIncrement = true;
          }
          break;
        }
      }
      if (idAnnotation == null) {
        throw new RuntimeException("@Id annotation not found for class " + clazz.getName());
      } else {
        System.out.println("Annotation @Id FOUND for class" + clazz.getName() + "" + idAnnotation);
        System.out.println("id field name= " + idPropertyName);
      }

      List<ColumnInfo> columnInfoList = dbUtils.getTableColumnInfo(tableName);
      if (CommonUtils.isEmpty(columnInfoList)) {
        // try again with upper case table name
        tableName = tableName.toUpperCase();
        columnInfoList = dbUtils.getTableColumnInfo(tableName);
        if (CommonUtils.isEmpty(columnInfoList)) {
          throw new RuntimeException(
              "Could not find corresponding table for class " + clazz.getSimpleName());
        }
      }

      // if code reaches here table exists and class has @Id annotation
      try {
        List<PropertyInfo> propertyInfoList =
            CommonUtils.getObjectPropertyInfo(clazz.newInstance());
        List<PropertyMapping> propertyMappings = new ArrayList<>();
        // Match  database table columns to the Object properties
        for (ColumnInfo columnInfo : columnInfoList) {
          // property name corresponding to column name
          String propertyName = CommonUtils.convertSnakeToCamelCase(columnInfo.getColumnName());
          // check if the property exists for the Object
          PropertyInfo propertyInfo =
              propertyInfoList
                  .stream()
                  .filter(pi -> propertyName.equals(pi.getPropertyName()))
                  .findAny()
                  .orElse(null);

          // add matched object property info and table column info to mappings
          if (propertyInfo != null) {
        	  PropertyMapping propertyMapping = new PropertyMapping(
                      propertyInfo.getPropertyName(),
                      propertyInfo.getPropertyType(),
                      columnInfo.getColumnName(),
                      columnInfo.getColumnSqlDataType());
            propertyMappings.add(propertyMapping);
                        
          }
        }

        tableMapping = new TableMapping(tableName, idPropertyName, propertyMappings);
        tableMapping.setIdAutoIncrement(isAutoIncrement);

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    objectToTableMappingCache.put(clazz.getName(), tableMapping);
    return tableMapping;
  }
}
