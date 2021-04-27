package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProductLong {
  private Long id;
  private String name;
  private Double cost;
  private LocalDateTime createdOn;
  private String createdBy;
  private LocalDateTime updatedOn;
  private String updatedBy;
  private Integer version;
}
