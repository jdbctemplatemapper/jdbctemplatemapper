package io.github.jdbctemplatemapper.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;

@Component
public class JdbcTemplateMapperConfig {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Primary
  @Bean(name = "ds1")
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource dataSourceDs1() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "ds1JdbcTemplate")
  public JdbcTemplate jdbcTemplateDs1(@Qualifier("ds1") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Primary
  @Bean(name = "ds1JdbcTemplateMapper")
  public JdbcTemplateMapper ds1JdbcTemplateMapper(
      @Qualifier("ds1JdbcTemplate") JdbcTemplate jdbcTemplate) {

    JdbcTemplateMapper jdbcTemplateMapper = null;
    if (jdbcDriver.contains("mysql")) {
      jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate, null, "schema1"); // schema1
                                                                                  // database name
                                                                                  // for mysql
    } else {
      jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate, "schema1");
    }

    return jdbcTemplateMapper.withRecordOperatorResolver(new RecordOperatorResolver());
  }

  @Bean(name = "dsAll")
  @ConfigurationProperties(prefix = "all.spring.datasource")
  public DataSource dataSourceAll() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "allJdbcTemplate")
  public JdbcTemplate jdbcTemplateAll(@Qualifier("dsAll") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  @Qualifier("allJdbcTemplateMapper")
  public JdbcTemplateMapper allJdbcTemplateMapperAll(
      @Qualifier("allJdbcTemplate") JdbcTemplate jdbcTemplate) {
    JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate);

    if (jdbcDriver.contains("oracle")) {
      // Need this to get meta-data of table names which are synonyms
      jdbcTemplateMapper.includeSynonymsForTableColumnMetaData();
    }

    return jdbcTemplateMapper.withRecordOperatorResolver(new RecordOperatorResolver());
  }
}
