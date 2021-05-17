package org.jdbctemplatemapper.core;

/**
 * When JdbcTemplateMapper is configured with a property which is used for versioning and the Object
 * has that property, updates with stale data will throw this exception
 *
 * @author ajoseph
 */
public class OptimisticLockingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public OptimisticLockingException(String message) {
    super(message);
  }
}
