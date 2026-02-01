package com.kubelize.securewarps.portal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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
import java.util.logging.Level;
import java.util.logging.Logger;

final class PortalWarpService {
  private final DatabaseManager databaseManager;
  private final ServerConfig serverConfig;

  PortalWarpService(DatabaseManager databaseManager, ServerConfig serverConfig) {
    this.databaseManager = databaseManager;
    this.serverConfig = serverConfig;
  }

  void teleport(Store<EntityStore> store,
                Ref<EntityStore> ref,
                PlayerRef playerRef,
                String warpName) {
    databaseManager.getWarpByName(warpName)
        .thenAccept(result -> {
          if (playerRef == null || !playerRef.isValid()) {
            return;
          }
          GameThread.run(playerRef, () -> handleTeleport(store, ref, playerRef, warpName, result));
        })
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to load warp " + warpName, ErrorUtil.rootCause(err));
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err)
              ? "Warp lookup timed out."
              : "Failed to load warp: " + warpName)));
          return null;
        });
  }

  private void handleTeleport(Store<EntityStore> store,
                              Ref<EntityStore> ref,
                              PlayerRef playerRef,
                              String name,
                              Optional<WarpRecord> result) {
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    if (result.isEmpty()) {
      playerRef.sendMessage(Message.raw("Warp not found or world unavailable: " + name));
      return;
    }

    WarpRecord warp = result.get();
    if (isRemoteWarp(warp)) {
      Player live = playerRef.getComponent(Player.getComponentType());
      if (live == null) {
        playerRef.sendMessage(Message.raw("Player unavailable for transfer."));
        return;
      }
      handleRemoteTransfer(playerRef, live, warp);
      return;
    }

    World targetWorld = Universe.get().getWorld(warp.worldId());
    if (targetWorld == null) {
      if (!Universe.get().isWorldLoadable(warp.worldId())) {
        playerRef.sendMessage(Message.raw("Warp world not loadable: " + warp.worldId()));
        return;
      }
      Universe.get().loadWorld(warp.worldId())
          .thenRun(() -> GameThread.run(playerRef, () -> teleportToLoadedWorld(store, ref, playerRef, name, warp)))
          .exceptionally(err -> {
            GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw("Failed to load warp world: " + warp.worldId())));
            return null;
          });
      return;
    }

    applyTeleport(store, ref, playerRef, name, warp);
  }

  private void teleportToLoadedWorld(Store<EntityStore> store,
                                     Ref<EntityStore> ref,
                                     PlayerRef playerRef,
                                     String name,
                                     WarpRecord warp) {
    World targetWorld = Universe.get().getWorld(warp.worldId());
    if (targetWorld == null) {
      playerRef.sendMessage(Message.raw("Warp world not loaded: " + warp.worldId()));
      return;
    }
    applyTeleport(store, ref, playerRef, name, warp);
  }

  private void applyTeleport(Store<EntityStore> store,
                             Ref<EntityStore> ref,
                             PlayerRef playerRef,
                             String name,
                             WarpRecord warp) {
    Vector3d position = new Vector3d(warp.x(), warp.y(), warp.z());
    Vector3f rotation = new Vector3f(warp.rotX(), warp.rotY(), warp.rotZ());
    Teleport teleport = Teleport.createForPlayer(Universe.get().getWorld(warp.worldId()), position, rotation);
    store.putComponent(ref, Teleport.getComponentType(), teleport);
    playerRef.sendMessage(Message.raw("Teleported to warp: " + name));
  }

  private void handleRemoteTransfer(PlayerRef playerRef, Player player, WarpRecord warp) {
    String host = warp.serverHost();
    Integer port = warp.serverPort();
    if (host == null || host.isBlank() || port == null || port <= 0) {
      playerRef.sendMessage(Message.raw("Warp has no remote server assigned: " + warp.name()));
      return;
    }

    if (!RemoteTransferGuard.tryMark(playerRef.getUuid())) {
      playerRef.sendMessage(Message.raw("Transfer already in progress..."));
      return;
    }

    databaseManager.saveInventory(playerRef.getUuid(), InventorySnapshotUtil.encode(player.getInventory()))
        .thenRun(() -> GameThread.run(playerRef, () -> {
          try {
            long now = System.currentTimeMillis();
            String payloadString = "sw1|" + now + "|" + host + "|" + port + "|" + warp.name();
            byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
            playerRef.referToServer(host, port, payload);
            playerRef.sendMessage(Message.raw("Transferring to " + host + ":" + port + "..."));
          } catch (Exception e) {
            RemoteTransferGuard.clear(playerRef.getUuid());
            playerRef.sendMessage(Message.raw("Remote transfer failed: " + e.getMessage()));
          }
        }))
        .exceptionally(err -> {
          RemoteTransferGuard.clear(playerRef.getUuid());
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err)
              ? "Inventory save timed out. Transfer cancelled."
              : "Failed to save inventory. Transfer cancelled.")));
          return null;
        });
  }

  private boolean isRemoteWarp(WarpRecord warp) {
    String host = warp.serverHost();
    Integer port = warp.serverPort();
    if (host == null || host.isBlank() || port == null || port <= 0) {
      return false;
    }
    String localHost = serverConfig.getHost();
    int localPort = serverConfig.getPort();
    if (localHost == null || localHost.isBlank() || localPort <= 0) {
      return false;
    }
    return !(host.equalsIgnoreCase(localHost) && port == localPort);
  }
}
