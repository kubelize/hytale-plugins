package com.kubelize.securewarps.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import java.util.List;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WarpListCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;

  public WarpListCommand(DatabaseManager databaseManager, String permission) {
    super("list", "List available warps");
    this.databaseManager = databaseManager;
    this.requirePermission(permission);
  }

  @Override
  protected void execute(@NonNullDecl CommandContext commandContext,
                         @NonNullDecl Store<EntityStore> store,
                         @NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl PlayerRef playerRef,
                         @NonNullDecl World world) {
    databaseManager.listWarps()
        .thenAccept(warps -> sendList(playerRef, warps))
        .exceptionally(err -> {
          playerRef.sendMessage(Message.raw("Failed to list warps."));
          return null;
        });
  }

  private void sendList(PlayerRef playerRef, List<WarpRecord> warps) {
    if (warps.isEmpty()) {
      playerRef.sendMessage(Message.raw("No warps found."));
      return;
    }

    playerRef.sendMessage(Message.raw("=== Warps ==="));
    for (WarpRecord warp : warps) {
      playerRef.sendMessage(Message.raw("- " + warp.name() + " [" + warp.worldId() + "]"));
    }
    playerRef.sendMessage(Message.raw("Total: " + warps.size()));
  }
}
