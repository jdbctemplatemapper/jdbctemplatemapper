package org.jdbctemplatemapper.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ColumnInfo {
	private String columnName;
	private int columnDataType; // see  java.sql.Types
}
