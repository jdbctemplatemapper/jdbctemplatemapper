package io.github.jdbctemplatemapper.core;

class Relationship implements Cloneable {

  private Class<?> mainClazz;
  private SelectMapper<?> selectMapperMainClazz;
  private RelationshipType relationshipType;
  private Class<?> relatedClazz;
  private SelectMapper<?> selectMapperRelatedClazz;
  private String propertyName; // propertyName on main class that needs to be populated

  Relationship(Class<?> mainClazz) {
    this.mainClazz = mainClazz;
  }

  Relationship(
      Class<?> mainClazz,
      RelationshipType relationshipType,
      Class<?> relatedClazz,
      String relationshipPropertyName) {
    this.mainClazz = mainClazz;
    this.relationshipType = relationshipType;

    this.relatedClazz = relatedClazz;

    this.propertyName = relationshipPropertyName;
  }

  public Class<?> getMainClazz() {
    return mainClazz;
  }

  public void setMainClazz(Class<?> mainClazz) {
    this.mainClazz = mainClazz;
  }

  public RelationshipType getRelationshipType() {
    return relationshipType;
  }

  public void setRelationshipType(RelationshipType relationshipType) {
    this.relationshipType = relationshipType;
  }

  public Class<?> getRelatedClazz() {
    return relatedClazz;
  }

  public void setRelatedClazz(Class<?> relatedClazz) {
    this.relatedClazz = relatedClazz;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public SelectMapper<?> getSelectMapperMainClazz() {
    return selectMapperMainClazz;
  }

  public void setSelectMapperMainClazz(SelectMapper<?> selectMapperMainClazz) {
    this.selectMapperMainClazz = selectMapperMainClazz;
  }

  public SelectMapper<?> getSelectMapperRelatedClazz() {
    return selectMapperRelatedClazz;
  }

  public void setSelectMapperRelatedClazz(SelectMapper<?> selectMapperRelatedClazz) {
    this.selectMapperRelatedClazz = selectMapperRelatedClazz;
  }
}
