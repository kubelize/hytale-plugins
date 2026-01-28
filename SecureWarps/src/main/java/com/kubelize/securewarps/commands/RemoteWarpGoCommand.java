package com.kubelize.securewarps.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.config.ServerConfig;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import com.kubelize.securewarps.inventory.InventorySnapshotUtil;
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RemoteWarpGoCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;
  private final ServerConfig serverConfig;
  @Nonnull
  private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Warp name", (ArgumentType) ArgTypes.STRING);

  public RemoteWarpGoCommand(DatabaseManager databaseManager, ServerConfig serverConfig, String permission) {
    super("go", "Warp to a remote server");
    this.databaseManager = databaseManager;
    this.serverConfig = serverConfig;
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

    databaseManager.getWarpByName(name)
        .thenAccept(result -> GameThread.run(playerRef, () -> handleRemoteWarp(store, ref, playerRef, name, result)))
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to load warp " + name, ErrorUtil.rootCause(err));
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err) ? "Warp lookup timed out." : "Failed to load warp: " + name)));
          return null;
        });
  }

  private void handleRemoteWarp(Store<EntityStore> store,
                                Ref<EntityStore> ref,
                                PlayerRef playerRef,
                                String name,
                                Optional<WarpRecord> result) {
    if (result.isEmpty()) {
      playerRef.sendMessage(Message.raw("Warp not found: " + name));
      return;
    }

    WarpRecord warp = result.get();
    String host = warp.serverHost();
    Integer port = warp.serverPort();
    if (host == null || host.isBlank() || port == null || port <= 0) {
      playerRef.sendMessage(Message.raw("Warp has no remote server assigned: " + name));
      return;
    }

    String localHost = serverConfig.getHost();
    int localPort = serverConfig.getPort();
    if (localHost == null || localHost.isBlank() || localPort <= 0) {
      playerRef.sendMessage(Message.raw("Server address not configured for remote warps."));
      return;
    }

    if (host.equalsIgnoreCase(localHost) && port == localPort) {
      playerRef.sendMessage(Message.raw("Warp is local. Use /warp go " + name));
      return;
    }

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      playerRef.sendMessage(Message.raw("Unable to read inventory for transfer."));
      return;
    }

    databaseManager.saveInventory(player.getUuid(), InventorySnapshotUtil.encode(player.getInventory()))
        .thenRun(() -> GameThread.run(playerRef, () -> {
          try {
            byte[] payload = warp.name().getBytes(StandardCharsets.UTF_8);
            playerRef.referToServer(host, port, payload);
            playerRef.sendMessage(Message.raw("Transferring to " + host + ":" + port + "..."));
          } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Remote transfer failed: " + e.getMessage()));
          }
        }))
        .exceptionally(err -> {
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err)
              ? "Inventory save timed out. Transfer cancelled."
              : "Failed to save inventory. Transfer cancelled.")));
          return null;
        });
  }
}
