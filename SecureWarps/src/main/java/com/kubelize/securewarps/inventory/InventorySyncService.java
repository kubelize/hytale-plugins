package com.kubelize.securewarps.inventory;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.logger.HytaleLogger;
import com.kubelize.securewarps.config.InventoryConfig;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.util.GameThread;
import com.hypixel.hytale.server.core.util.concurrent.ThreadUtil;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bson.BsonDocument;

public class InventorySyncService implements AutoCloseable {
  private final InventoryConfig config;
  private final DatabaseManager databaseManager;
  private final HytaleLogger logger;
  private ScheduledExecutorService scheduler;
  private final ConcurrentHashMap<UUID, PlayerRef> activePlayers = new ConcurrentHashMap<>();

  public InventorySyncService(InventoryConfig config, DatabaseManager databaseManager, HytaleLogger logger) {
    this.config = config;
    this.databaseManager = databaseManager;
    this.logger = logger;
  }

  public void start() {
    if (!config.isEnabled() || !config.isSavePeriodically()) {
      return;
    }
    scheduler = Executors.newSingleThreadScheduledExecutor(ThreadUtil.daemonCounted("SecureWarps-InvSave"));
    int interval = Math.max(10, config.getSaveIntervalSeconds());
    scheduler.scheduleAtFixedRate(this::saveAllPlayersSafe, interval, interval, TimeUnit.SECONDS);
  }

  public void onPlayerReady(PlayerReadyEvent event) {
    if (!config.isEnabled()) {
      return;
    }
    Player player = event.getPlayer();
    PlayerRef playerRef = player.getPlayerRef();
    UUID uuid = playerRef.getUuid();
    activePlayers.put(uuid, playerRef);

    if (!config.isLoadOnReady()) {
      return;
    }

    databaseManager.loadInventory(uuid)
        .thenAccept(optional -> {
          if (optional.isEmpty()) {
            return;
          }
          Inventory restored = optional.get();
          GameThread.run(playerRef, () -> {
            Player live = playerRef.getComponent(Player.getComponentType());
            if (live == null) {
              return;
            }
            live.setInventory(restored);
            live.sendInventory();
          });
        })
        .exceptionally(err -> {
          logger.at(Level.WARNING).withCause(err).log("[SecureWarps] Failed to load inventory for " + uuid);
          return null;
        });
  }

  public void onPlayerDisconnect(PlayerDisconnectEvent event) {
    if (!config.isEnabled()) {
      return;
    }
    PlayerRef ref = event.getPlayerRef();
    activePlayers.remove(ref.getUuid());
    if (!config.isSaveOnDisconnect()) {
      return;
    }
    GameThread.run(ref, () -> {
      Player player = ref.getComponent(Player.getComponentType());
      if (player == null) {
        return;
      }
      savePlayerInventory(player);
    });
  }

  private void saveAllPlayersSafe() {
    try {
      for (PlayerRef ref : activePlayers.values()) {
        if (ref == null || !ref.isValid()) {
          continue;
        }
        GameThread.run(ref, () -> {
          Player player = ref.getComponent(Player.getComponentType());
          if (player == null) {
            return;
          }
          savePlayerInventory(player);
        });
      }
    } catch (Exception e) {
      logger.at(Level.WARNING).withCause(e).log("[SecureWarps] Periodic inventory save failed");
    }
  }

  private void savePlayerInventory(Player player) {
    UUID uuid = player.getPlayerRef().getUuid();
    Inventory inventory = player.getInventory();
    BsonDocument snapshot = InventorySnapshotUtil.encode(inventory);
    databaseManager.saveInventory(uuid, snapshot)
        .exceptionally(err -> {
          logger.at(Level.WARNING).withCause(err).log("[SecureWarps] Failed to save inventory for " + uuid);
          return null;
        });
  }

  @Override
  public void close() {
    if (scheduler != null) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
