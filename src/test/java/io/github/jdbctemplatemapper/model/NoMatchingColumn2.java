package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "product")
public class NoMatchingColumn2 {
  @Id private Integer productId;

  @Column(name = "abc")
  private Double cost;
}
