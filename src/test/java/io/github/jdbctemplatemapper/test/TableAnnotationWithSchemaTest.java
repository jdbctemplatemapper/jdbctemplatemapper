package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.core.Query;
import io.github.jdbctemplatemapper.core.QueryCount;
import io.github.jdbctemplatemapper.core.QueryMerge;
import io.github.jdbctemplatemapper.model.CompanySchema2;
import io.github.jdbctemplatemapper.model.CustomerSchema1;
import io.github.jdbctemplatemapper.model.OfficeSchema2;
import io.github.jdbctemplatemapper.model.PersonSchema1;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableAnnotationWithSchemaTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  @Qualifier("allJdbcTemplateMapper")
  private JdbcTemplateMapper jtm;

  
  @Test
  public void autoId_crud_withSchema1Annotation() {
    if (!jdbcDriver.contains("mysql")) {
      CustomerSchema1 customer = new CustomerSchema1();
      customer.setFirstName("john");
      customer.setLastName("doe");

      jtm.insert(customer);

      customer.setLastName("xyz");
      jtm.update(customer);

      CustomerSchema1 customer1 =
          jtm.findById(CustomerSchema1.class, customer.getCustomerId());

      assertNotNull(customer1);
      assertEquals("xyz", customer1.getLastName());

      int count = jtm.delete(customer1);
      assertEquals(count, 1);
    }
  }
  
  @Test
  public void nonAutoId_crud_withSchema1Annotation() {
    if (!jdbcDriver.contains("mysql")) {
      PersonSchema1 person = new PersonSchema1();
      person.setPersonId("2001");
      person.setFirstName("john");
      person.setLastName("doe");

      jtm.insert(person);

      person.setLastName("xyz");
      jtm.update(person);

      PersonSchema1 person1 = jtm.findById(PersonSchema1.class, person.getPersonId());
      assertNotNull(person1);
      assertEquals("xyz", person1.getLastName());

      int count = jtm.delete(person1);
      assertEquals(count, 1);
    }
  }
  
  
  @Test
  public void query_withSchema1Annotation() {
    if (!jdbcDriver.contains("mysql")) {
      CustomerSchema1 customer = new CustomerSchema1();
      customer.setLastName("abc");

      jtm.insert(customer);

      List<CustomerSchema1> list = Query.type(CustomerSchema1.class)
          .where("customer_id = ?", customer.getCustomerId()).execute(jtm);

      assertEquals(1, list.size());

      jtm.delete(customer);
    }
  }
  
  @Test
  public void queryCount_withSchema1Annotation() {
    if (!jdbcDriver.contains("mysql")) {
      CustomerSchema1 customer = new CustomerSchema1();
      customer.setLastName("abc");

      jtm.insert(customer);

      Integer count = QueryCount.type(CustomerSchema1.class)
          .where("customer_id = ?", customer.getCustomerId()).execute(jtm);

      assertEquals(1, count);

      jtm.deleteById(CustomerSchema1.class, customer.getCustomerId());
    }
  }
  
  @Test
  public void query_WithCatalogSchema2Annotation() {
    if (!jdbcDriver.contains("mysql")) {
      CompanySchema2 company = new CompanySchema2();
      company.setName("abc");

      jtm.insert(company);

      List<CompanySchema2> list =
          Query.type(CompanySchema2.class).where("id = ?", company.getId()).execute(jtm);

      assertEquals(1, list.size());

      jtm.deleteById(CompanySchema2.class, company.getId());

    }
  }
  
  @Test
  public void queryCount_withSchema2Annotation() {
    if (!jdbcDriver.contains("mysql")) {
      CompanySchema2 company = new CompanySchema2();
      company.setName("abc");

      jtm.insert(company);

      Integer count = QueryCount.type(CompanySchema2.class).where("id = ?", company.getId())
          .execute(jtm);

      assertEquals(1, count);

      jtm.deleteById(CustomerSchema1.class, company.getId());
    }
  }
  
  
  @Test
  public void queryMerge_withCatalogSchema2Annotation() {
    if (!jdbcDriver.contains("mysql")) {

      CompanySchema2 company = new CompanySchema2();
      company.setName("abc");

      jtm.insert(company);

      OfficeSchema2 office = new OfficeSchema2();
      office.setAddress("street 1");
      office.setCompanyId(company.getId());

      jtm.insert(office);

      List<CompanySchema2> companies =
          Query.type(CompanySchema2.class).where("id = ?", company.getId()).execute(jtm);

      QueryMerge.type(CompanySchema2.class).hasMany(OfficeSchema2.class)
          .joinColumnManySide("company_id").populateProperty("offices").execute(jtm, companies);
      
    }
  }
  
}
