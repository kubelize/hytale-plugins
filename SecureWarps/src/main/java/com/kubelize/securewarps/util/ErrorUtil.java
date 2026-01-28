package com.kubelize.securewarps.util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public final class ErrorUtil {
  private ErrorUtil() {}

  public static Throwable rootCause(Throwable error) {
    Throwable current = error;
    while (current instanceof CompletionException && current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }

  public static boolean isTimeout(Throwable error) {
    Throwable root = rootCause(error);
    return root instanceof TimeoutException;
  }
}
