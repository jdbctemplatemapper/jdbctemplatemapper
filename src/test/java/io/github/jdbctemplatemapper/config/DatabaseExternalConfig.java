package io.github.jdbctemplatemapper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configure database connection properties in an external file.
 * /opt/tomcat/conf/jdbctemplatemapper-database.properties
 *
 * The file will have entries like below:
 *
 * spring.datasource.jdbc-url=jdbc:postgresql://localhost:5432/postgres
 * spring.datasource.username=someusername
 * spring.datasource.password=somepassword
 * spring.datasource.driver-class-name=org.postgresql.Driver
 *
 * @author ajoseph
 */
@Configuration
@PropertySource(value= {"file:/opt/tomcat/conf/jdbctemplatemapper-database.properties"}, ignoreResourceNotFound=true)
public class DatabaseExternalConfig {
}
