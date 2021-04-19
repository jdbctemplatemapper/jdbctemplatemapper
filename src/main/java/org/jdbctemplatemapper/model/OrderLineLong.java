package org.jdbctemplatemapper.model;

import lombok.Data;

@Data
public class OrderLineLong {
    private Long id;
    private Long orderLongId;
    private Long productLongId;
    private Integer numOfUnits;
    
    /**************************************/
    private OrderLong order;
    private ProductLong product;
	
	
}
