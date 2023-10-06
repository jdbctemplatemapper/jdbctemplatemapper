package io.github.jdbctemplatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When record is created the property will be set to LocalDateTime.now(). It has to be of type
 * LocalDateTime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CreatedOn {
}
