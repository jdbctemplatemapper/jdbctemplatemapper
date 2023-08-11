package io.github.jdbctemplatemapper.core;

import java.util.regex.Pattern;

import org.springframework.jdbc.support.JdbcUtils;

public class AppUtils {
	// Convert camel case to underscore case regex pattern. Pattern is thread safe
	private static Pattern TO_UNDERSCORE_NAME_PATTERN = Pattern.compile("(.)(\\p{Upper})");

	
	/**
	 * Converts underscore case to camel case. Ex: user_last_name gets converted to
	 * userLastName.
	 *
	 * @param str snake case string
	 * @return the camel case string
	 */
	public static String convertSnakeToCamelCase(String str) {
		return JdbcUtils.convertUnderscoreNameToPropertyName(str);
	}

	/**
	 * Converts camel case to underscore case. Ex: userLastName gets converted to
	 * user_last_name.
	 *
	 * @param str underscore case string
	 * @return the camel case string
	 */
	public static String convertPropertyNameToUnderscoreName(String str) {
		return TO_UNDERSCORE_NAME_PATTERN.matcher(str).replaceAll("$1_$2").toLowerCase();
	}

}
