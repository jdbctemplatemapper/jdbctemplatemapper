package io.github.jdbctemplatemapper.exception;

/**
 * When JdbcTemplateMapper is configured with a version property  
 * and the Object has that property, updates with stale data will throw this exception
 * 
 */
public class OptimisticLockingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public OptimisticLockingException(String message) {
    super(message);
  }
}
