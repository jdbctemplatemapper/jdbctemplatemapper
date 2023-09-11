package io.github.jdbctemplatemapper.exception;

/**
 * updates with stale data will throw this exception when model property is annotated with
 * {@literal @}Version,
 */
public class OptimisticLockingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public OptimisticLockingException(String message) {
    super(message);
  }
}
