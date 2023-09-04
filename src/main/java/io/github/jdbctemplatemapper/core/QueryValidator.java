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

public class QueryValidator {

    private QueryValidator() {
    }

    static void validate(JdbcTemplateMapper jtm, Class<?> ownerType, RelationshipType relationshipType,
            Class<?> relatedType, String joinColumn, String propertyName, String throughJoinTable,
            String throughOwnerTypeJoinColumn, String throughRelatedTypeJoinColumn) {
        
        if(jtm == null) {
            throw new QueryException("JdbcTemplateMapper cannot be null");
        }

        MappingHelper mappingHelper = jtm.getMappingHelper();
        TableMapping ownerTypeTableMapping = mappingHelper.getTableMapping(ownerType);

        if (relatedType != null) {
            TableMapping relatedTypeTableMapping = mappingHelper.getTableMapping(relatedType);
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
            if (relationshipType == RelationshipType.HAS_ONE) {
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                if (!relatedType.isAssignableFrom(pd.getPropertyType())) {
                    throw new QueryException("property type conflict. property " + ownerType.getSimpleName() + "."
                            + propertyName + " is of type " + pd.getPropertyType().getSimpleName()
                            + " while type for hasOne relationship is " + relatedType.getSimpleName());
                }

                if (MapperUtils.isBlank(joinColumn)) {
                    throw new QueryException("joinColumn cannot be blank");
                }
                if (joinColumn.contains(".")) {
                    throw new QueryException("Invalid joinColumn. It should have no table prefix");
                }

                // hasOne join column is on main class
                String joinColumnPropertyName = ownerTypeTableMapping
                        .getPropertyName(MapperUtils.toLowerCase(joinColumn));

                if (joinColumnPropertyName == null) {
                    throw new QueryException("Invalid join column " + joinColumn + " table "
                            + ownerTypeTableMapping.getTableName() + " for object " + ownerType.getSimpleName()
                            + " does not have a column " + joinColumn);
                }

                Class<?> joinColumnPropertyType = ownerTypeTableMapping.getPropertyType(joinColumnPropertyName);
                Class<?> relatedTypeIdPropertyType = relatedTypeTableMapping.getIdPropertyMapping().getPropertyType();

                if (joinColumnPropertyType != relatedTypeIdPropertyType) {
                    throw new QueryException("Property type mismatch. join column " + joinColumn + " property "
                            + ownerType.getSimpleName() + "." + joinColumnPropertyName + " is of type "
                            + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
                            + relatedType.getSimpleName() + "." + relatedTypeTableMapping.getIdPropertyName()
                            + " is of type " + relatedTypeIdPropertyType.getSimpleName());
                }
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                validatePopulatePropertyForCollection(propertyName, ownerModel, ownerType, relatedType);

                if (MapperUtils.isBlank(joinColumn)) {
                    throw new QueryException("joinColumn cannot be blank");
                }
                if (joinColumn.contains(".")) {
                    throw new QueryException("Invalid joinColumn. It should have no table prefix");
                }

                // hasMany() join column is on related class
                String joinColumnPropertyName = relatedTypeTableMapping
                        .getPropertyName(MapperUtils.toLowerCase(joinColumn));

                if (joinColumnPropertyName == null) {
                    throw new QueryException("Invalid join column " + joinColumn + " . table "
                            + relatedTypeTableMapping.getTableName() + " for object " + relatedType.getSimpleName()
                            + " does not have a column " + joinColumn + " or that column has not been mapped");
                }

                Class<?> joinColumnPropertyType = relatedTypeTableMapping.getPropertyType(joinColumnPropertyName);
                Class<?> ownerTypeIdPropertyType = ownerTypeTableMapping.getIdPropertyMapping().getPropertyType();

                if (joinColumnPropertyType != ownerTypeIdPropertyType) {
                    throw new QueryException("Property type mismatch. join column " + joinColumn + " property "
                            + relatedType.getSimpleName() + "." + joinColumnPropertyName + " is of type "
                            + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
                            + ownerType.getSimpleName() + "." + ownerTypeTableMapping.getIdPropertyName()
                            + " is of type " + ownerTypeIdPropertyType.getSimpleName());
                }
            } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
                validatePopulatePropertyForCollection(propertyName, ownerModel, ownerType, relatedType);

                if (MapperUtils.isBlank(throughJoinTable)) {
                    throw new QueryException("throughJoinTable cannot be blank");

                }
                if (throughJoinTable.contains(".")) {
                    throw new QueryException("Invalid throughJoinTable " + throughJoinTable +" .It should have no prefixes");
                }

                if (MapperUtils.isBlank(throughOwnerTypeJoinColumn)) {
                    throw new QueryException("Invalid throughJoinColumns. Cannot be blank");

                }
                if (throughOwnerTypeJoinColumn.contains(".")) {
                    throw new QueryException("Invalid throughJoinColumns " + throughOwnerTypeJoinColumn + " .It should have no prefixes");
                }

                if (MapperUtils.isBlank(throughRelatedTypeJoinColumn)) {
                    throw new QueryException("Invalid throughJoinColumns. Cannot be blank");
                }
                if (throughRelatedTypeJoinColumn.contains(".")) {
                    throw new QueryException("Invalid throughJoinColumns " + throughRelatedTypeJoinColumn + " .It should have no prefixes");
                }
            }
        }
    }
    
    

    public static void validateWhereAndOrderBy(JdbcTemplateMapper jtm, String where, String orderBy, Class<?> ownerType,
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
                        if (validTableName(tmpTableName, ownerTypeTableName)) {
                            if (!validTableColumn(ownerTypeTableMapping, tmpColumnName)) {
                                throw new QueryException("orderBy() invalid column name " + tmpColumnName + " Table "
                                        + ownerTypeTableName + " for model "
                                        + ownerTypeTableMapping.getTableClass().getSimpleName()
                                        + " either does not have column " + tmpColumnName + " or is not mapped.");
                            }
                        } else {
                            if (relatedType != null) {
                                if (validTableName(tmpTableName, relatedTypeTableName)) {
                                    if (!validTableColumn(relatedTypeTableMapping, tmpColumnName)) {
                                        throw new QueryException("orderBy() invalid column name " + tmpColumnName
                                                + " Table " + relatedTypeTableName + " for model "
                                                + relatedTypeTableMapping.getTableClass().getSimpleName()
                                                + " either does not have column " + tmpColumnName
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
                        throw new QueryException("Invalid orderBy() column names should be prefixed with table alias");
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

    private static void validatePopulatePropertyForCollection(String propertyName, Object ownerModel, Class<?> ownerType,
            Class<?> relatedType) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(ownerModel);
        PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
        if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
            throw new QueryException("property " + ownerType.getSimpleName() + "." + propertyName
                    + " is not a collection. hasMany() relationship requires it to be a collection");
        }
        Class<?> propertyType = getGenericTypeOfCollection(ownerModel, propertyName);
        if (propertyType == null) {
            throw new QueryException("Collections without generic types are not supported. Collection for property "
                    + ownerType.getSimpleName() + "." + propertyName + " does not have a generic type.");
        }
        if (!propertyType.isAssignableFrom(relatedType)) {
            throw new QueryException(
                    "Collection generic type and hasMany relationship type mismatch. " + ownerType.getSimpleName() + "."
                            + propertyName + " has generic type " + propertyType.getSimpleName()
                            + " while the hasMany relationship is of type " + relatedType.getSimpleName());
        }
        Object value = bw.getPropertyValue(propertyName);
        if (value == null) {
            throw new QueryException("Query only works with initialized collections. Collection property "
                    + ownerType.getSimpleName() + "." + propertyName + " is not initialized");
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

    private static boolean validTableName(String inputTableName, String tableName) {
        return MapperUtils.equals(MapperUtils.toLowerCase(inputTableName), tableName);
    }

    private static boolean validTableColumn(TableMapping tableMapping, String columnName) {
        if (tableMapping == null) {
            return true;
        }
        return tableMapping.getPropertyName(columnName) != null;
    }

}
