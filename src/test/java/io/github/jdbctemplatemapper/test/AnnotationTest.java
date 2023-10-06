package io.github.jdbctemplatemapper.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.model.BlankTableObject;
import io.github.jdbctemplatemapper.model.ConflictAnnotation;
import io.github.jdbctemplatemapper.model.ConflictAnnotation2;
import io.github.jdbctemplatemapper.model.ConflictAnnotation3;
import io.github.jdbctemplatemapper.model.DuplicateCreatedByAnnotaition;
import io.github.jdbctemplatemapper.model.DuplicateCreatedOnAnnotation;
import io.github.jdbctemplatemapper.model.DuplicateIdAnnotion;
import io.github.jdbctemplatemapper.model.DuplicateUpdatedByAnnotation;
import io.github.jdbctemplatemapper.model.DuplicateUpdatedOnAnnotation;
import io.github.jdbctemplatemapper.model.DuplicateVersionAnnotation;
import io.github.jdbctemplatemapper.model.InvalidTableObject;
import io.github.jdbctemplatemapper.model.NoIdObject;
import io.github.jdbctemplatemapper.model.NoMatchingColumn;
import io.github.jdbctemplatemapper.model.NoMatchingColumn2;
import io.github.jdbctemplatemapper.model.NoTableAnnotationModel;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnnotationTest {

  @Value("${spring.datasource.driver-class-name}")
  private String jdbcDriver;

  @Autowired private JdbcTemplateMapper jtm;

  @Test
  public void noTableAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(NoTableAnnotationModel.class, 1);
            });
    assertTrue(exception.getMessage().contains("does not have the @Table annotation"));
  }

  @Test
  public void invalidTable_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(InvalidTableObject.class, 1);
            });
    assertTrue(exception.getMessage().contains("Unable to locate meta-data for table"));
  }

  @Test
  public void blankTable_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(BlankTableObject.class, 1);
            });
    assertTrue(exception.getMessage().contains("@Table annotation has a blank name"));
  }

  @Test
  public void noIdObject_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(NoIdObject.class, 1);
            });
    assertTrue(exception.getMessage().contains("@Id annotation not found"));
  }

  @Test
  public void noMatchingColumn_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(NoMatchingColumn.class, 1);
            });
    assertTrue(exception.getMessage().contains("column not found in table"));
  }

  @Test
  public void noMatchingColumn2_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(NoMatchingColumn2.class, 1);
            });
    assertTrue(exception.getMessage().contains("column not found in table"));
  }

  @Test
  public void duplicateIdAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(DuplicateIdAnnotion.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple @Id annotations"));
  }

  @Test
  public void duplicateVersionAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(DuplicateVersionAnnotation.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple @Version annotations"));
  }

  @Test
  public void duplicateCreatedOnAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(DuplicateCreatedOnAnnotation.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple @CreatedOn annotations"));
  }

  @Test
  public void duplicateCreatedByAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(DuplicateCreatedByAnnotaition.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple @CreatedBy annotations"));
  }

  @Test
  public void duplicateUpdatedOnAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(DuplicateUpdatedOnAnnotation.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple @UpdatedOn annotations"));
  }

  @Test
  public void duplicateUpdatedByAnnotation_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(DuplicateUpdatedByAnnotation.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple @UpdatedBy annotations"));
  }

  @Test
  void conflictingAnnotations_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(ConflictAnnotation.class, 1);
            });
    assertTrue(exception.getMessage().contains("id has multiple annotations that conflict"));
  }

  @Test
  void conflictingAnnotations2_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(ConflictAnnotation2.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple annotations that conflict"));
  }

  @Test
  void conflictingAnnotations3_Test() {
    Exception exception =
        Assertions.assertThrows(
            AnnotationException.class,
            () -> {
              jtm.findById(ConflictAnnotation3.class, 1);
            });
    assertTrue(exception.getMessage().contains("has multiple annotations that conflict"));
  }
}
