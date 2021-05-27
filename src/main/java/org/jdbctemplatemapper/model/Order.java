package org.jdbctemplatemapper.model;

import java.time.LocalDateTime;
import java.util.List;

import org.jdbctemplatemapper.core.Table;

import lombok.Data;
@Data
@Table(name = "orders")
public class Order {
    private Integer id;
    private LocalDateTime orderDate;
    private Integer customerId;
    private String status;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
    
    private Integer version;
    
    
    /*****************************/
    
    private Customer customer;
    private List<OrderLine> orderLines;
    
    
    
}
