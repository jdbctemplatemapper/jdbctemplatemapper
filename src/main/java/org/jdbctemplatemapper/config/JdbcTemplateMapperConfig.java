package org.jdbctemplatemapper.config;

import javax.sql.DataSource;

import org.jdbctemplatemapper.core.JdbcTemplateMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JdbcTemplateMapperConfig {

  @Bean(name = "sqlDataSource")
  // get dataSource properties from application.properties
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource sqlDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "jdbcTemplateMapper")
  public JdbcTemplateMapper jdbcTemplateMapper(@Qualifier("sqlDataSource") DataSource dataSource) {
    JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(dataSource, "jdbctemplatemapper");
    jdbcTemplateMapper
        .withRecordOperatorResolver(new RecordOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");

    return jdbcTemplateMapper;
  }
}
