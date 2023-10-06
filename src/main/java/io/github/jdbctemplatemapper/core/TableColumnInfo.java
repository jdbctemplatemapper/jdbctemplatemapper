package io.github.jdbctemplatemapper.core;

import java.util.ArrayList;
import java.util.List;

class TableColumnInfo {
  private final String tableName;
  private final String schemaName;
  private final String catalogName;
  private List<ColumnInfo> columnInfos;

  TableColumnInfo(String tableName, String schemaName, String catalogName,
      List<ColumnInfo> columnInfos) {
    this.tableName = tableName;
    this.schemaName = MapperUtils.isEmpty(schemaName) ? null : schemaName;
    this.catalogName = MapperUtils.isEmpty(catalogName) ? null : catalogName;
    if (columnInfos == null) {
      this.columnInfos = new ArrayList<ColumnInfo>();
    } else {
      this.columnInfos = columnInfos;
    }
  }

  public String getTableName() {
    return tableName;
  }
  
  public String getSchemaName() {
    return schemaName;
  }

  public String getCatalogName() {
    return catalogName;
  }

  public List<ColumnInfo> getColumnInfos() {
    return columnInfos;
  }
}
