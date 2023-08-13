
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
			jtm.findById(NoTableAnnotationModel.class, 1);
		});
	}
	
	@Test
	public void invalidTable_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(InvalidTableObject.class,1);
		});
	}
	
	@Test
	public void blankTable_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(BlankTableObject.class, 1 );
		});
	}
	
	@Test
	public void noIdObject_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(NoIdObject.class,1);
		});
	}
	
	@Test
	public void noMatchingColumn_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(NoMatchingColumn.class,1);
		});
	}
	
	@Test
	public void noMatchingColumn2_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(NoMatchingColumn2.class,1);
		});
	}
	

	@Test
	public void duplicateIdAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(DuplicateIdAnnotion.class,1 );
		});
	}
	
	@Test
	public void duplicateVersionAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(DuplicateVersionAnnotation.class,1 );
		});
	}
	
	@Test
	public void duplicateCreatedOnAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(DuplicateCreatedOnAnnotation.class,1);
		});
	}
	
	@Test
	public void duplicateCreatedByAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(DuplicateCreatedByAnnotaition.class,1);
		});
	}
	
	@Test
	public void duplicateUpdatedOnAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(DuplicateUpdatedOnAnnotation.class,1 );
		});
	}
	
	@Test
	public void duplicateUpdatedByAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(DuplicateUpdatedByAnnotation.class,1);
		});
	}
	
	@Test void conflictingAnnotations_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(ConflictAnnotation.class,1);
		});
	}
	
	@Test void conflictingAnnotations2_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(ConflictAnnotation2.class,1);
		});
	}
	
	@Test void conflictingAnnotations3_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(ConflictAnnotation3.class,1);
		});
	}
}
