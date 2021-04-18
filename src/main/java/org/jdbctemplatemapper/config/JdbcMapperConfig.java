package org.jdbctemplatemapper.config;

import org.jdbctemplatemapper.core.JdbcMapper;
import org.jdbctemplatemapper.core.RecordOperatorResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcMapperConfig {

  @Bean(name = "jdbcMapper")
  public JdbcMapper jdbcMapper(@Qualifier("npJdbcTemplate") NamedParameterJdbcTemplate npJdbcTemplate) {
    JdbcMapper jdbcMapper = new JdbcMapper(npJdbcTemplate);
    jdbcMapper
        .withSchemaName("jdbctemplatemapper")
        .withRecordOperatorResolver(new RecordOperatorResolver())
        .withCreatedOnPropertyName("createdOn")
        .withCreatedByPropertyName("createdBy")
        .withUpdatedOnPropertyName("updatedOn")
        .withUpdatedByPropertyName("updatedBy")
        .withVersionPropertyName("version");

    return jdbcMapper;
  }
}
