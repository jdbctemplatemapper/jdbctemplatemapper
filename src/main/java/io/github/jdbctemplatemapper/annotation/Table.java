package io.github.jdbctemplatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to map an object to a database table
 *
 * <pre>
 * This is a class level annotation and is required for models.
 * It can be any name and should match a table in the database
 *
 * {@literal @}Table(name="products")
 *  public class Product{
 *    .....
 *  }
 *
 * </pre>
 *
 * @author ajoseph
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
  String name();
}
