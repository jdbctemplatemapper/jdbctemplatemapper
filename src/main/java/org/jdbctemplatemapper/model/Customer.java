package org.jdbctemplatemapper.model;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Customer {
     private Integer id;
     private String firstName;
     private String lastName;
     
     // Non table fields below to test out case were database table 'customer' 
     // does NOT have these auto assigned fields.
     private LocalDateTime createdOn;
     private String createdBy;
     private LocalDateTime updatedOn;
     private String updatedBy;
     private String version;
}
