package io.github.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.util.StringUtils;

import io.github.jdbctemplatemapper.exception.QueryException;

class QueryValidator {

  private QueryValidator() {}

  static void validate(
      JdbcTemplateMapper jtm,
      Class<?> ownerType,
      RelationshipType relationshipType,
      Class<?> relatedType,
      String joinColumnOwningSide,
      String joinColumnManySide,
      String propertyName,
      String throughJoinTable,
      String throughOwnerTypeJoinColumn,
      String throughRelatedTypeJoinColumn) {

    validateCommon(jtm, ownerType, relatedType, propertyName);

    Object ownerModel = null;
    try {
      ownerModel = ownerType.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (relatedType != null) {
      if (relationshipType == RelationshipType.HAS_ONE) {
        validateHasOne(jtm, ownerType, relatedType, joinColumnOwningSide, propertyName, ownerModel);
      } else if (relationshipType == RelationshipType.HAS_MANY) {
        validateHasMany(jtm, ownerType, relatedType, joinColumnManySide, propertyName, ownerModel);
      } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
        validateHasManyThrough(
            ownerType,
            relatedType,
            throughJoinTable,
            throughOwnerTypeJoinColumn,
            throughRelatedTypeJoinColumn,
            propertyName,
            ownerModel);
      }
    }
  }

