package io.github.jdbctemplatemapper.model;

import io.github.jdbctemplatemapper.annotation.Column;
import io.github.jdbctemplatemapper.annotation.Id;
import io.github.jdbctemplatemapper.annotation.Table;

@Table(name = "person")
public class Person {
	@Id
	private String personId;
	@Column
	private String lastName;
	@Column
	private String firstName;

	private String someNonDatabaseProperty;

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String id) {
		this.personId = id;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getSomeNonDatabaseProperty() {
		return someNonDatabaseProperty;
	}

	public void setSomeNonDatabaseProperty(String someNonDatabaseProperty) {
		this.someNonDatabaseProperty = someNonDatabaseProperty;
	}
}
