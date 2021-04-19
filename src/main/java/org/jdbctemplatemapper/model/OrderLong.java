package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderLong {
    private Long id;
    private LocalDateTime orderDate;
    private Long customerLongId;
    private String status;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    
    private Integer version;
    
    
    private CustomerLong customer;
    private List<OrderLineLong> orderLines;
	
}
