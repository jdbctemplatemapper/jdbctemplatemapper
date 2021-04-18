package org.jdbctemplatemapper.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
/**
 * Datasource and jdbc template configuration for the app.
 * 
 * @author ajoseph
 *
 */
@Configuration
public class DatabaseConfig {

  @Bean(name = "sqlDataSource")
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource sqlDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "npJdbcTemplate")
  public NamedParameterJdbcTemplate npJdbcTemplate(
      @Qualifier("sqlDataSource") DataSource sqlDataSource) {
    return new NamedParameterJdbcTemplate(sqlDataSource);
  }
  
  @Bean(name = "jdbcTemplate")
  public JdbcTemplate npJdbcTemplate(
      @Qualifier("npJdbcTemplate") NamedParameterJdbcTemplate npJdbcTemplate) {
    return npJdbcTemplate.getJdbcTemplate();
  }
  
}
