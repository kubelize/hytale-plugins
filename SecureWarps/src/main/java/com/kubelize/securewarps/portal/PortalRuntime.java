package com.kubelize.securewarps.portal;

import com.hypixel.hytale.logger.HytaleLogger;
import com.kubelize.securewarps.config.PermissionsConfig;
import com.kubelize.securewarps.config.ServerConfig;
import com.kubelize.securewarps.db.DatabaseManager;

public final class PortalRuntime {
  private static DatabaseManager databaseManager;
  private static ServerConfig serverConfig;
  private static PermissionsConfig permissions;
  private static HytaleLogger logger;

  private PortalRuntime() {
  }

  public static void init(DatabaseManager databaseManager,
                          ServerConfig serverConfig,
                          PermissionsConfig permissions,
                          HytaleLogger logger) {
    PortalRuntime.databaseManager = databaseManager;
    PortalRuntime.serverConfig = serverConfig;
    PortalRuntime.permissions = permissions;
    PortalRuntime.logger = logger;
  }

  public static DatabaseManager database() {
    if (databaseManager == null) {
      throw new IllegalStateException("PortalRuntime not initialized");
    }
    return databaseManager;
  }

  public static ServerConfig serverConfig() {
    if (serverConfig == null) {
      throw new IllegalStateException("PortalRuntime not initialized");
    }
    return serverConfig;
  }

  public static PermissionsConfig permissions() {
    if (permissions == null) {
      throw new IllegalStateException("PortalRuntime not initialized");
    }
    return permissions;
  }

  public static HytaleLogger logger() {
    if (logger == null) {
      throw new IllegalStateException("PortalRuntime not initialized");
    }
    return logger;
  }
}
