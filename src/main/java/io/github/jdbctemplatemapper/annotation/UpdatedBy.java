package io.github.jdbctemplatemapper.annotation;

import java.lang.annotation.ElementType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If IRecordOperatorResolver is implemented and configured with
 * JdbcTemplateMapper the value will be set to value returned by implementation
 * when the record is updated. Without configuration no values will be set. The
 * type returned should match the type of the property. 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UpdatedBy {

}
