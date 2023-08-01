package org.jdbctemplatemapper.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *<pre>
 * Annotation to map an object to a database table:
 * 
 * {@literal @}Table(name="products")
 *  public class Product{
 *    .....
 *  }
 *
 * Above will map the Product class to the 'products' table
 *</pre>
 *
 * @author ajoseph
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
    String name();
}
