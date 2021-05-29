package org.jdbctemplatemapper.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PropertyColumnMapping {
	private String propertyName;
	private String columnName;
}
