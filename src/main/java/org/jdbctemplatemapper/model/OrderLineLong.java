package org.jdbctemplatemapper.model;

import lombok.Data;

@Data
public class OrderLineLong {
    private Long id;
    private Long orderId;
    private Long productId;
    private Integer numOfUnits;
    
    /**************************************/
    private Order order;
    private Product product;
	
	
}
