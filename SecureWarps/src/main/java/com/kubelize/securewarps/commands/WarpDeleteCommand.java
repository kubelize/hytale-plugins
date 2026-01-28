package com.kubelize.securewarps.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WarpDeleteCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;
  @Nonnull
  private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Warp name", (ArgumentType) ArgTypes.STRING);

  public WarpDeleteCommand(DatabaseManager databaseManager, String permission) {
    super("delete", "Delete a warp");
    this.databaseManager = databaseManager;
    this.requirePermission(permission);
  }

  @Override
  protected void execute(@NonNullDecl CommandContext commandContext,
                         @NonNullDecl Store<EntityStore> store,
                         @NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl PlayerRef playerRef,
                         @NonNullDecl World world) {
    String name = nameArg.get(commandContext);
    if (name == null || name.isBlank()) {
      playerRef.sendMessage(Message.raw("Warp name is required."));
      return;
    }

    databaseManager.deleteWarpByName(name)
        .thenAccept(deleted -> GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(deleted ? "Warp deleted: " + name : "Warp not found: " + name))))
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to delete warp " + name, ErrorUtil.rootCause(err));
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err) ? "Warp delete timed out." : "Failed to delete warp: " + name)));
          return null;
        });
  }
}
