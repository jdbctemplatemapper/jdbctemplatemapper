package org.jdbctemplatemapper.model;

import lombok.Data;
@Data
public class OrderLine {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private Integer numOfUnits;
    
    /**************************************/
    private Order order;
    private Product product;
    
    private String status;
}
