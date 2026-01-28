package com.kubelize.securewarps.commands.inventory;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class InventoryDeleteCommand extends AbstractCommand {
  private final DatabaseManager databaseManager;
  @Nonnull
  private final RequiredArg<UUID> uuidArg = this.withRequiredArg("uuid", "Player UUID", (ArgumentType) ArgTypes.PLAYER_UUID);

  public InventoryDeleteCommand(DatabaseManager databaseManager, String permission) {
    super("delete", "Delete a player's inventory snapshot");
    this.databaseManager = databaseManager;
    this.requirePermission(permission);
  }

  @Override
  protected CompletableFuture<Void> execute(@Nonnull CommandContext commandContext) {
    UUID uuid = uuidArg.get(commandContext);
    if (uuid == null) {
      commandContext.sendMessage(Message.raw("UUID is required."));
      return CompletableFuture.completedFuture(null);
    }

    return databaseManager.deleteInventory(uuid)
        .thenAccept(deleted -> GameThread.run(commandContext, () -> commandContext.sendMessage(Message.raw(deleted ? "Inventory deleted." : "No inventory found."))))
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to delete inventory for " + uuid, ErrorUtil.rootCause(err));
          GameThread.run(commandContext, () -> commandContext.sendMessage(Message.raw(ErrorUtil.isTimeout(err) ? "Inventory delete timed out." : "Failed to delete inventory.")));
          return null;
        });
  }
}
