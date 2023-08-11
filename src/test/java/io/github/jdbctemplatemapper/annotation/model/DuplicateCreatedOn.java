package io.github.jdbctemplatemapper.annotation.model;

import java.time.LocalDateTime;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedOn;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "annotation_check")
public class DuplicateCreatedOn {
	@Id(type = IdType.AUTO_INCREMENT)
	private Integer id;

	@CreatedOn
	private LocalDateTime createdOn1;

	@CreatedOn
	private LocalDateTime createdOn2;

	@Column
	private String something;
}
