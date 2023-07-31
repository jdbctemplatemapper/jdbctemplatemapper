package org.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jdbctemplatemapper.model.Person;
import org.jdbctemplatemapper.model.Person2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
/**
 * Tests where the jdbcTemplateMapper does NOT have things like
 * createdOn, updatedOn, createdBy, updatedBy, version etc configured
 * @author ajoseph
 *
 */
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

    assertNotNull(person.getPersonId());
  }

  @Test
  public void update_Test() {
    Person person = jdbcTemplateMapper.findById(1, Person.class);
    person.setLastName("jordan");
    jdbcTemplateMapper.update(person);

    person = jdbcTemplateMapper.findById(1, Person.class); // requery
    assertEquals("jordan", person.getLastName());
  }
  
  @Test
  public void update_mapperReservedPropertiesTest() {
    Person2 person2 = jdbcTemplateMapper.findById(1, Person2.class);
    person2.setLastName("mike");
     person2.setUpdatedBy("xyz");
     person2.setCreatedBy("abc");
    jdbcTemplateMapper.update(person2);

    person2 = jdbcTemplateMapper.findById(1, Person2.class); // requery
    assertEquals("mike", person2.getLastName());
    assertEquals("xyz", person2.getUpdatedBy());
  }
}
