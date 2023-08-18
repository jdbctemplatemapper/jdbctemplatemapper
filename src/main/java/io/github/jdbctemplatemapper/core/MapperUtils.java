package io.github.jdbctemplatemapper.core;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;

class MapperUtils {
    /**
     * Converts underscore case to camel case. Ex: user_last_name gets converted to
     * userLastName.
     *
     * @param str underscore case string
     * @return the camel case string
     */
    public static String toCamelCaseName(String str) {
        return JdbcUtils.convertUnderscoreNameToPropertyName(str);
    }

    /**
     * Converts camel case to underscore case. Ex: userLastName gets converted to
     * user_last_name. Copy of code from Spring BeanPropertyRowMapper
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
     * Splits the list into multiple lists of chunk size. Used to split the sql IN
     * clauses since some databases have a limitation of 1024.
     *
     * @param list      The list of Long
     * @param chunkSize The size of each chunk
     * @return Collection of lists broken down by chunkSize
     */
    public static <T extends Object> Collection<List<T>> chunkTheCollection(
            Collection<T> collection, Integer chunkSize) {
          Assert.notNull(collection, "collection must not be null");
          AtomicInteger counter = new AtomicInteger();
          Collection<List<T>> result =
              collection
                  .stream()
                  .filter(e -> e != null)
                  .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                  .values();
          return result;
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
}
