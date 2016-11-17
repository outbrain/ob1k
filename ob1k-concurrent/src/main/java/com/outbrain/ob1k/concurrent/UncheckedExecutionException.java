package com.outbrain.ob1k.concurrent;

/**
 * Unchecked version of {@link java.util.concurrent.ExecutionException}.
 * Can be thrown inside composition functions as wrapping checked exception.
 * In {@link ComposableFuture}, every handler that throws this exception will be un-wraped.
 *
 * @author marenzon
 */
public class UncheckedExecutionException extends RuntimeException {

  // As Exception implements Serializable
  private static final long serialVersionUID = 0;

  public UncheckedExecutionException(final Throwable cause) {
    super(cause);
  }

  public UncheckedExecutionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
