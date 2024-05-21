package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.model.ModelWithInheritedTableAnnotation;
import io.github.jdbctemplatemapper.model.OrderInheritedAudit;
import io.github.jdbctemplatemapper.model.OrderInheritedColumn;
import io.github.jdbctemplatemapper.model.OrderInheritedId;
import io.github.jdbctemplatemapper.model.OrderInheritedOverridenId;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnnotationInheritedTest {

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void annotationOrderInheritedAudit_Test() {

    TableMapping tableMapping = jtm.getTableMapping(OrderInheritedAudit.class);

    List<String> mappedProperties = Arrays.asList("orderId", "orderDate", "customerId", "status",
        "createdOn", "createdBy", "updatedOn", "updatedBy", "version");
    for (String propertyName : mappedProperties) {
      assertNotNull(tableMapping.getPropertyMappingByPropertyName(propertyName));
    }
  }

  @Test
  public void annotationOrderInheritedColumn_Test() {

    TableMapping tableMapping = jtm.getTableMapping(OrderInheritedColumn.class);

    List<String> mappedProperties = Arrays.asList("orderId", "orderDate", "customerId", "status");
    for (String propertyName : mappedProperties) {
      assertNotNull(tableMapping.getPropertyMappingByPropertyName(propertyName));
    }
  }

  @Test
  public void annotationOrderInheritedId_Test() {

    TableMapping tableMapping = jtm.getTableMapping(OrderInheritedId.class);

    List<String> mappedProperties = Arrays.asList("orderId", "orderDate", "customerId", "status");
    for (String propertyName : mappedProperties) {
      assertNotNull(tableMapping.getPropertyMappingByPropertyName(propertyName));
    }
  }

  @Test
  public void annotationOrderIdOverriden_failure_Test() {

    Exception exception = Assertions.assertThrows(AnnotationException.class, () -> {
      jtm.getTableMapping(OrderInheritedOverridenId.class);
    });
    assertTrue(exception.getMessage().contains("@Id annotation not found in class"));

  }

  @Test
  public void tableAnnotationInherited_Test() {
    Assertions.assertDoesNotThrow(
        () -> jtm.getTableMapping(ModelWithInheritedTableAnnotation.class));
  }

}
