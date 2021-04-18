package org.jdbctemplatemapper.core;

public class OptimisticLockingException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public OptimisticLockingException(String message) {
    super(message);
  }

  public OptimisticLockingException(Throwable e) {
    super(e);
  }
}
