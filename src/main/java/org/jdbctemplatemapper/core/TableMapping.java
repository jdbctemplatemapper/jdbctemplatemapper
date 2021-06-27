package org.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
}
