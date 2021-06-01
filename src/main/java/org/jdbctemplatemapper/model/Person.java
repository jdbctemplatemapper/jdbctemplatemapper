package org.jdbctemplatemapper.model;

import lombok.Data;

@Data
public class Person {
	private Integer id;
	private String lastName;
	private String firstName;
	
    private String someNonDatabaseProperty;

}
