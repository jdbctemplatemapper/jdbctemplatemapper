package org.jdbctemplatemapper.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class CommonUtils {

  // Convert camel case to snake case regex pattern. Pattern is thread safe
  private static Pattern TO_SNAKE_CASE_PATTERN = Pattern.compile("(.)(\\p{Upper})");

  // Map key - camel case string,
  //     value - snake case string
  private static Map<String, String> camelToSnakeCache = new ConcurrentHashMap<>();

  // Map key - simple Class name
  //     value - list of property names
  private static Map<String, List<PropertyInfo>> objectPropertyInfoCache =
      new ConcurrentHashMap<>();
  
  
  // Map key - object class name - propertyName
  //     value - The getter Method
  private static Map<String, Method> objectGetMethodCache = new ConcurrentHashMap<>();

  // Map key - object class name - propertyName
  //     value - The setter Method
  private static Map<String, Method> objectSetMethodCache = new ConcurrentHashMap<>();

  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  @SuppressWarnings("all")
  public static boolean isEmpty(Collection coll) {
    return (coll == null || coll.isEmpty());
  }

  @SuppressWarnings("all")
  public static boolean isNotEmpty(Collection coll) {
    return !isEmpty(coll);
  }

  /**
   * Get property information of an object. The property infos are cached by the object class name
   *
   * @param obj The object
   * @return List of PropertyInfo
   */
  public static List<PropertyInfo> getObjectPropertyInfo(Object obj) {
    Assert.notNull(obj, "Object must not be null");
    List<PropertyInfo> propertyInfoList = objectPropertyInfoCache.get(obj.getClass().getName());
    if (propertyInfoList == null) {
      BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
      propertyInfoList = new ArrayList<>();
      PropertyDescriptor[] propertyDescriptors = bw.getPropertyDescriptors();
      for (PropertyDescriptor pd : propertyDescriptors) {
        String propName = pd.getName();
        // log.debug("Property name:{}" + propName);
        if ("class".equals(propName)) {
          continue;
        } else {
          propertyInfoList.add(new PropertyInfo(propName, pd.getPropertyType()));
        }
      }
      objectPropertyInfoCache.put(obj.getClass().getName(), propertyInfoList);
    }
    return propertyInfoList;
  }

  public static Class<?> getGenericTypeOfCollection(Object mainObj, String propertyName) {
    Assert.notNull(mainObj, "mainObj must not be null");
    Assert.notNull(propertyName, "propertyName must not be null");
    try {
      Field field = mainObj.getClass().getDeclaredField(propertyName);

      ParameterizedType pt = (ParameterizedType) field.getGenericType();
      Type[] genericType = pt.getActualTypeArguments();

      if (genericType != null && genericType.length > 0) {
        return Class.forName(genericType[0].getTypeName());
      } else {
        throw new RuntimeException(
            "Could not find generic type for property: "
                + propertyName
                + " in class: "
                + mainObj.getClass().getSimpleName());
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Class<?> getPropertyClass(Object obj, String propertyName) {
    Assert.notNull(obj, "obj must not be null");
    Assert.notNull(propertyName, "propertyName must not be null");

    List<PropertyInfo> propertyInfoList = CommonUtils.getObjectPropertyInfo(obj);
    PropertyInfo propertyInfo =
        propertyInfoList
            .stream()
            .filter(pi -> propertyName.equals(pi.getPropertyName()))
            .findAny()
            .orElse(null);

    if (propertyInfo == null) {
      throw new RuntimeException(
          "property:" + propertyName + " not found in class:" + obj.getClass().getName());
    } else {
      return propertyInfo.getPropertyType();
    }
  }

  /**
   * Converts camel case to snake case. Ex: userLastName gets converted to user_last_name. The
   * conversion info is cached.
   *
   * @param str camel case String
   * @return the snake case string to lower case.
   */
  public static String convertCamelToSnakeCase(String str) {
    String snakeCase = camelToSnakeCache.get(str);
    if (snakeCase == null) {
      if (str != null) {
        snakeCase = TO_SNAKE_CASE_PATTERN.matcher(str).replaceAll("$1_$2").toLowerCase();
        camelToSnakeCache.put(str, snakeCase);
      }
    }
    return snakeCase;
  }

  /**
   * Converts snake case to camel case. Ex: user_last_name gets converted to userLastName. The
   * conversion info is cached.
   *
   * @param str snake case string
   * @return the camel case string
   */
  public static String convertSnakeToCamelCase(String str) {
    return JdbcUtils.convertUnderscoreNameToPropertyName(str);
  }
  
  
  /**
   * Set a property using reflection. Used instead of BeanWrapper since it is too heavy creating an
   * instance of BeanWrapper just to set 1 property. The setter Methods are cached for performance.
   *
   * @param obj The object
   * @param propertyName The property that needs to be set
   * @param val The value that needs to be set
   */
  public static void setPropertyValue(Object obj, String propertyName, Object val) {
    Assert.notNull(obj, "Object must not be null");
    Assert.hasLength(propertyName, "propertyName must not be empty");

    Method method = objectSetMethodCache.get(obj.getClass().getName() + "-" + propertyName);
    if (method == null) {
      Field field = ReflectionUtils.findField(obj.getClass(), propertyName);
      if (field == null) {
        throw new RuntimeException(
            "property " + propertyName + " not found in class " + obj.getClass().getName());
      }
      method =
          ReflectionUtils.findMethod(
              obj.getClass(), "set" + StringUtils.capitalize(propertyName), field.getType());
      if (method == null) {
        throw new RuntimeException(
            "method set"
                + StringUtils.capitalize(propertyName)
                + "("
                + field.getType().getName()
                + ") not found in object "
                + obj.getClass().getName());
      }
      objectSetMethodCache.put(obj.getClass().getName() + "-" + propertyName, method);
    }
    ReflectionUtils.invokeMethod(method, obj, val);
  }
  
  /**
   * Get a property using reflection. Used instead of BeanWrapper since it is too heavy creating an
   * instance of BeanWrapper to just lookup 1 property. The getter Methods are cached for
   * performance.
   *
   * @param obj The object
   * @param propertyName The property that needs needs to be retrieved
   * @return the value of property
   */
  public static Object getPropertyValue(Object obj, String propertyName) {
    Assert.notNull(obj, "Object must not be null");
    Assert.hasLength(propertyName, "propertyName must not be empty");

    Method method = objectGetMethodCache.get(obj.getClass().getName() + "-" + propertyName);
    if (method == null) {
      method =
          ReflectionUtils.findMethod(obj.getClass(), "get" + StringUtils.capitalize(propertyName));
      if (method == null) {
        throw new RuntimeException(
            "method get"
                + StringUtils.capitalize(propertyName)
                + "() not found in object "
                + obj.getClass().getName());
      }
      objectGetMethodCache.put(obj.getClass().getName() + "-" + propertyName, method);
    }
    return ReflectionUtils.invokeMethod(method, obj);
  }
  
  
  /**
   * Converts an object to a Map. The map key will be object property name and value with
   * corresponding object property value.
   *
   * @param obj The object to be converted.
   * @return Map with key: property name, value: object value
   */
  public static Map<String, Object> convertObjectToMap(Object obj) {
    Assert.notNull(obj, "Object must not be null");
    Map<String, Object> camelCaseAttrs = new HashMap<>();
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(obj);
    for (PropertyInfo propertyInfo : CommonUtils.getObjectPropertyInfo(obj)) {
      camelCaseAttrs.put(
          propertyInfo.getPropertyName(), bw.getPropertyValue(propertyInfo.getPropertyName()));
    }
    return camelCaseAttrs;
  }
  
  /**
   * Converts an object to a map with key as database column names and values the corresponding
   * object value. Camel case property names are converted to snake case. For example property name
   * 'userLastName' will get converted to map key 'user_last_name' and assigned the corresponding
   * object value.
   *
   * @param obj The object to convert
   * @return A map with keys that are in snake case to match database column names and values
   *     corresponding to the object property
   */
  public static Map<String, Object> convertToSnakeCaseAttributes(Object obj) {
    Assert.notNull(obj, "Object must not be null");

    Map<String, Object> camelCaseAttrs = CommonUtils.convertObjectToMap(obj);
    Map<String, Object> snakeCaseAttrs = new HashMap<>();
    for (String key : camelCaseAttrs.keySet()) {
      // ex: lastName will get converted to last_name
      String snakeCaseKey = CommonUtils.convertCamelToSnakeCase(key);
      snakeCaseAttrs.put(snakeCaseKey, camelCaseAttrs.get(key));
    }
    return snakeCaseAttrs;
  }


}
