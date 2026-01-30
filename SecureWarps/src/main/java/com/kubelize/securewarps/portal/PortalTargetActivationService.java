package com.kubelize.securewarps.portal;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.PortalTarget;
import com.kubelize.securewarps.util.ErrorUtil;
import com.hypixel.hytale.server.core.util.concurrent.ThreadUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class PortalTargetActivationService {
  private static final int SYNC_INTERVAL_SECONDS = 30;

  private final DatabaseManager databaseManager;
  private final HytaleLogger logger;
  private ScheduledExecutorService scheduler;

  public PortalTargetActivationService(DatabaseManager databaseManager, HytaleLogger logger) {
    this.databaseManager = databaseManager;
    this.logger = logger;
  }

  public void start() {
    if (scheduler != null) {
      return;
    }
    scheduler = Executors.newSingleThreadScheduledExecutor(ThreadUtil.daemonCounted("SecureWarps-PortalSync"));
    scheduler.scheduleAtFixedRate(this::syncSafe, 5, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  public void close() {
    if (scheduler == null) {
      return;
    }
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      scheduler.shutdownNow();
    } finally {
      scheduler = null;
    }
  }

  private void syncSafe() {
    try {
      sync();
    } catch (Exception e) {
      logger.at(Level.WARNING).withCause(e).log("[SecureWarps] Portal target sync failed.");
    }
  }

  private void sync() {
    Map<String, World> worlds = Universe.get().getWorlds();
    for (World world : worlds.values()) {
      if (world == null || !world.isAlive()) {
        continue;
      }
      String worldId = world.getName();
      if (worldId == null || worldId.isBlank()) {
        continue;
      }
      databaseManager.listPortalTargets(worldId)
          .thenAccept(targets -> activateTargets(world, targets))
          .exceptionally(err -> {
            logger.at(Level.WARNING).withCause(ErrorUtil.rootCause(err))
                .log("[SecureWarps] Failed to load portal targets for world: " + worldId);
            return null;
          });
    }
  }

  private void activateTargets(World world, List<PortalTarget> targets) {
    if (targets == null || targets.isEmpty()) {
      return;
    }
    world.execute(() -> {
      for (PortalTarget target : targets) {
        BlockPosition pos = new BlockPosition(target.x(), target.y(), target.z());
        PortalBlockStateUtil.setInteractionState(world, pos, "Active");
      }
    });
  }
}
