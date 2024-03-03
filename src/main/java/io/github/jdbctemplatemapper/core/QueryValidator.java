/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jdbctemplatemapper.core;

import io.github.jdbctemplatemapper.exception.MapperException;
import io.github.jdbctemplatemapper.exception.QueryException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * Validates Query, QueryMerge and QueryCount.
 *
 * @author ajoseph
 */
class QueryValidator {

  private QueryValidator() {}

  static void validate(JdbcTemplateMapper jtm, Class<?> type, String relationshipType,
      Class<?> relatedType, String joinColumnTypeSide, String joinColumnManySide,
      String propertyName, String throughJoinTable, String throughTypeJoinColumn,
      String throughRelatedTypeJoinColumn) {

    if (jtm == null) {
      throw new QueryException("JdbcTemplateMapper cannot be null");
    }

    Object typeModel = null;
    try {
      typeModel = type.getConstructor().newInstance();
    } catch (Exception e) {
      throw new MapperException(
          "Failed to instantiate " + type.getName() + " No default constructor found.", e);
    }

    if (relatedType != null && relationshipType != null) {
      BeanWrapper bwTypeModel = PropertyAccessorFactory.forBeanPropertyAccess(typeModel);
      if (!bwTypeModel.isReadableProperty(propertyName)) {
        throw new QueryException(
            "Invalid property name " + propertyName + " for class " + type.getSimpleName());
      }

      if (RelationshipType.HAS_ONE.equals(relationshipType)) {
        validateHasOne(jtm, type, relatedType, joinColumnTypeSide, propertyName, bwTypeModel);
      } else if (RelationshipType.HAS_MANY.equals(relationshipType)) {
        validateHasMany(jtm, type, relatedType, joinColumnManySide, propertyName, bwTypeModel);
      } else if (RelationshipType.HAS_MANY_THROUGH.equals(relationshipType)) {
        validateHasManyThrough(type, relatedType, throughJoinTable, throughTypeJoinColumn,
            throughRelatedTypeJoinColumn, propertyName, bwTypeModel);
      }
    }
  }

  static void validateQueryCount(JdbcTemplateMapper jtm, Class<?> type, String relationshipType,
      Class<?> relatedType, String joinColumnTypeSide) {

    if (jtm == null) {
      throw new QueryException("JdbcTemplateMapper cannot be null");
    }

    Object typeModel = null;
    try {
      typeModel = type.getConstructor().newInstance();
    } catch (Exception e) {
      throw new MapperException(
          "Failed to instantiate " + type.getName() + " No default constructor found.", e);
    }

    if (relatedType != null && relationshipType != null) {
      if (RelationshipType.HAS_ONE.equals(relationshipType)) {
        BeanWrapper bwTypeModel = PropertyAccessorFactory.forBeanPropertyAccess(typeModel);
        validateHasOne(jtm, type, relatedType, joinColumnTypeSide, null, bwTypeModel);
      }
    }
  }

  static void validateQueryLimitOffsetClause(String relationshipType, String limitOffsetClause) {
    if (RelationshipType.HAS_MANY.equals(relationshipType)
        || RelationshipType.HAS_MANY_THROUGH.equals(relationshipType)) {
      if (MapperUtils.isNotBlank(limitOffsetClause)) {
        throw new IllegalArgumentException(
            "limitOffsetClause is not supported for hasMany and hasMany through relationships. "
                + "See documentation to achieve similar functionality.");
      }
    }
  }

  private static void validateHasOne(JdbcTemplateMapper jtm, Class<?> type, Class<?> relatedType,
      String joinColumnTypeSide, String propertyName, BeanWrapper bwTypeModel) {

    if (propertyName != null) {
      PropertyDescriptor pd = bwTypeModel.getPropertyDescriptor(propertyName);
      if (relatedType != null && !relatedType.getName().equals(pd.getPropertyType().getName())) {
        throw new QueryException("property type conflict. property " + type.getSimpleName() + "."
            + propertyName + " is of type " + pd.getPropertyType().getSimpleName()
            + " while type for hasOne relationship is " + relatedType.getSimpleName());
      }
    }

    if (joinColumnTypeSide.contains(".")) {
      throw new QueryException("Invalid joinColumnTypeSide. It should have no table prefix");
    }

    TableMapping typeTableMapping = jtm.getTableMapping(type);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);
    String joinColumnPropertyName =
        typeTableMapping.getPropertyName(MapperUtils.toLowerCase(joinColumnTypeSide));

    if(typeTableMapping.isQuotedIdentifier()) {
      joinColumnPropertyName = typeTableMapping.getPropertyName(joinColumnTypeSide);
    }
    
    if (joinColumnPropertyName == null) {
      throw new QueryException("Invalid join column " + joinColumnTypeSide + " . Table "
          + typeTableMapping.getTableName() + " for class " + type.getSimpleName()
          + " either does not have column " + joinColumnTypeSide
          + " or the column has not been mapped in the class.");
    }