  private static void validateCommon(
      JdbcTemplateMapper jtm, Class<?> ownerType, Class<?> relatedType, String propertyName) {
    if (jtm == null) {
      throw new QueryException("JdbcTemplateMapper cannot be null");
    }

    if (relatedType != null) {
      Object ownerModel = null;
      try {
        ownerModel = ownerType.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
      if (!bw.isReadableProperty(propertyName)) {
        throw new QueryException(
            "Invalid property name " + propertyName + " for class " + ownerType.getSimpleName());
      }
    }
  }

  private static void validateHasOne(
      JdbcTemplateMapper jtm,
      Class<?> ownerType,
      Class<?> relatedType,
      String joinColumnOwningSide,
      String propertyName,
      Object ownerModel) {

    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
    PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
    if (relatedType != pd.getPropertyType()) {
      throw new QueryException(
          "property type conflict. property "
              + ownerType.getSimpleName()
              + "."
              + propertyName
              + " is of type "
              + pd.getPropertyType().getSimpleName()
              + " while type for hasOne relationship is "
              + relatedType.getSimpleName());
    }

    if (MapperUtils.isBlank(joinColumnOwningSide)) {
      throw new QueryException("joinColumnOwningSide cannot be blank");
    }
    if (joinColumnOwningSide.contains(".")) {
      throw new QueryException("Invalid joinColumnOwningSide. It should have no table prefix");
    }

    TableMapping ownerTypeTableMapping = jtm.getMappingHelper().getTableMapping(ownerType);
    TableMapping relatedTypeTableMapping = jtm.getMappingHelper().getTableMapping(relatedType);
    String joinColumnPropertyName =
        ownerTypeTableMapping.getPropertyName(MapperUtils.toLowerCase(joinColumnOwningSide));

    if (joinColumnPropertyName == null) {
      throw new QueryException(
          "Invalid join column "
              + joinColumnOwningSide
              + " table "
              + ownerTypeTableMapping.getTableName()
              + " for object "
              + ownerType.getSimpleName()
              + " does not have a column "
              + joinColumnOwningSide);
    }

    Class<?> joinColumnPropertyType = ownerTypeTableMapping.getPropertyType(joinColumnPropertyName);
    Class<?> relatedTypeIdPropertyType =
        relatedTypeTableMapping.getIdPropertyMapping().getPropertyType();

    // During development some IDE plugins (ex devTools) could use different class loaders.
    // Since this is just a check we use full class name. Assigning the value is done
    // using reflection so its not a problem
    if (!joinColumnPropertyType.getName().equals(relatedTypeIdPropertyType.getName())) {
      throw new QueryException(
          "Property type mismatch. join column "
              + joinColumnOwningSide
              + " property "
              + ownerType.getSimpleName()
              + "."
              + joinColumnPropertyName
              + " is of type "
              + joinColumnPropertyType.getSimpleName()
              + " but the property being joined to "
              + relatedType.getSimpleName()
              + "."
              + relatedTypeTableMapping.getIdPropertyName()
              + " is of type "
              + relatedTypeIdPropertyType.getSimpleName());
    }
  }

  private static void validateHasMany(
      JdbcTemplateMapper jtm,
      Class<?> ownerType,
      Class<?> relatedType,
      String joinColumnManySide,
      String propertyName,
      Object ownerModel) {

    validatePopulatePropertyForCollection(propertyName, ownerModel, ownerType, relatedType);

    if (MapperUtils.isBlank(joinColumnManySide)) {
      throw new QueryException("joinColumnManySide cannot be blank");
    }
    if (joinColumnManySide.contains(".")) {
      throw new QueryException("Invalid joinColumnManySide. It should have no table prefix");
    }

    TableMapping ownerTypeTableMapping = jtm.getMappingHelper().getTableMapping(ownerType);
    TableMapping relatedTypeTableMapping = jtm.getMappingHelper().getTableMapping(relatedType);
    String joinColumnPropertyName =
        relatedTypeTableMapping.getPropertyName(MapperUtils.toLowerCase(joinColumnManySide));
    if (joinColumnPropertyName == null) {
      throw new QueryException(
          "Invalid join column "
              + joinColumnManySide
              + " . table "
              + relatedTypeTableMapping.getTableName()
              + " for object "
              + relatedType.getSimpleName()
              + " does not have a column "
              + joinColumnManySide
              + " or that column has not been mapped");
    }

    Class<?> joinColumnPropertyType =
        relatedTypeTableMapping.getPropertyType(joinColumnPropertyName);
    Class<?> ownerTypeIdPropertyType =
        ownerTypeTableMapping.getIdPropertyMapping().getPropertyType();

    // During development some IDE plugins (ex devTools) could use different class loaders.
    // Since this is just a check we use full class name. Assigning the value is done
    // using reflection so its not a problem
    if (!joinColumnPropertyType.getName().equals(ownerTypeIdPropertyType.getName())) {
      throw new QueryException(
          "Property type mismatch. join column "
              + joinColumnManySide
              + " property "
              + relatedType.getSimpleName()
              + "."
              + joinColumnPropertyName
              + " is of type "
              + joinColumnPropertyType.getSimpleName()
              + " but the property being joined to "
              + ownerType.getSimpleName()
              + "."
              + ownerTypeTableMapping.getIdPropertyName()
              + " is of type "
              + ownerTypeIdPropertyType.getSimpleName());
    }
  }

  private static void validateHasManyThrough(
      Class<?> ownerType,
      Class<?> relatedType,
      String throughJoinTable,
      String throughOwnerTypeJoinColumn,
      String throughRelatedTypeJoinColumn,
      String propertyName,
      Object ownerModel) {
    validatePopulatePropertyForCollection(propertyName, ownerModel, ownerType, relatedType);

    if (MapperUtils.isBlank(throughJoinTable)) {
      throw new QueryException("throughJoinTable cannot be blank");
    }
    if (throughJoinTable.contains(".")) {
      throw new QueryException(
          "Invalid throughJoinTable " + throughJoinTable + " .It should have no prefixes");
    }

    if (MapperUtils.isBlank(throughOwnerTypeJoinColumn)) {
      throw new QueryException("Invalid throughJoinColumns. Cannot be blank");
    }
    if (throughOwnerTypeJoinColumn.contains(".")) {
      throw new QueryException(
          "Invalid throughJoinColumns "
              + throughOwnerTypeJoinColumn
              + " .It should have no prefixes");
    }
    if (MapperUtils.isBlank(throughRelatedTypeJoinColumn)) {
      throw new QueryException("Invalid throughJoinColumns. Cannot be blank");
    }
    if (throughRelatedTypeJoinColumn.contains(".")) {
      throw new QueryException(
          "Invalid throughJoinColumns "
              + throughRelatedTypeJoinColumn
              + " .It should have no prefixes");
    }
  }

  public static void validateWhereAndOrderBy(
      JdbcTemplateMapper jtm,
      String where,
      String orderBy,
      Class<?> ownerType,
      Class<?> relatedType) {
    if (where != null) {
      if (MapperUtils.isBlank(where)) {
        throw new QueryException("where() blank string is invalid. where() is optional");
      }
    }
    if (orderBy == null) {
      return;
    } else if (MapperUtils.isBlank(orderBy)) {
      throw new QueryException("orderBy() blank string is invalid. orderBy() is optional");
    }

    MappingHelper mappingHelper = jtm.getMappingHelper();
    TableMapping ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);
    String ownerTypeTableName = ownerTypeTableMapping.getTableName();

    TableMapping relatedTypeTableMapping = null;
    String relatedTypeTableName = null;
    if (relatedType != null) {
      relatedTypeTableMapping = mappingHelper.getTableMapping(relatedType);
      relatedTypeTableName = relatedTypeTableMapping.getTableName();
    }

    String[] clauses = orderBy.split(",");
    for (String clause : clauses) {
      String clauseStr = MapperUtils.toLowerCase(clause.trim());
      String[] splits = clauseStr.split("\\s+");
      for (String str : splits) {
        if (str.contains(".")) {
          String[] arr = StringUtils.split(str, ".");
          if (arr.length == 2) {
            String tmpTableName = arr[0];
            String tmpColumnName = arr[1];
            if (isValidTableName(tmpTableName, ownerTypeTableName)) {
              if (!isValidTableColumn(ownerTypeTableMapping, tmpColumnName)) {
                throw new QueryException(
                    "orderBy() invalid column name "
                        + tmpColumnName
                        + " Table "
                        + ownerTypeTableName
                        + " for model "
                        + ownerTypeTableMapping.getTableClass().getSimpleName()
                        + " either does not have column "
                        + tmpColumnName
                        + " or is not mapped.");
              }
            } else {
              if (relatedType != null) {
                if (isValidTableName(tmpTableName, relatedTypeTableName)) {
                  if (!isValidTableColumn(relatedTypeTableMapping, tmpColumnName)) {
                    throw new QueryException(
                        "orderBy() invalid column name "
                            + tmpColumnName
                            + " Table "
                            + relatedTypeTableName
                            + " for model "
                            + relatedTypeTableMapping.getTableClass().getSimpleName()
                            + " either does not have column "
                            + tmpColumnName
                            + " or is not mapped.");
                  }
                } else {
                  throw new QueryException("orderBy() invalid table alias " + tmpTableName);
                }
              } else {
                throw new QueryException("orderBy() invalid table alias " + tmpTableName);
              }
            }
          } else {
            throw new QueryException(
                "Invalid orderBy() column names should be prefixed with table alias");
          }
        } else {
          if ("asc".equals(str) || "desc".equals(str)) {
            // do nothing
          } else {
            throw new QueryException(
                "Invalid orderBy(). Note that the column name should be prefixed by table alias.");
          }
        }
      } // for loop
    }
  }

