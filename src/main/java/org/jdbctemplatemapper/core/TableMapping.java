package org.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * The database table mapping details on an object
 *
 * @author ajoseph
 */
public class TableMapping {
  private String tableName;
  private String idPropertyName;

  // object property to database column mapping.
  // Only properties which have corresponding database column will be in this list.
  private List<PropertyMapping> propertyMappings = new ArrayList<>();

  
  public TableMapping(String tableName, String idPropertyName, List<PropertyMapping> propertyMappings) {
	  Assert.notNull(tableName, "tableName must not be null");
	  Assert.notNull(idPropertyName, "idPropertyName must not be null");
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

  public String getTableName() {
    return tableName;
  }

  public String getIdPropertyName() {
	  return getIdPropertyMapping().getPropertyName();
  }
  
  public String getIdColumnName() {
	  return getIdPropertyMapping().getColumnName();
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
