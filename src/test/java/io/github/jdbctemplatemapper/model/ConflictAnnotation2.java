package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedOn;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedOn;

@Table(name = "annotation_check")
public class ConflictAnnotation2 {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  @Column
  private String something;

  @CreatedOn
  @UpdatedOn
  private LocalDateTime createdOn1;
}
