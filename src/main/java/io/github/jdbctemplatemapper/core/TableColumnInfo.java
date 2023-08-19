package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

class TableColumnInfo {
    private final String tableName;
    private List<ColumnInfo> columnInfos;

    TableColumnInfo(String tableName, List<ColumnInfo> columnInfos) {
        this.tableName = tableName;
        if (columnInfos == null) {
            this.columnInfos = new ArrayList<ColumnInfo>();
        } else {
            this.columnInfos = columnInfos;
        }
    }

    public String getTableName() {
        return tableName;
    }

    public List<ColumnInfo> getColumnInfos() {
        return columnInfos;
    }

}
