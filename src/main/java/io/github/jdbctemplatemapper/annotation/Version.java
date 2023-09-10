package io.github.jdbctemplatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used for optimistic locking. It has to be of type Integer. Will be set to 1
 * when record is created and will incremented on updates. If the version is stale an
 * OptimisticLockingException will be thrown.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Version {}
