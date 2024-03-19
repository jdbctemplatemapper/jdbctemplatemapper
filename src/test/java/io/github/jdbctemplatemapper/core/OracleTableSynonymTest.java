package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.github.jdbctemplatemapper.model.Testsynonym;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class OracleTableSynonymTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired
  @Qualifier("allJdbcTemplateMapper")
  private JdbcTemplateMapper jtm;

  @Test
  public void oracleSynonymTable_test() {
    if (jdbcDriver.contains("oracle")) {

      // testsynonym table created in SCHEMA1 and synonym created in SCHEMA2 and accessing it in
      // SCHEMA2
      Testsynonym model = new Testsynonym();
      model.setName("abc");

      jtm.insert(model);
      assertNotNull(model.getId());

      model.setName("xyz");
      jtm.update(model);

      Testsynonym model2 = jtm.findById(Testsynonym.class, model.getId());
      assertEquals("xyz", model2.getName());

      List<Testsynonym> list = Query.type(Testsynonym.class).execute(jtm);

      assertTrue(list.size() > 0);

    }
  }

}
