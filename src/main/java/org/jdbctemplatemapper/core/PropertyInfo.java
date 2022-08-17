package org.jdbctemplatemapper.core;

public class PropertyInfo {
  private String propertyName;
  private Class<?> propertyType;

  public PropertyInfo(String propertyName, Class<?> propertyType) {
    super();
    this.propertyName = propertyName;
    this.propertyType = propertyType;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public Class<?> getPropertyType() {
    return propertyType;
  }

}
