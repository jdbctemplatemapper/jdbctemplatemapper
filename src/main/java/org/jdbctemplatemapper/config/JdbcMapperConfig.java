package org.jdbctemplatemapper.config;

import javax.sql.DataSource;

import org.jdbctemplatemapper.core.JdbcMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JdbcMapperConfig {

  @Bean(name = "sqlDataSource")
  // get dataSource properties from application.properties
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource sqlDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "jdbcMapper")
  public JdbcMapper jdbcMapper(@Qualifier("sqlDataSource") DataSource dataSource) {
    JdbcMapper jdbcMapper = new JdbcMapper(dataSource, "jdbctemplatemapper");
    jdbcMapper
        .withRecordOperatorResolver(new RecordOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");

    return jdbcMapper;
  }
}
