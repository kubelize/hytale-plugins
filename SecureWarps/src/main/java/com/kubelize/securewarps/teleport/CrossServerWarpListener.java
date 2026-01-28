package com.kubelize.securewarps.teleport;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import com.kubelize.securewarps.util.GameThread;
import java.nio.charset.StandardCharsets;
import com.hypixel.hytale.server.core.inventory.Inventory;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrossServerWarpListener {
  private final DatabaseManager databaseManager;
  private final ConcurrentHashMap<UUID, String> pendingWarps = new ConcurrentHashMap<>();

  public CrossServerWarpListener(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
  }

  public void onSetupConnect(PlayerSetupConnectEvent event) {
    if (!event.isReferralConnection()) {
      return;
    }
    byte[] data = event.getReferralData();
    if (data == null || data.length == 0) {
      return;
    }
    String warpName = new String(data, StandardCharsets.UTF_8).trim();
    if (warpName.isBlank()) {
      return;
    }
    pendingWarps.put(event.getUuid(), warpName);
  }

  public void onPlayerReady(PlayerReadyEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUuid();
    String warpName = pendingWarps.remove(uuid);
    if (warpName == null) {
      return;
    }

    databaseManager.getWarpByName(warpName)
        .thenCompose(this::resolveWarpWorld)
        .thenCompose(result -> databaseManager.loadInventory(uuid)
            .thenApply(inventory -> new WarpAndInventory(result, inventory)))
        .thenAccept(result -> {
          if (result.isEmpty()) {
            GameThread.run(getPlayerRef(event.getPlayerRef()), () -> player.sendMessage(Message.raw("Warp not found or world unavailable: " + warpName)));
            return;
          }
          GameThread.run(getPlayerRef(event.getPlayerRef()), () -> {
            if (result.inventory().isPresent()) {
              player.setInventory(result.inventory().get());
              player.sendInventory();
            }
            teleport(player, event.getPlayerRef(), result.warp().get(), warpName);
          });
        })
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to resolve cross-server warp " + warpName, err);
          GameThread.run(getPlayerRef(event.getPlayerRef()), () -> player.sendMessage(Message.raw("Failed to complete cross-server warp.")));
          return null;
        });
  }

  private static void teleport(Player player, Ref<EntityStore> ref, WarpRecord warp, String name) {
    World target = Universe.get().getWorld(warp.worldId());
    if (target == null) {
      player.sendMessage(Message.raw("Warp world not loaded: " + warp.worldId()));
      return;
    }

    Vector3d position = new Vector3d(warp.x(), warp.y(), warp.z());
    Vector3f rotation = new Vector3f(warp.rotX(), warp.rotY(), warp.rotZ());
    Teleport teleport = Teleport.createForPlayer(target, position, rotation);

    Store<EntityStore> store = ref.getStore();
    store.putComponent(ref, Teleport.getComponentType(), teleport);
    player.sendMessage(Message.raw("Teleported to warp: " + name));
  }

  private java.util.concurrent.CompletableFuture<Optional<WarpRecord>> resolveWarpWorld(Optional<WarpRecord> warp) {
    if (warp.isEmpty()) {
      return java.util.concurrent.CompletableFuture.completedFuture(warp);
    }
    WarpRecord record = warp.get();
    World target = Universe.get().getWorld(record.worldId());
    if (target != null) {
      return java.util.concurrent.CompletableFuture.completedFuture(warp);
    }
    if (!Universe.get().isWorldLoadable(record.worldId())) {
      return java.util.concurrent.CompletableFuture.completedFuture(Optional.empty());
    }
    return Universe.get().loadWorld(record.worldId()).thenApply(world -> warp);
  }

  private record WarpAndInventory(Optional<WarpRecord> warp, Optional<Inventory> inventory) {
    boolean isEmpty() {
      return warp == null || warp.isEmpty();
    }
  }

  private static PlayerRef getPlayerRef(Ref<EntityStore> ref) {
    if (ref == null || !ref.isValid()) {
      return null;
    }
    return ref.getStore().getComponent(ref, PlayerRef.getComponentType());
  }
}
