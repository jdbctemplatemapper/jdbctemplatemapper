package org.jdbctemplatemapper.model;

import org.jdbctemplatemapper.core.Table;

import lombok.Data;

@Data
@Table (name = "person")
public class Person {
	private Integer id;
	private String lastName;
	private String firstName;
}
