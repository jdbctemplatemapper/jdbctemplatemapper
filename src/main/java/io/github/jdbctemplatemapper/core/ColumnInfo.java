package io.github.jdbctemplatemapper.core;

class ColumnInfo {
    private String columnName;
    private int columnSqlDataType; // see java.sql.Types

    public ColumnInfo(String columnName, int columnSqlDataType) {
        this.columnName = MapperUtils.toLowerCase(columnName);
        this.columnSqlDataType = columnSqlDataType;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnSqlDataType() {
        return columnSqlDataType;
    }

}
