package com.kubelize.securewarps.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.kubelize.securewarps.config.PermissionsConfig;
import com.kubelize.securewarps.config.ServerConfig;
import com.kubelize.securewarps.db.DatabaseManager;

public class RemoteWarpCommand extends AbstractCommandCollection {
  public RemoteWarpCommand(DatabaseManager databaseManager, ServerConfig serverConfig, PermissionsConfig permissions) {
    super("rwarp", "Remote warp commands");
    this.addSubCommand(new RemoteWarpGoCommand(databaseManager, serverConfig, permissions.getUse()));
    this.addSubCommand(new RemoteWarpSetCommand(databaseManager, permissions.getAdmin()));
  }
}
