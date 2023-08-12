
package io.github.jdbctemplatemapper.test;

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

	@Autowired
	private JdbcTemplateMapper jtm;
	
	
	@Test
	public void noTableAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, NoTableAnnotationModel.class);
		});
	}
	
	@Test
	public void invalidTable_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, InvalidTableObject.class);
		});
	}
	
	@Test
	public void blankTable_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, BlankTableObject.class);
		});
	}
	
	@Test
	public void noIdObject_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, NoIdObject.class);
		});
	}
	
	@Test
	public void noMatchingColumn_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, NoMatchingColumn.class);
		});
	}
	
	@Test
	public void noMatchingColumn2_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, NoMatchingColumn2.class);
		});
	}
	

	@Test
	public void duplicateIdAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateIdAnnotion.class);
		});
	}
	
	@Test
	public void duplicateVersionAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateVersionAnnotation.class);
		});
	}
	
	@Test
	public void duplicateCreatedOnAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateCreatedOnAnnotation.class);
		});
	}
	
	@Test
	public void duplicateCreatedByAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateCreatedByAnnotaition.class);
		});
	}
	
	@Test
	public void duplicateUpdatedOnAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateUpdatedOnAnnotation.class);
		});
	}
	
	@Test
	public void duplicateUpdatedByAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateUpdatedByAnnotation.class);
		});
	}
	
	@Test void conflictingAnnotations_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, ConflictAnnotation.class);
		});
	}
	
	@Test void conflictingAnnotations2_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, ConflictAnnotation2.class);
		});
	}
	
	@Test void conflictingAnnotations3_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, ConflictAnnotation3.class);
		});
	}
}
