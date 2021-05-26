package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jdbctemplatemapper.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class JdbcTemplateMapperWithNoAutoAssignmentTest {

  @Autowired
  @Qualifier("noConfigJdbcTemplateMapper")
  private JdbcTemplateMapper jdbcTemplateMapper;

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
