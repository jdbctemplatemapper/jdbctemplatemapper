package io.github.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.CreatedBy;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedBy;

@Table(name = "annotation_check")
public class ConflictAnnotation3 {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;

    @Column
    private String something;

    @CreatedBy
    @UpdatedBy
    private LocalDateTime createdBy1;
}
