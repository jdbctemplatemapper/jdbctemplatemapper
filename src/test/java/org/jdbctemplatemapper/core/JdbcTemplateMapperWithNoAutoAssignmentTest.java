package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jdbctemplatemapper.model.Person;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcTemplateMapperWithNoAutoAssignmentTest {

  private static JdbcTemplateMapper jdbcTemplateMapper;

  private static JdbcTemplate jdbcTemplate;

  @SuppressWarnings("all")
  @BeforeAll
  public static void setup() {
    PGSimpleDataSource source = new PGSimpleDataSource();
    source.setServerName("localhost");
    source.setDatabaseName("postgres");
    source.setUser("postgres");
    source.setPassword("pass123");
    source.setCurrentSchema("jdbctemplatemapper");

    jdbcTemplate = new JdbcTemplate(source);
    // configuration without any createdOn, createdBy, version etc
    jdbcTemplateMapper = new JdbcTemplateMapper(jdbcTemplate, "jdbctemplatemapper");
  }

  @Test
  public void insert_Test() {
    Person person = new Person();
    person.setLastName("Doe");
    person.setFirstName("Jane");

    jdbcTemplateMapper.insert(person);

    assertNotNull(person.getId());
  }

  @Test
  public void update_Test() {
    Person person = jdbcTemplateMapper.findById(1, Person.class);
    person.setLastName("jordan");
    jdbcTemplateMapper.update(person);

    person = jdbcTemplateMapper.findById(1, Person.class); // requery
    assertEquals("jordan", person.getLastName());
  }
}
