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

    static void validate(JdbcTemplateMapper jtm, Class<?> mainClazz, RelationshipType relationshipType,
            Class<?> relatedClazz, String joinColumn, String propertyName, String throughJoinTable,
            String throughMainClazzJoinColumn, String throughRelatedClazzJoinColumn) {

        MappingHelper mappingHelper = jtm.getMappingHelper();
        TableMapping mainClazzTableMapping = mappingHelper.getTableMapping(mainClazz);

        if (relatedClazz != null) {
            TableMapping relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);
            Object mainModel = null;
            try {
                mainModel = mainClazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
            if (!bw.isReadableProperty(propertyName)) {
                throw new QueryException(
                        "Invalid property name " + propertyName + " for class " + mainClazz.getSimpleName());
            }
            if (relationshipType == RelationshipType.HAS_ONE) {
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                if (!relatedClazz.isAssignableFrom(pd.getPropertyType())) {
                    throw new QueryException("property type conflict. property " + mainClazz.getSimpleName() + "."
                            + propertyName + " is of type " + pd.getPropertyType().getSimpleName()
                            + " while type for hasOne relationship is " + relatedClazz.getSimpleName());
                }

                if (MapperUtils.isBlank(joinColumn)) {
                    throw new QueryException("joinColumn cannot be blank");
                }
                if (joinColumn.contains(".")) {
                    throw new QueryException("Invalid joinColumn. It should have no table prefix");
                }

                // hasOne join column is on main class
                String joinColumnPropertyName = mainClazzTableMapping
                        .getPropertyName(MapperUtils.toLowerCase(joinColumn));

                if (joinColumnPropertyName == null) {
                    throw new QueryException("Invalid join column " + joinColumn + " table "
                            + mainClazzTableMapping.getTableName() + " for object " + mainClazz.getSimpleName()
                            + " does not have a column " + joinColumn);
                }

                Class<?> joinColumnPropertyType = mainClazzTableMapping.getPropertyType(joinColumnPropertyName);
                Class<?> relatedClazzIdPropertyType = relatedClazzTableMapping.getIdPropertyMapping().getPropertyType();

                if (joinColumnPropertyType != relatedClazzIdPropertyType) {
                    throw new QueryException("Property type mismatch. join column " + joinColumn + " property "
                            + mainClazz.getSimpleName() + "." + joinColumnPropertyName + " is of type "
                            + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
                            + relatedClazz.getSimpleName() + "." + relatedClazzTableMapping.getIdPropertyName()
                            + " is of type " + relatedClazzIdPropertyType.getSimpleName());
                }
            } else if (relationshipType == RelationshipType.HAS_MANY) {
                validatePopulatePropertyForCollection(propertyName, mainModel, mainClazz, relatedClazz);

                if (MapperUtils.isBlank(joinColumn)) {
                    throw new QueryException("joinColumn cannot be blank");
                }
                if (joinColumn.contains(".")) {
                    throw new QueryException("Invalid joinColumn. It should have no table prefix");
                }

                // hasMany() join column is on related class
                String joinColumnPropertyName = relatedClazzTableMapping
                        .getPropertyName(MapperUtils.toLowerCase(joinColumn));

                if (joinColumnPropertyName == null) {
                    throw new QueryException("Invalid join column " + joinColumn + " . table "
                            + relatedClazzTableMapping.getTableName() + " for object " + relatedClazz.getSimpleName()
                            + " does not have a column " + joinColumn + " or that column has not been mapped");
                }

                Class<?> joinColumnPropertyType = relatedClazzTableMapping.getPropertyType(joinColumnPropertyName);
                Class<?> mainClazzIdPropertyType = mainClazzTableMapping.getIdPropertyMapping().getPropertyType();

                if (joinColumnPropertyType != mainClazzIdPropertyType) {
                    throw new QueryException("Property type mismatch. join column " + joinColumn + " property "
                            + relatedClazz.getSimpleName() + "." + joinColumnPropertyName + " is of type "
                            + joinColumnPropertyType.getSimpleName() + " but the property being joined to "
                            + mainClazz.getSimpleName() + "." + mainClazzTableMapping.getIdPropertyName()
                            + " is of type " + mainClazzIdPropertyType.getSimpleName());
                }
            } else if (relationshipType == RelationshipType.HAS_MANY_THROUGH) {
                validatePopulatePropertyForCollection(propertyName, mainModel, mainClazz, relatedClazz);

                if (MapperUtils.isBlank(throughJoinTable)) {
                    throw new QueryException("throughJoinTable cannot be blank");

                }
                if (throughJoinTable.contains(".")) {
                    throw new QueryException("Invalid throughJoinTable " + throughJoinTable +" .It should have no prefixes");
                }

                if (MapperUtils.isBlank(throughMainClazzJoinColumn)) {
                    throw new QueryException("Invalid throughJoinColumns. Cannot be blank");

                }
                if (throughMainClazzJoinColumn.contains(".")) {
                    throw new QueryException("Invalid throughJoinColumns " + throughMainClazzJoinColumn + " .It should have no prefixes");
                }

                if (MapperUtils.isBlank(throughRelatedClazzJoinColumn)) {
                    throw new QueryException("Invalid throughJoinColumns. Cannot be blank");
                }
                if (throughRelatedClazzJoinColumn.contains(".")) {
                    throw new QueryException("Invalid throughJoinColumns " + throughRelatedClazzJoinColumn + " .It should have no prefixes");
                }
            }
        }
    }

    public static void validateWhereAndOrderBy(JdbcTemplateMapper jtm, String where, String orderBy, Class<?> mainClazz,
            Class<?> relatedClazz) {

        if (where != null) {
            if (MapperUtils.isBlank(where)) {
                throw new QueryException("where() blank string is invalid. where() is an optional");
            }
        }

        if (orderBy == null) {
            return;
        } else if (MapperUtils.isBlank(orderBy)) {
            throw new QueryException("orderBy() blank string is invalid. orderBy() is an optional");
        }

        MappingHelper mappingHelper = jtm.getMappingHelper();
        TableMapping mainClazzTableMapping = mappingHelper.getTableMapping(mainClazz);
        String mainClazzTableName = mainClazzTableMapping.getTableName();

        TableMapping relatedClazzTableMapping = null;
        String relatedClazzTableName = null;
        if (relatedClazz != null) {
            relatedClazzTableMapping = mappingHelper.getTableMapping(relatedClazz);
            relatedClazzTableName = relatedClazzTableMapping.getTableName();
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
                        if (validTableName(tmpTableName, mainClazzTableName)) {
                            if (!validTableColumn(mainClazzTableMapping, tmpColumnName)) {
                                throw new QueryException("orderBy() invalid column name " + tmpColumnName + " Table "
                                        + mainClazzTableName + " for model "
                                        + mainClazzTableMapping.getTableClass().getSimpleName()
                                        + " either does not have column " + tmpColumnName + " or is not mapped.");
                            }
                        } else {
                            if (relatedClazz != null) {
                                if (validTableName(tmpTableName, relatedClazzTableName)) {
                                    if (!validTableColumn(relatedClazzTableMapping, tmpColumnName)) {
                                        throw new QueryException("orderBy() invalid column name " + tmpColumnName
                                                + " Table " + relatedClazzTableName + " for model "
                                                + relatedClazzTableMapping.getTableClass().getSimpleName()
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

    private static void validatePopulatePropertyForCollection(String propertyName, Object mainModel, Class<?> mainClazz,
            Class<?> relatedClazz) {
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mainModel);
        PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
        if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
            throw new QueryException("property " + mainClazz.getSimpleName() + "." + propertyName
                    + " is not a collection. hasMany() relationship requires it to be a collection");
        }
        Class<?> propertyType = getGenericTypeOfCollection(mainModel, propertyName);
        if (propertyType == null) {
            throw new QueryException("Collections without generic types are not supported. Collection for property "
                    + mainClazz.getSimpleName() + "." + propertyName + " does not have a generic type.");
        }
        if (!propertyType.isAssignableFrom(relatedClazz)) {
            throw new QueryException(
                    "Collection generic type and hasMany relationship type mismatch. " + mainClazz.getSimpleName() + "."
                            + propertyName + " has generic type " + propertyType.getSimpleName()
                            + " while the hasMany relationship is of type " + relatedClazz.getSimpleName());
        }
        Object value = bw.getPropertyValue(propertyName);
        if (value == null) {
            throw new QueryException("Query only works with initialized collections. Collection property "
                    + mainClazz.getSimpleName() + "." + propertyName + " is not initialized");
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

    private static boolean validTableName(String modelClazzTableName, String tableName) {
        return MapperUtils.toLowerCase(modelClazzTableName).equals(tableName);
    }

    private static boolean validTableColumn(TableMapping tableMapping, String columnName) {
        if (tableMapping == null) {
            return true;
        }
        return tableMapping.getPropertyName(columnName) != null;

    }

}
