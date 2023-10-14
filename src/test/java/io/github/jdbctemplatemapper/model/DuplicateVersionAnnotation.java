package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.Version;

@Table(name = "annotation_check")
public class DuplicateVersionAnnotation {
  @Id(type = IdType.AUTO_INCREMENT)
  private Integer id;

  @Version
  private Integer version1;

  @Version
  private Integer version2;

  @Column
  private String something;
}
