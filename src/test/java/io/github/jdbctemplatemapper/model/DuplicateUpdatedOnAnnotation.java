package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedOn;

@Table(name = "annotation_check")
public class DuplicateUpdatedOnAnnotation {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  @UpdatedOn private LocalDateTime updatedOn1;

  @UpdatedOn private LocalDateTime updatedOn2;

  @Column private String something;
}
