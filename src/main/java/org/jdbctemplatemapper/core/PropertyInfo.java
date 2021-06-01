package org.jdbctemplatemapper.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PropertyInfo {
	private String propertyName;
	private Class<?> propertyType;
}
