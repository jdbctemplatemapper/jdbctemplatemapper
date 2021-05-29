package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import org.jdbctemplatemapper.core.Table;

import lombok.Data;

@Data
@Table(name="person")
public class Person2 {
	private Integer id;
	private String lastName;
	private String firstName;
	
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    
    private Integer version;
	
}
