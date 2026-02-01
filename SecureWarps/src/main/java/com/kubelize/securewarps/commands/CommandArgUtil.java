package com.kubelize.securewarps.commands;

import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;

public final class CommandArgUtil {
  private CommandArgUtil() {
  }

  @SuppressWarnings("unchecked")
  public static <T> ArgumentType<T> typed(ArgumentType<?> type) {
    return (ArgumentType<T>) type;
  }
}
