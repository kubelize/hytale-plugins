package com.kubelize.securewarps.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.kubelize.securewarps.config.PermissionsConfig;
import com.kubelize.securewarps.config.ServerConfig;
import com.kubelize.securewarps.db.DatabaseManager;

public class WarpCommand extends AbstractCommandCollection {
  public WarpCommand(DatabaseManager databaseManager, ServerConfig serverConfig, PermissionsConfig permissions) {
    super("warp", "Warp commands");
    this.addSubCommand(new WarpSetCommand(databaseManager, serverConfig, permissions.getSet()));
    this.addSubCommand(new WarpGoCommand(databaseManager, serverConfig, permissions.getUse()));
    this.addSubCommand(new WarpListCommand(databaseManager, permissions.getList()));
    this.addSubCommand(new WarpDeleteCommand(databaseManager, permissions.getDelete()));
  }
}
