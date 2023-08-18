package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;
import io.github.jdbctemplatemapper.annotation.UpdatedBy;

@Table(name = "annotation_check")
public class DuplicateUpdatedByAnnotation {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;

    @UpdatedBy
    private String updatedBy1;

    @UpdatedBy
    private String updatedBy2;

    @Column
    private String something;
}
