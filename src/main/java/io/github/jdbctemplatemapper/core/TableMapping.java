package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * The database table mapping details on an object
 *
 * @author ajoseph
 */
public class TableMapping {
  private Class<?> tableClass;
  private String tableName;
  private String idPropertyName;
  private boolean idAutoIncrement = false;

  // object property to database column mapping.
  // Only properties which have corresponding database column will be in this list.
  private List<PropertyMapping> propertyMappings = new ArrayList<>();

  
  public TableMapping(Class<?> tableClass, String tableName, String idPropertyName, List<PropertyMapping> propertyMappings) {
	  Assert.notNull(tableClass, "tableClass must not be null");
	  Assert.notNull(tableName, "tableName must not be null");
	  Assert.notNull(idPropertyName, "idPropertyName must not be null");
	  this.tableClass = tableClass;
	  this.tableName = tableName;
	  this.idPropertyName = idPropertyName;
	  this.propertyMappings = propertyMappings;
  }
  public String getColumnName(String propertyName) {
    if (propertyName != null) {
      for (PropertyMapping mapping : propertyMappings) {
        if (propertyName.equals(mapping.getPropertyName())) {
          return mapping.getColumnName();
        }
      }
    }
    return null;
  }

  public int getPropertySqlType(String propertyName) {
    if (propertyName != null) {
      for (PropertyMapping mapping : propertyMappings) {
        if (propertyName.equals(mapping.getPropertyName())) {
          return mapping.getColumnSqlDataType();
        }
      }
    }
    return 0;
  }
  
  public Class<?> getTableClass(){
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
      for (PropertyMapping mapping : propertyMappings) {
          if (idPropertyName.equals(mapping.getPropertyName())) {
            return mapping;
          }
        }
      throw new RuntimeException("For @Id property " + idPropertyName + " could not find corresponding column in database table " + tableName);
  }

  public List<PropertyMapping> getPropertyMappings() {
    return propertyMappings;
  }

}