  private static void validatePopulatePropertyForCollection(
      String propertyName, Object ownerModel, Class<?> ownerType, Class<?> relatedType) {
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
    PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
    if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
      throw new QueryException(
          "property "
              + ownerType.getSimpleName()
              + "."
              + propertyName
              + " is not a collection. hasMany() relationship requires it to be a collection");
    }
    Class<?> propertyType = getGenericTypeOfCollection(ownerModel, propertyName);
    if (propertyType == null) {
      throw new QueryException(
          "Collections without generic types are not supported. Collection for property "
              + ownerType.getSimpleName()
              + "."
              + propertyName
              + " does not have a generic type.");
    }
    // During development some IDE plugins (ex devTools) could use different class loaders.
    // Since this is just a check we use full class name. Assigning the value is done
    // using reflection so its not a problem
    if (!propertyType.getName().equals(relatedType.getName())) {
      throw new QueryException(
          "Collection generic type and hasMany relationship type mismatch. "
              + ownerType.getSimpleName()
              + "."
              + propertyName
              + " has generic type "
              + propertyType.getSimpleName()
              + " while the hasMany relationship is of type "
              + relatedType.getSimpleName());
    }
    Object value = bw.getPropertyValue(propertyName);
    if (value == null) {
      throw new QueryException(
          "Query only works with initialized collections. Collection property "
              + ownerType.getSimpleName()
              + "."
              + propertyName
              + " is not initialized");
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

  private static boolean isValidTableName(String inputTableName, String tableName) {
    return MapperUtils.equals(
        MapperUtils.toLowerCase(inputTableName), MapperUtils.toLowerCase(tableName));
  }

  private static boolean isValidTableColumn(TableMapping tableMapping, String columnName) {
    if (tableMapping == null) {
      return true;
    }
    return tableMapping.getPropertyName(columnName) != null;
  }
}
