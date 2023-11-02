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

  static void validate(JdbcTemplateMapper jtm, Class<?> ownerType, String relationshipType,
      Class<?> relatedType, String joinColumnTypeSide, String joinColumnManySide,
      String propertyName, String throughJoinTable, String throughOwnerTypeJoinColumn,
      String throughRelatedTypeJoinColumn) {

    if (jtm == null) {
      throw new QueryException("JdbcTemplateMapper cannot be null");
    }

    Object ownerModel = null;
    try {
      ownerModel = ownerType.getConstructor().newInstance();
    } catch (Exception e) {
      throw new MapperException(
          "Failed to instantiate " + ownerType.getName() + " No default constructor found.", e);
    }

    if (relatedType != null && relationshipType != null) {
      BeanWrapper bwOwnerModel = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
      if (!bwOwnerModel.isReadableProperty(propertyName)) {
        throw new QueryException(
            "Invalid property name " + propertyName + " for class " + ownerType.getSimpleName());
      }

      if (RelationshipType.BELONGS_TO.equals(relationshipType)) {
        validateHasOne(jtm, ownerType, relatedType, joinColumnTypeSide, propertyName,
            bwOwnerModel);
      } else if (RelationshipType.HAS_MANY.equals(relationshipType)) {
        validateHasMany(jtm, ownerType, relatedType, joinColumnManySide, propertyName,
            bwOwnerModel);
      } else if (RelationshipType.HAS_MANY_THROUGH.equals(relationshipType)) {
        validateHasManyThrough(ownerType, relatedType, throughJoinTable, throughOwnerTypeJoinColumn,
            throughRelatedTypeJoinColumn, propertyName, bwOwnerModel);
      }
    }
  }

  static void validateQueryCount(JdbcTemplateMapper jtm, Class<?> ownerType,
      String relationshipType, Class<?> relatedType, String joinColumnTypeSide) {

    if (jtm == null) {
      throw new QueryException("JdbcTemplateMapper cannot be null");
    }

    Object ownerModel = null;
    try {
      ownerModel = ownerType.getConstructor().newInstance();
    } catch (Exception e) {
      throw new MapperException(
          "Failed to instantiate " + ownerType.getName() + " No default constructor found.", e);
    }

    if (relatedType != null && relationshipType != null) {
      if (RelationshipType.BELONGS_TO.equals(relationshipType)) {
        BeanWrapper bwOwnerModel = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
        validateHasOne(jtm, ownerType, relatedType, joinColumnTypeSide, null, bwOwnerModel);
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

  private static void validateHasOne(JdbcTemplateMapper jtm, Class<?> ownerType,
      Class<?> relatedType, String joinColumnTypeSide, String propertyName,
      BeanWrapper bwOwnerModel) {

    if (propertyName != null) {
      PropertyDescriptor pd = bwOwnerModel.getPropertyDescriptor(propertyName);
      if (relatedType != null && !relatedType.getName().equals(pd.getPropertyType().getName())) {
        throw new QueryException("property type conflict. property " + ownerType.getSimpleName()
            + "." + propertyName + " is of type " + pd.getPropertyType().getSimpleName()
            + " while type for toOne relationship is " + relatedType.getSimpleName());
      }
    }

    if (joinColumnTypeSide.contains(".")) {
      throw new QueryException("Invalid joinColumnTypeSide. It should have no table prefix");
    }

    TableMapping ownerTypeTableMapping = jtm.getTableMapping(ownerType);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);
    String joinColumnPropertyName =
        ownerTypeTableMapping.getPropertyName(MapperUtils.toLowerCase(joinColumnTypeSide));

    if (joinColumnPropertyName == null) {
      throw new QueryException("Invalid join column " + joinColumnTypeSide + " . Table "
          + ownerTypeTableMapping.getTableName() + " for class " + ownerType.getSimpleName()
          + " either does not have column " + joinColumnTypeSide
          + " or the column has not been mapped in the class.");
    }

    Class<?> joinColumnPropertyType = ownerTypeTableMapping.getPropertyType(joinColumnPropertyName);
    Class<?> relatedTypeIdPropertyType =
        relatedTypeTableMapping.getIdPropertyMapping().getPropertyType();

    if (!joinColumnPropertyType.getName().equals(relatedTypeIdPropertyType.getName())) {
      throw new QueryException("Property type mismatch. join column " + joinColumnTypeSide
          + " property " + ownerType.getSimpleName() + "." + joinColumnPropertyName + " is of type "
          + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
          + relatedType.getSimpleName() + "." + relatedTypeTableMapping.getIdPropertyName()
          + " is of type " + relatedTypeIdPropertyType.getSimpleName());
    }
  }

  private static void validateHasMany(JdbcTemplateMapper jtm, Class<?> ownerType,
      Class<?> relatedType, String joinColumnManySide, String propertyName,
      BeanWrapper bwOwnerModel) {

    validatePopulatePropertyForCollection(propertyName, bwOwnerModel, ownerType, relatedType);

    if (joinColumnManySide.contains(".")) {
      throw new QueryException("Invalid joinColumnManySide. It should have no table prefix");
    }

    TableMapping ownerTypeTableMapping = jtm.getTableMapping(ownerType);
    TableMapping relatedTypeTableMapping = jtm.getTableMapping(relatedType);
    String joinColumnPropertyName =
        relatedTypeTableMapping.getPropertyName(MapperUtils.toLowerCase(joinColumnManySide));
    if (joinColumnPropertyName == null) {
      throw new QueryException("Invalid join column " + joinColumnManySide + " . Table "
          + relatedTypeTableMapping.getTableName() + " for class " + relatedType.getSimpleName()
          + " either does not have column " + joinColumnManySide
          + " or the column has not been mapped in the class.");
    }

    Class<?> joinColumnPropertyType =
        relatedTypeTableMapping.getPropertyType(joinColumnPropertyName);
    Class<?> ownerTypeIdPropertyType =
        ownerTypeTableMapping.getIdPropertyMapping().getPropertyType();

    if (!joinColumnPropertyType.getName().equals(ownerTypeIdPropertyType.getName())) {
      throw new QueryException(
          "Property type mismatch. join column " + joinColumnManySide + " property "
              + relatedType.getSimpleName() + "." + joinColumnPropertyName + " is of type "
              + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
              + ownerType.getSimpleName() + "." + ownerTypeTableMapping.getIdPropertyName()
              + " is of type " + ownerTypeIdPropertyType.getSimpleName());
    }
  }

  private static void validateHasManyThrough(Class<?> ownerType, Class<?> relatedType,
      String throughJoinTable, String throughOwnerTypeJoinColumn,
      String throughRelatedTypeJoinColumn, String propertyName, BeanWrapper bwOwnerModel) {
    validatePopulatePropertyForCollection(propertyName, bwOwnerModel, ownerType, relatedType);

    if (throughOwnerTypeJoinColumn.contains(".")) {
      throw new QueryException("Invalid throughJoinColumns " + throughOwnerTypeJoinColumn
          + " .It should have no prefixes");
    }
    if (throughRelatedTypeJoinColumn.contains(".")) {
      throw new QueryException("Invalid throughJoinColumns " + throughRelatedTypeJoinColumn
          + " .It should have no prefixes");
    }
  }


  private static void validatePopulatePropertyForCollection(String propertyName,
      BeanWrapper bwOwnerModel, Class<?> ownerType, Class<?> relatedType) {
    PropertyDescriptor pd = bwOwnerModel.getPropertyDescriptor(propertyName);
    if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
      throw new QueryException("property " + ownerType.getSimpleName() + "." + propertyName
          + " is not a collection. hasMany() relationship requires it to be a collection");
    }

    Object ownerModel = bwOwnerModel.getWrappedInstance();
    Class<?> collectionGenericType = getGenericTypeOfCollection(ownerModel, propertyName);
    if (collectionGenericType == null) {
      throw new QueryException(
          "Collections without generic types are not supported. Collection for property "
              + ownerType.getSimpleName() + "." + propertyName + " does not have a generic type.");
    }

    if (!collectionGenericType.getName().equals(relatedType.getName())) {
      throw new QueryException("Collection generic type and hasMany relationship type mismatch. "
          + ownerType.getSimpleName() + "." + propertyName + " has generic type "
          + collectionGenericType.getSimpleName() + " while the hasMany relationship is of type "
          + relatedType.getSimpleName());
    }
    Object value = bwOwnerModel.getPropertyValue(propertyName);
    if (value == null) {
      throw new QueryException(
          "Only initialized collections can be populated by queries. Collection property "
              + ownerType.getSimpleName() + "." + propertyName + " is not initialized.");
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
