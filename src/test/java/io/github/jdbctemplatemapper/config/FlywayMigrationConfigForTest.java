package io.github.jdbctemplatemapper.config;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Every time the tests are run the database is reset (schema is dropped) and migration scripts run
 * again so we have a fresh set of tables and data.
 *
 * @author ajoseph
 */
@Component
public class FlywayMigrationConfigForTest {

  @Value("${all.spring.flyway.locations}")
  private String locations;

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  @Qualifier("dsAll")
  private DataSource dsAll;

  @Bean
  public static FlywayMigrationStrategy cleanMigrateStrategy() {
    FlywayMigrationStrategy strategy = new FlywayMigrationStrategy() {
      @Override
      public void migrate(Flyway flyway) {
        flyway.clean();
        flyway.migrate();
      }
    };
    return strategy;
  }


  @PostConstruct
  public void flyWayDsAll() {
    if (jdbcDriver.contains("postgres")) {
      Flyway.configure().dataSource(dsAll).defaultSchema("schema2").locations(locations).load()
          .migrate();

    } else {
      Flyway.configure().dataSource(dsAll).locations(locations).load().migrate();
    }

  }


}
