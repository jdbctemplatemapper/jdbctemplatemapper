package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.Version;

@Table(name = "annotation_check")
public class ConflictAnnotation {
	@Id(type = IdType.AUTO_INCREMENT)
	@Version
	private Integer id;

	@Column
	private String something;
}
