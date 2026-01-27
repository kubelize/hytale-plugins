package com.kubelize.securewarps.commands.inventory;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.kubelize.securewarps.config.PermissionsConfig;
import com.kubelize.securewarps.db.DatabaseManager;

public class InventoryAdminCommand extends AbstractCommandCollection {
  public InventoryAdminCommand(DatabaseManager databaseManager, PermissionsConfig permissions) {
    super("invdb", "Inventory database admin commands");
    this.requirePermission(permissions.getAdmin());
    this.addSubCommand(new InventoryDeleteCommand(databaseManager, permissions.getAdmin()));
  }
}