    Class<?> joinColumnPropertyType = typeTableMapping.getPropertyType(joinColumnPropertyName);
    Class<?> relatedTypeIdPropertyType =
        relatedTypeTableMapping.getIdPropertyMapping().getPropertyType();

    if (!joinColumnPropertyType.getName().equals(relatedTypeIdPropertyType.getName())) {
      throw new QueryException("Property type mismatch. join column " + joinColumnTypeSide
          + " property " + type.getSimpleName() + "." + joinColumnPropertyName + " is of type "
          + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
          + relatedType.getSimpleName() + "." + relatedTypeTableMapping.getIdPropertyName()
          + " is of type " + relatedTypeIdPropertyType.getSimpleName());
    }
  }

  private static void validateHasMany(JdbcTemplateMapper jtm, Class<?> type, Class<?> relatedType,
      String joinColumnManySide, String propertyName, BeanWrapper bwTypeModel) {

    validatePopulatePropertyForCollection(propertyName, bwTypeModel, type, relatedType);

    if (joinColumnManySide.contains(".")) {
      throw new QueryException("Invalid joinColumnManySide. It should have no table prefix");
    }

    TableMapping typeTableMapping = jtm.getTableMapping(type);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);
    String joinColumnPropertyName =
        relatedTypeTableMapping.getPropertyName(MapperUtils.toLowerCase(joinColumnManySide));
    
    if(typeTableMapping.isQuotedIdentifier()) {
      joinColumnPropertyName = relatedTypeTableMapping.getPropertyName(joinColumnManySide);
    }
    
    if (joinColumnPropertyName == null) {
      throw new QueryException("Invalid join column " + joinColumnManySide + " . Table "
          + relatedTypeTableMapping.getTableName() + " for class " + relatedType.getSimpleName()
          + " either does not have column " + joinColumnManySide
          + " or the column has not been mapped in the class.");
    }

    Class<?> joinColumnPropertyType =
        relatedTypeTableMapping.getPropertyType(joinColumnPropertyName);
    Class<?> typeIdPropertyType = typeTableMapping.getIdPropertyMapping().getPropertyType();

    if (!joinColumnPropertyType.getName().equals(typeIdPropertyType.getName())) {
      throw new QueryException(
          "Property type mismatch. join column " + joinColumnManySide + " property "
              + relatedType.getSimpleName() + "." + joinColumnPropertyName + " is of type "
              + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
              + type.getSimpleName() + "." + typeTableMapping.getIdPropertyName() + " is of type "
              + typeIdPropertyType.getSimpleName());
    }
  }

  private static void validateHasManyThrough(Class<?> type, Class<?> relatedType,
      String throughJoinTable, String throughTypeJoinColumn, String throughRelatedTypeJoinColumn,
      String propertyName, BeanWrapper bwTypeModel) {
    validatePopulatePropertyForCollection(propertyName, bwTypeModel, type, relatedType);

    if (throughTypeJoinColumn.contains(".")) {
      throw new QueryException(
          "Invalid throughJoinColumns " + throughTypeJoinColumn + " .It should have no prefixes");
    }
    if (throughRelatedTypeJoinColumn.contains(".")) {
      throw new QueryException("Invalid throughJoinColumns " + throughRelatedTypeJoinColumn
          + " .It should have no prefixes");
    }
  }


  private static void validatePopulatePropertyForCollection(String propertyName,
      BeanWrapper bwTypeModel, Class<?> type, Class<?> relatedType) {
    PropertyDescriptor pd = bwTypeModel.getPropertyDescriptor(propertyName);
    if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
      throw new QueryException("property " + type.getSimpleName() + "." + propertyName
          + " is not a collection. hasMany() relationship requires it to be a collection");
    }

    Object typeModel = bwTypeModel.getWrappedInstance();
    Class<?> collectionGenericType = getGenericTypeOfCollection(typeModel, propertyName);
    if (collectionGenericType == null) {
      throw new QueryException(
          "Collections without generic types are not supported. Collection for property "
              + type.getSimpleName() + "." + propertyName + " does not have a generic type.");
    }

    if (!collectionGenericType.getName().equals(relatedType.getName())) {
      throw new QueryException(
          "Collection generic type and hasMany relationship type mismatch. " + type.getSimpleName()
              + "." + propertyName + " has generic type " + collectionGenericType.getSimpleName()
              + " while the hasMany relationship is of type " + relatedType.getSimpleName());
    }
    Object value = bwTypeModel.getPropertyValue(propertyName);
    if (value == null) {
      throw new QueryException(
          "Only initialized collections can be populated by queries. Collection property "
              + type.getSimpleName() + "." + propertyName + " is not initialized.");
    }
  }

  private static Class<?> getGenericTypeOfCollection(Object mainObj, String propertyName) {
    try {
      Field field = mainObj.getClass().getDeclaredField(propertyName);
      ParameterizedType pt = (ParameterizedType) field.getGenericType();
      Type[] genericType = pt.getActualTypeArguments();
      if (genericType != null && genericType.length > 0) {
        return Class.forName(genericType[0].getTypeName());
      }
    } catch (Exception e) {
      // do nothing
    }
    return null;
  }

}
