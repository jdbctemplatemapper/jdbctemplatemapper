package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.IdType;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "annotation_check")
public class DuplicateIdAnnotion {
    @Id(type = IdType.AUTO_INCREMENT)
    private Integer id;

    @Id
    private Integer id2;

    @Column
    private String something;
}