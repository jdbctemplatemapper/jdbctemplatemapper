package io.github.jdbctemplatemapper.core;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

class MapperUtils {
  
  public static String getTableNameOnly(String str) {
    if(str != null && str.contains("."))  {
        return str.substring(str.lastIndexOf('.') + 1);
    }
    return str;
  }
  
  public static String getFullyQualifiedTableNameForThroughJoinTable(String throughJoinTable, TableMapping tableMapping) {
    if(throughJoinTable != null) {
      if(throughJoinTable.contains(".")) {
        return throughJoinTable;
      }
      else {
        return tableMapping.fullyQualifiedTablePrefix() + throughJoinTable;
      }
    }
    return throughJoinTable;
  }
  
  /**
   * Converts underscore case to camel case. Ex: user_last_name gets converted to userLastName.
   *
   * @param str underscore case string
   * @return the camel case string
   */
  public static String toCamelCaseName(String str) {
    return JdbcUtils.convertUnderscoreNameToPropertyName(str);
  }

  /**
   * Converts camel case to underscore case. Ex: userLastName gets converted to user_last_name. Copy
   * of code from Spring BeanPropertyRowMapper
   *
   * @param str camel case string
   * @return the underscore case string
   */
  public static String toUnderscoreName(String str) {
    if (isEmpty(str)) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    result.append(Character.toLowerCase(str.charAt(0)));
    for (int i = 1; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isUpperCase(c)) {
        result.append('_').append(Character.toLowerCase(c));
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Splits the list into multiple lists of chunk size. Used to split the sql IN clauses since some
   * databases have a limitation of 1024.
   *
   * @param list The list of Long
   * @param chunkSize The size of each chunk
   * @return Collection of lists broken down by chunkSize
   */
  @SuppressWarnings("rawtypes")
  public static Collection chunkTheCollection(Collection<?> collection, Integer chunkSize) {
    Assert.notNull(collection, "collection must not be null");
    AtomicInteger counter = new AtomicInteger();
    return collection
        .stream()
        .filter(e -> e != null)
        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
        .values();
  }

  public static boolean isBlank(final CharSequence cs) {
    int strLen;
    if (cs == null || (strLen = cs.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotBlank(final CharSequence cs) {
    return !isBlank(cs);
  }

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

  public static String toLowerCase(String str) {
    return str != null ? str.toLowerCase(Locale.US) : null;
  }

  public static boolean equals(Object obj1, Object obj2) {
    if (obj1 == null || obj2 == null) {
      return false;
    }

    return obj1.equals(obj2);
  }
}
