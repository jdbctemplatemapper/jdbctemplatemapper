package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "    ")
public class BlankTableObject {
  @Id
  private Integer id;
}
