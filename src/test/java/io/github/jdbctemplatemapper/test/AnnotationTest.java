
package io.github.jdbctemplatemapper.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.annotation.model.ConflictAnnotation;
import io.github.jdbctemplatemapper.annotation.model.ConflictAnnotation2;
import io.github.jdbctemplatemapper.annotation.model.ConflictAnnotation3;
import io.github.jdbctemplatemapper.annotation.model.DuplicateCreatedByAnnotaition;
import io.github.jdbctemplatemapper.annotation.model.DuplicateCreatedOnAnnotation;
import io.github.jdbctemplatemapper.annotation.model.DuplicateIdAnnotion;
import io.github.jdbctemplatemapper.annotation.model.DuplicateUpdatedByAnnotation;
import io.github.jdbctemplatemapper.annotation.model.DuplicateUpdatedOnAnnotation;
import io.github.jdbctemplatemapper.annotation.model.DuplicateVersionAnnotation;
import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.exception.AnnotationException;
import io.github.jdbctemplatemapper.model.NoIdObject;
import io.github.jdbctemplatemapper.model.NoTableObject;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnnotationTest {

	@Value("${spring.datasource.driver-class-name}")
	private String jdbcDriver;

	@Autowired
	private JdbcTemplateMapper jtm;
	
	@Test
	public void insert_noIdObjectFailureTest() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			NoIdObject pojo = new NoIdObject();
			pojo.setSomething("abc");
			jtm.insert(pojo);
		});
	}
	

	@Test
	public void update_noTableObjectFailureTest() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			NoTableObject pojo = new NoTableObject();
			pojo.setSomething("abc");
			jtm.update(pojo);
		});
	}
	
	@Test
	public void delete_noIdObjectFailureTest() {
		Assertions.assertThrows(RuntimeException.class, () -> {
			NoIdObject pojo = new NoIdObject();
			pojo.setSomething("abc");
			jtm.delete(pojo);
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
