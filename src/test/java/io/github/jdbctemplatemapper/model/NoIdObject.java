package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "no_id_object")
public class NoIdObject {
    @Column
    private String something;
}
