package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class OrderLong {
    private Long id;
    private LocalDateTime orderDate;
    private Long customerId;
    private String status;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    
    private Integer version;
	
}
