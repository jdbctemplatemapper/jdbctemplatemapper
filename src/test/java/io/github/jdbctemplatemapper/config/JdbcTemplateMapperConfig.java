package io.github.jdbctemplatemapper.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

@Component
public class JdbcTemplateMapperConfig {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource sqlDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    
    JdbcTemplateMapper jdbcTemplateMapper = null;
    if (jdbcDriver.contains("mysql")) {
       jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate, null, "jdbctemplatemapper");
    }
    else {
      String schemaName = getSchemaName();
      jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate, schemaName);
    }
    
    return jdbcTemplateMapper.withRecordOperatorResolver(new RecordOperatorResolver());
  }

  private String getSchemaName() {
    // Test databases setup
    String schemaName = "jdbctemplatemapper"; // default
    if (jdbcDriver.contains("mysql")) {
      schemaName = null; // no schemas for my sql
    } else if (jdbcDriver.contains("oracle")) {
      schemaName = "JDBCTEMPLATEMAPPER"; // oracle has upper case
    }
    return schemaName;
  }
}
