package org.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The database table mapping details on an object
 *
 * @author ajoseph
 */
public class TableMapping {
  private String tableName;
  private String idColumnName;

  // object property to database column mapping.
  // Only properties which have corresponding database column will be in this list.
  private List<PropertyMapping> propertyMappings = new ArrayList<>();

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

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getIdColumnName() {
    return idColumnName;
  }

  public void setIdColumnName(String idColumnName) {
    this.idColumnName = idColumnName;
  }

  public List<PropertyMapping> getPropertyMappings() {
    return propertyMappings;
  }

  public void setPropertyMappings(List<PropertyMapping> propertyMappings) {
    this.propertyMappings = propertyMappings;
  }
}
