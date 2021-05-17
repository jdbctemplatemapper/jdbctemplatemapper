package org.jdbctemplatemapper.core;

import lombok.Data;

/**
 * Maps sql select columns starting with a specific prefix to an object
 * 
 * @author ajoseph
 */
@Data
public class SelectMapper<T> {
  private Class<T> clazz;
  private String sqlColumnPrefix;
/**
 * <pre>
 * For query below:
 * select o.id o_id, o.order_date o_order_date ol.id 
 *        ol_id, ol.propduct_id ol_product_id, ol.quandity ol_quantity 
 * from order o
 * left join order_line ol on o.id = ol.order_id;
 *
 * new SelectMapper(Order.class, "o_") will map the resultset values of select columns
 * starting with 'o_' to Order
 *
 * new SelectMapper(OrderLine.class, "ol_") will map the resultset values of select columns
 * starting with 'ol_' to OrderLine
 * 
 * </pre>
 * 
 * @param clazz - The class of the instance that needs to be populated
 * @param sqlColumnPrefix - the sql column prefix that need to be mapped to the object
 */
  public SelectMapper(Class<T> clazz, String sqlColumnPrefix) {
    if (clazz == null || sqlColumnPrefix == null) {
      throw new IllegalArgumentException("The arguments cannot be null");
    }
    this.clazz = clazz;
    this.sqlColumnPrefix = sqlColumnPrefix;
  }
}
