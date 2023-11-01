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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * Utility methods used by mapper.
 *
 * @author ajoseph
 */
class MapperUtils {
  public static final String OWNER_COL_ALIAS_PREFIX = "o";
  public static final String RELATED_COL_ALIAS_PREFIX = "r";

  public static boolean isNumericSqlType(int sqlType) {
    return JdbcUtils.isNumeric(sqlType);
  }

  public static String getTableNameOnly(String str) {
    if (str != null && str.contains(".")) {
      return str.substring(str.lastIndexOf('.') + 1);
    }
    return str;
  }

  public static String columnPrefix(String tableAlias, String tableName) {
    return tableAlias == null ? tableName : tableAlias;
  }

  public static String tableStrForFrom(String tableAlias, String fullyQualifiedTableName) {
    return tableAlias == null ? fullyQualifiedTableName
        : fullyQualifiedTableName + " " + tableAlias;
  }

  // if user entered someschema.tablename use that. otherwise get the
  // schema/catalog(table prefix) and concatenate with tableName
  public static String getFullyQualifiedTableNameForThroughJoinTable(String throughJoinTable,
      TableMapping tableMapping) {
    if (throughJoinTable != null) {
      if (throughJoinTable.contains(".")) {
        return throughJoinTable;
      } else {
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
   * databases have a limitation on the number.
   *
   * @param collection the list to chunk
   * @param chunkSize The size of each chunk
   * @return Collection of lists broken down by chunkSize
   */
  public static List<List<?>> chunkTheList(List<?> collection, Integer chunkSize) {
    List<List<?>> chunks = new ArrayList<>();
    if (collection != null) {
      for (int i = 0; i < collection.size(); i += chunkSize) {
        chunks.add(collection.subList(i, Math.min(i + chunkSize, collection.size())));
      }
    }
    return chunks;
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

  @SuppressWarnings("all")
  public static boolean isEmpty(Collection coll) {
    return (coll == null || coll.isEmpty());
  }

  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  @SuppressWarnings("all")
  public static boolean isNotEmpty(Collection coll) {
    return !isEmpty(coll);
  }

  public static String toLowerCase(String str) {
    return str != null ? str.toLowerCase(Locale.US) : null;
  }

}
