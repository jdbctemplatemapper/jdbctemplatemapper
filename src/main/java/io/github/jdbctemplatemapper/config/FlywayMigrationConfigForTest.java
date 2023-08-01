package io.github.jdbctemplatemapper.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Every time the tests are run the database is reset (schema is dropped) and 
 * migration scripts run again so we have a fresh set of tables and data.
 * 
 * @author ajoseph
 *
 */


@Component
public class FlywayMigrationConfigForTest {

  @Bean
  public static FlywayMigrationStrategy cleanMigrateStrategy() {
    FlywayMigrationStrategy strategy =
        new FlywayMigrationStrategy() {
          @Override
          public void migrate(Flyway flyway) {
            flyway.clean();
            flyway.migrate();
          }
        };
    return strategy;
  }
}
