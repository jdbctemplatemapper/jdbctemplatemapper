package io.github.jdbctemplatemapper.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * Annotation for Identifier
 * 
 * For auto increment id:
 * class Product {
 *  {@literal @}Id(type=IdType.AUTO_INCREMENT)
 *   private Integer productId;
 *   ...
 * }
 * After a successful insert() operation the productId property will be populated with the new id.
 * 
 * For NON auto increment id:
 * class Customer {
 *  {@literal @}Id
 *   private Integer id;
 *   ...
 * }
 * In this case you will have to manually set the id value before calling insert()
 * </pre>
 *
 * @author ajoseph
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
  IdType type() default IdType.MANUAL;
}
