package org.jdbctemplatemapper.core;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.DatabaseMetaDataCallback;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;

public class DatabaseUtils {
  // Map key - table name,
  //     value - the list of database column names
  private Map<String, List<ColumnInfo>> tableColumnInfoCache = new ConcurrentHashMap<>();

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate npJdbcTemplate;
  private String schemaName;
  private String catalogName;

  // this is for the call to databaseMetaData.getColumns() just in case a database needs something
  // other than null
  private String metaDataColumnNamePattern;

  public DatabaseUtils(
      JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate npJdbcTemplate, String schemaName) {
    this.jdbcTemplate = jdbcTemplate;
    this.npJdbcTemplate = npJdbcTemplate;
    this.schemaName = schemaName;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public String getCatalogName() {
    return catalogName;
  }

  public void setCatalogName(String catalogName) {
    this.catalogName = catalogName;
  }

  public void setMetaDataColumnNamePattern(String metaDataColumnNamePattern) {
    this.metaDataColumnNamePattern = metaDataColumnNamePattern;
  }

  public List<ColumnInfo> getTableColumnInfo(String tableName) {
    Assert.hasLength(tableName, "tableName must not be empty");
    try {
      List<ColumnInfo> columnInfos = tableColumnInfoCache.get(tableName);
      if (CommonUtils.isEmpty(columnInfos)) {
        // Using Spring JdbcUtils.extractDatabaseMetaData() since it has some robust processing for
        // connection access
        columnInfos =
            JdbcUtils.extractDatabaseMetaData(
                jdbcTemplate.getDataSource(),
                new DatabaseMetaDataCallback<List<ColumnInfo>>() {
                  public List<ColumnInfo> processMetaData(DatabaseMetaData metadata)
                      throws SQLException, MetaDataAccessException {
                    ResultSet rs = null;
                    try {
                      List<ColumnInfo> columnInfoList = new ArrayList<>();
                      rs =
                          metadata.getColumns(
                              catalogName, schemaName, tableName, metaDataColumnNamePattern);
                      while (rs.next()) {
                        columnInfoList.add(
                            new ColumnInfo(rs.getString("COLUMN_NAME"), rs.getInt("DATA_TYPE")));
                      }
                      if (CommonUtils.isNotEmpty(columnInfoList)) {
                        tableColumnInfoCache.put(tableName, columnInfoList);
                      }
                      return columnInfoList;
                    } finally {
                      JdbcUtils.closeResultSet(rs);
                    }
                  }
                });
      }
      return columnInfos;
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }
  
  
  /**
   * Gets the resultSet lower case column names ie the column names in the 'select' statement of the
   * sql
   *
   * @param rs The jdbc ResultSet
   * @return List of select column names in lower case
   */
  public List<String> getResultSetColumnNames(ResultSet rs) {
    List<String> rsColNames = new ArrayList<>();
    try {
      ResultSetMetaData rsmd = rs.getMetaData();
      int numberOfColumns = rsmd.getColumnCount();
      // jdbc indexes start at 1
      for (int i = 1; i <= numberOfColumns; i++) {
        rsColNames.add(rsmd.getColumnLabel(i).toLowerCase());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return rsColNames;
  }
  
  /**
   * Get the fully qualified table name. If schema is present then will be schemaName.tableName
   * otherwise just tableName
   *
   * @param tableName The table name
   * @return The fully qualified table name
   */
  public String fullyQualifiedTableName(String tableName) {
    Assert.hasLength(tableName, "tableName must not be empty");
    if (CommonUtils.isNotEmpty(getSchemaName())) {
      return getSchemaName() + "." + tableName;
    }
    return tableName;
  }
}
