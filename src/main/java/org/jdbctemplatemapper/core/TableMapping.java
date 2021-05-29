package org.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TableMapping {
  private String tableName;
  private String idName;
  private List<PropertyColumnMapping> propertyColumnMappings = new ArrayList<>();

  public String getColumnName(String propertyName) {
    String val = null;
    if (propertyName != null) {
      for (PropertyColumnMapping mapping : propertyColumnMappings) {
        if (mapping.getPropertyName().equals(propertyName)) {
          val = mapping.getColumnName();
          break;
        }
      }
    }
    return val;
  }
}
