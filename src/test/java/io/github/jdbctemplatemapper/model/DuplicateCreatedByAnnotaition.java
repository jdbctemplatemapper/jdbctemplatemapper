package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedBy;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "annotation_check")
public class DuplicateCreatedByAnnotaition {
	@Id(type = IdType.AUTO_INCREMENT)
	private Integer id;

	@CreatedBy
	private String createdBy1;

	@CreatedBy
	private String createdBy2;

	@Column
	private String something;
}
