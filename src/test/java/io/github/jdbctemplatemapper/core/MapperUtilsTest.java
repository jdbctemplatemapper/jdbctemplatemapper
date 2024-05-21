package io.github.jdbctemplatemapper.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.lang.reflect.Field;
import java.util.List;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.github.jdbctemplatemapper.model.OrderInheritedAudit;
import io.github.jdbctemplatemapper.model.Person;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MapperUtilsTest {

  @Autowired
  private JdbcTemplateMapper jtm;

  @Test
  public void chunkList_test() {
    Integer[] arr = new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    List<List<?>> chunkedList = null;
    chunkedList = MapperUtils.chunkTheList(Arrays.asList(arr), 3);
    assertEquals(4, chunkedList.size());
    assertEquals(1, chunkedList.get(3).size());

    chunkedList = MapperUtils.chunkTheList(Arrays.asList(arr), 5);
    assertEquals(2, chunkedList.size());
    assertEquals(5, chunkedList.get(1).size());

    chunkedList = MapperUtils.chunkTheList(Arrays.asList(arr), 10);
    assertEquals(1, chunkedList.size());
    assertEquals(10, chunkedList.get(0).size());

    chunkedList = MapperUtils.chunkTheList(Arrays.asList(arr), 100);
    assertEquals(1, chunkedList.size());
    assertEquals(10, chunkedList.get(0).size());
  }

  @Test
  public void tableNameOnly_test() {
    String tableName = null;
    tableName = MapperUtils.getTableNameOnly(null);
    assertNull(tableName);

    tableName = MapperUtils.getTableNameOnly("aaa.sometable");
    assertEquals("sometable", tableName);

    tableName = MapperUtils.getTableNameOnly("usertable");
    assertEquals("usertable", tableName);

    tableName = MapperUtils.getTableNameOnly("aaa.bbb.employeetable");
    assertEquals("employeetable", tableName);

  }

  @Test
  public void fullyQualifiedTableNameForThroughJoinTable_test() {
    TableMapping tableMapping = jtm.getTableMapping(Person.class);
    String name = null;

    name = MapperUtils.getFullyQualifiedTableNameForThroughJoinTable("sometablename", tableMapping);
    assertEquals("schema1.sometablename", name.toLowerCase());

    name = MapperUtils.getFullyQualifiedTableNameForThroughJoinTable("aaa.anothertablename",
        tableMapping);
    assertEquals("aaa.anothertablename", name.toLowerCase());

  }

  @Test
  public void inheritedClass_fieldCount_test() {
    List<Field> fields = MapperUtils.getAllFields(OrderInheritedAudit.class);
    assertEquals(fields.size(), 9);
  }

}
