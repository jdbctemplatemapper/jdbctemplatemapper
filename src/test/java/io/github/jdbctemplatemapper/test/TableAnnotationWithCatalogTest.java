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
import io.github.jdbctemplatemapper.model.CompanyCatalogSchema2;
import io.github.jdbctemplatemapper.model.CustomerCatalogSchema1;
import io.github.jdbctemplatemapper.model.OfficeCatalogSchema2;
import io.github.jdbctemplatemapper.model.PersonCatalogSchema1;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableAnnotationWithCatalogTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  @Qualifier("allJdbcTemplateMapper")
  private JdbcTemplateMapper jtm;


  @Test
  public void autoId_crud_withCatalogSchema1Annotation() {
    if (jdbcDriver.contains("mysql")) {
      CustomerCatalogSchema1 customer = new CustomerCatalogSchema1();
      customer.setFirstName("john");
      customer.setLastName("doe");

      jtm.insert(customer);

      customer.setLastName("xyz");
      jtm.update(customer);

      CustomerCatalogSchema1 customer1 =
          jtm.findById(CustomerCatalogSchema1.class, customer.getCustomerId());

      assertNotNull(customer1);
      assertEquals("xyz", customer1.getLastName());

      int count = jtm.delete(customer1);
      assertEquals(count, 1);
    }
  }

  @Test
  public void nonAutoId_crud_withCatalogSchema1Annotation() {
    if (jdbcDriver.contains("mysql")) {
      PersonCatalogSchema1 person = new PersonCatalogSchema1();
      person.setPersonId("2001");
      person.setFirstName("john");
      person.setLastName("doe");

      jtm.insert(person);

      person.setLastName("xyz");
      jtm.update(person);

      PersonCatalogSchema1 person1 = jtm.findById(PersonCatalogSchema1.class, person.getPersonId());
      assertNotNull(person1);
      assertEquals("xyz", person1.getLastName());

      int count = jtm.delete(person1);
      assertEquals(count, 1);
    }
  }


  @Test
  public void query_withCatalogSchema1Annotation() {
    if (jdbcDriver.contains("mysql")) {
      CustomerCatalogSchema1 customer = new CustomerCatalogSchema1();
      customer.setLastName("abc");

      jtm.insert(customer);

      List<CustomerCatalogSchema1> list = Query.type(CustomerCatalogSchema1.class)
          .where("customer_id = ?", customer.getCustomerId()).execute(jtm);

      assertEquals(1, list.size());

      jtm.delete(customer);
    }
  }

  @Test
  public void queryCount_withCatalogSchema1Annotation() {
    if (jdbcDriver.contains("mysql")) {
      CustomerCatalogSchema1 customer = new CustomerCatalogSchema1();
      customer.setLastName("abc");

      jtm.insert(customer);

      Integer count = QueryCount.type(CustomerCatalogSchema1.class)
          .where("customer_id = ?", customer.getCustomerId()).execute(jtm);

      assertEquals(1, count);

      jtm.deleteById(CustomerCatalogSchema1.class, customer.getCustomerId());
    }
  }


  @Test
  public void query_WithCatalogSchema2Annotation() {
    if (jdbcDriver.contains("mysql")) {
      CompanyCatalogSchema2 company = new CompanyCatalogSchema2();
      company.setName("abc");

      jtm.insert(company);

      List<CompanyCatalogSchema2> list =
          Query.type(CompanyCatalogSchema2.class).where("id = ?", company.getId()).execute(jtm);

      assertEquals(1, list.size());

      jtm.deleteById(CompanyCatalogSchema2.class, company.getId());

    }
  }

  @Test
  public void queryCount_withCatalogSchema2Annotation() {
    if (jdbcDriver.contains("mysql")) {
      CompanyCatalogSchema2 company = new CompanyCatalogSchema2();
      company.setName("abc");

      jtm.insert(company);

      Integer count = QueryCount.type(CompanyCatalogSchema2.class).where("id = ?", company.getId())
          .execute(jtm);

      assertEquals(1, count);

      jtm.deleteById(CustomerCatalogSchema1.class, company.getId());
    }
  }

  @Test
  public void queryMerge_withCatalogSchema2Annotation() {
    if (jdbcDriver.contains("mysql")) {

      CompanyCatalogSchema2 company = new CompanyCatalogSchema2();
      company.setName("abc");

      jtm.insert(company);

      OfficeCatalogSchema2 office = new OfficeCatalogSchema2();
      office.setAddress("street 1");
      office.setCompanyId(company.getId());

      jtm.insert(office);

      List<CompanyCatalogSchema2> companies =
          Query.type(CompanyCatalogSchema2.class).where("id = ?", company.getId()).execute(jtm);

      QueryMerge.type(CompanyCatalogSchema2.class).hasMany(OfficeCatalogSchema2.class)
          .joinColumnManySide("company_id").populateProperty("offices").execute(jtm, companies);

    }
  }

}
