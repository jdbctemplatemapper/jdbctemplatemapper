package io.github.jdbctemplatemapper.config;

import javax.sql.DataSource;

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

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource sqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplateMapper jdbcTemplateMapper(JdbcTemplate jdbcTemplate) {
        String schemaName = getSchemaName();
        JdbcTemplateMapper jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate, schemaName);
        jdbcTemplateMapper.withRecordOperatorResolver(new RecordOperatorResolver());
        
        return jdbcTemplateMapper;
    }

    private String getSchemaName() {
        // Test databases setup
        String schemaName = "jdbctemplatemapper"; // default
        if (jdbcDriver.contains("mysql")) {
            schemaName = null; // mysql does not have schema.
        } else if (jdbcDriver.contains("oracle")) {
            schemaName = "JDBCTEMPLATEMAPPER"; // oracle has upper case
        }
        return schemaName;
    }
}
