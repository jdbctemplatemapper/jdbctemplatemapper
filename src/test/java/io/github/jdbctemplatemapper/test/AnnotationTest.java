
package io.github.jdbctemplatemapper.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.jdbctemplatemapper.annotation.model.DuplicateCreatedBy;
import io.github.jdbctemplatemapper.annotation.model.DuplicateCreatedOn;
import io.github.jdbctemplatemapper.annotation.model.DuplicateId;
import io.github.jdbctemplatemapper.annotation.model.DuplicateUpdatedBy;
import io.github.jdbctemplatemapper.annotation.model.DuplicateUpdatedOn;
import io.github.jdbctemplatemapper.annotation.model.DuplicateVersion;
import io.github.jdbctemplatemapper.core.JdbcTemplateMapper;
import io.github.jdbctemplatemapper.exception.AnnotationException;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnnotationTest {

	@Value("${spring.datasource.driver-class-name}")
	private String jdbcDriver;

	@Autowired
	private JdbcTemplateMapper jtm;

	@Test
	public void duplicateIdAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateId.class);
		});
	}
	
	@Test
	public void duplicateVersionAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateVersion.class);
		});
	}
	
	@Test
	public void duplicateCreatedOnAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateCreatedOn.class);
		});
	}
	
	@Test
	public void duplicateCreatedByAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateCreatedBy.class);
		});
	}
	
	@Test
	public void duplicateUpdatedOnAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateUpdatedOn.class);
		});
	}
	
	@Test
	public void duplicateUpdatedByAnnotation_Test() {
		Assertions.assertThrows(AnnotationException.class, () -> {
			jtm.findById(1, DuplicateUpdatedBy.class);
		});
	}
	
	
}
