package io.github.jdbctemplatemapper.core;

class PropertyInfo {
  private String propertyName;
  private Class<?> propertyType;

  public PropertyInfo(String propertyName, Class<?> propertyType) {
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
