package org.jdbctemplatemapper.config;

import javax.sql.DataSource;

import org.jdbctemplatemapper.core.JdbcTemplateMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcTemplateMapperConfig {
	
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource sqlDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  @Primary
  public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    JdbcTemplateMapper jdbcTemplateMapper =
        new JdbcTemplateMapper(jdbcTemplate, "jdbctemplatemapper");
        //new JdbcTemplateMapper(jdbcTemplate, null);
    jdbcTemplateMapper
        .withRecordOperatorResolver(new RecordOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");
    
    return jdbcTemplateMapper;
  }
  
  @Bean
  @Qualifier("noConfigJdbcTemplateMapper")
  public JdbcTemplateMapper noConfigJdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
    JdbcTemplateMapper jdbcTemplateMapper =
        new JdbcTemplateMapper(jdbcTemplate, "jdbctemplatemapper");
        //new JdbcTemplateMapper(jdbcTemplate, null);
    return jdbcTemplateMapper;
  }
  
}
