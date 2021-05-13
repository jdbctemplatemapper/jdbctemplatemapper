package org.jdbctemplatemapper.model;
import lombok.Data;

@Data
public class Customer {
     private Integer id;
     private String firstName;
     private String lastName;
     
     private String version;
}
