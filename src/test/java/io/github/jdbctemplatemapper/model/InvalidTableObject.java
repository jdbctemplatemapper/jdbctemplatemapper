package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "invalid_table")
public class InvalidTableObject {
  @Id
  private Integer id;
}
