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
 *
 */
public class TableMapping {
  private String tableName;
  private String idName;
  
  // object property to database column mapping.
  private List<PropertyMapping> propertyMappings = new ArrayList<>();

  public String getColumnName(String propertyName) {
    String val = null;
    if (propertyName != null) {
      for (PropertyMapping mapping : propertyMappings) {
        if (mapping.getPropertyName().equals(propertyName)) {
          val = mapping.getColumnName();
          break;
        }
      }
    }
    return val;
  }
}
