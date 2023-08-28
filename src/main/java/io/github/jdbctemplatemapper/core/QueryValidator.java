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
            Class<?> relatedClazz, String joinColumn, String propertyName) {

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

                if (mainClazzTableMapping.getPropertyName(joinColumn) == null) {
                    throw new QueryException("Invalid join column " + joinColumn + " table "
                            + mainClazzTableMapping.getTableName() + " for object " + mainClazz.getSimpleName()
                            + " does not have a column " + joinColumn);
                }

            } else if (relationshipType == RelationshipType.HAS_MANY) {
                if (relatedClazzTableMapping.getPropertyName(joinColumn) == null) {
                    throw new QueryException("Invalid join column " + joinColumn + " . table "
                            + relatedClazzTableMapping.getTableName() + " for object " + relatedClazz.getSimpleName()
                            + " does not have a column " + joinColumn);
                }

                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                if (!Collection.class.isAssignableFrom(pd.getPropertyType())) {
                    throw new QueryException("property " + mainClazz.getSimpleName() + "." + propertyName
                            + " is not a collection. hasMany() relationship requires it to be a collection");
                }

                Class<?> propertyType = getGenericTypeOfCollection(mainModel, propertyName);
                if (propertyType == null) {
                    throw new QueryException(
                            "Collections without generic types are not supported. Collection for property "
                                    + mainClazz.getSimpleName() + "." + propertyName
                                    + " does not have a generic type.");
                }

                if (!propertyType.isAssignableFrom(relatedClazz)) {
                    throw new QueryException("Collection generic type and hasMany relationship type mismatch. "
                            + mainClazz.getSimpleName() + "." + propertyName + " has generic type "
                            + propertyType.getSimpleName() + " while the hasMany relationship is of type "
                            + relatedClazz.getSimpleName());
                }
                Object value = bw.getPropertyValue(propertyName);
                if (value == null) {
                    throw new QueryException("Query only works with initialized collections. Collection property "
                            + mainClazz.getSimpleName() + "." + propertyName + " is not initialized");
                }
            }
        }
    }

    static void validate(JdbcTemplateMapper jtm, Class<?> modelClazz, String whereClause, String orderBy) {
        MappingHelper mappingHelper = jtm.getMappingHelper();
        TableMapping modelClazzTableMapping = mappingHelper.getTableMapping(modelClazz);
        String modelTableName = modelClazzTableMapping.getTableName();

        validateOrderBy(orderBy, modelClazzTableMapping);

    }

    private static void validateOrderBy(String orderBy, TableMapping modelClazzTableMapping) {
        if (orderBy != null) {
            if (MapperUtils.isBlank(orderBy)) {
                throw new QueryException(
                        "orderBy() blank string is invalid. Don't invoke orderBy() method if no value");
            }

            String modelTableName = modelClazzTableMapping.getTableName();
     
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
                            if (!MapperUtils.toLowerCase(modelTableName).equals(tmpTableName)) {
                                throw new QueryException(
                                        "orderBy() Invalid table alias. Column should be prefixed by "
                                                + modelTableName);
                            }
                            if (modelClazzTableMapping.getPropertyName(tmpColumnName) == null) {
                                throw new QueryException("orderBy() invalid column name "+ tmpColumnName +" Table " + modelTableName + " for model "
                                        + modelClazzTableMapping.getTableClass().getSimpleName()
                                        + " either does not have column " + tmpColumnName + " or is not mapped.");
                            }
                        } else {
                            throw new QueryException(
                                    "Invalid orderBy() column names should be prefixed with table alias. In this case it should be something like "
                                            + modelClazzTableMapping.getTableName());
                        }
                    } else {
                        if ("asc".equals(str) || "desc".equals(str)) {
                            // do nothing
                        } else {
                            throw new QueryException(
                                    "Invalid orderBy() column names should be prefixed with table alias. In this case it should be something like "
                                            + modelClazzTableMapping.getTableName() + "." + str);
                        }
                    }
                }
            }
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
