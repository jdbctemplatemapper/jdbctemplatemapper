package org.jdbctemplatemapper.core;

import lombok.Data;

@Data

/**
 * Maps a class to the sql select columsn starting with a specific prefix.
 * 
 * select o.id o_id, o.order_date o_order_date
 *        ol.id ol_id, ol.propduct_id ol_product_id, ol.quandity ol_quantity
 * from order o, order_line ol
 * where o.id = ol.order_id
 * 
 * using new SelectMapper(Order.class, "o_") will map the resultset values of select columns starting with
 *  'o_' to Order
 *  
 * using new SelectMapper(OrderLine.class, "ol_") will map the resultset values of select columns starting with
 *  'ol_' to OrderLine
 *  
 * @author ajoseph
 *
 * @param <T>
 */
public class SelectMapper<T> {
  private Class<T> clazz;
  private String sqlColumnPrefix;

  public SelectMapper(Class<T> clazz) {
    this.clazz = clazz;
    this.sqlColumnPrefix = "";
  }

  public SelectMapper(Class<T> clazz, String sqlColumnPrefix) {
    this.clazz = clazz;
    this.sqlColumnPrefix = sqlColumnPrefix;
  }
}
