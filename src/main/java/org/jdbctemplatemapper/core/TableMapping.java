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
  private String idColumnName;
  
  // object property to database column mapping.
  // Only properties which have corresponding database column will be in list.
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
  
  public int getPropertySqlType(String propertyName) {
	    int val = 0;
	    if (propertyName != null) {
	      for (PropertyMapping mapping : propertyMappings) {
	        if (mapping.getPropertyName().equals(propertyName)) {
	          val = mapping.getColumnSqlDataType();
	          break;
	        }
	      }
	    }
	    return val;
	  }
  
  
}
