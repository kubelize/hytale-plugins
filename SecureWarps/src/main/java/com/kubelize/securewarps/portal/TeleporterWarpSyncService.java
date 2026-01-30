package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.math.vector.Transform;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import com.kubelize.securewarps.util.ErrorUtil;
import com.hypixel.hytale.server.core.util.concurrent.ThreadUtil;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class TeleporterWarpSyncService implements AutoCloseable {
  private static final long SYNC_INTERVAL_SECONDS = 30;

  private final DatabaseManager databaseManager;
  private final Set<String> managedWarps = ConcurrentHashMap.newKeySet();
  private ScheduledExecutorService scheduler;

  public TeleporterWarpSyncService(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
  }

  public void start() {
    if (scheduler != null) {
      return;
    }
    scheduler = Executors.newSingleThreadScheduledExecutor(ThreadUtil.daemonCounted("SecureWarps-TeleporterSync"));
    scheduler.scheduleAtFixedRate(this::syncSafe, 0, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  private void syncSafe() {
    databaseManager.listWarps()
        .thenAccept(this::syncWarps)
        .exceptionally(err -> {
          PortalRuntime.logger().at(Level.WARNING).withCause(ErrorUtil.rootCause(err))
              .log("[SecureWarps] Failed to sync teleporter warps.");
          return null;
        });
  }

  private void syncWarps(List<WarpRecord> warps) {
    TeleportPlugin teleportPlugin = TeleportPlugin.get();
    if (teleportPlugin == null) {
      return;
    }
    Map<String, Warp> warpMap = teleportPlugin.getWarps();
    if (warpMap == null) {
      return;
    }

    Map<String, Warp> snapshot = new HashMap<>();
    Set<String> nextManaged = new HashSet<>();
    synchronized (warpMap) {
      for (WarpRecord record : warps) {
        Warp warp = buildWarp(record);
        if (warp == null) {
          continue;
        }
        String key = TeleporterWarpCache.normalizeKey(record.name());
        warpMap.put(key, warp);
        snapshot.put(key, warp);
        nextManaged.add(key);
      }
    }
    TeleporterWarpCache.updateSnapshot(snapshot);
    TeleporterWarpCache.removeStale(warpMap, nextManaged);
    managedWarps.clear();
    managedWarps.addAll(nextManaged);
  }

  private Warp buildWarp(WarpRecord record) {
    try {
      Warp warp = new Warp();
      setField(Warp.class, warp, "id", record.name());
      setField(Warp.class, warp, "world", resolveWorldLabel(record));
      Transform transform = new Transform(record.x(), record.y(), record.z(), record.rotX(), record.rotY(), record.rotZ());
      setField(Warp.class, warp, "transform", transform);
      String creator = record.ownerUuid() != null ? record.ownerUuid().toString() : "SecureWarps";
      setField(Warp.class, warp, "creator", creator);
      setField(Warp.class, warp, "creationDate", Instant.now());
      return warp;
    } catch (Exception e) {
      PortalRuntime.logger().at(Level.WARNING).withCause(e)
          .log("[SecureWarps] Failed to build teleporter warp: " + record.name());
      return null;
    }
  }

  private String resolveWorldLabel(WarpRecord record) {
    String worldId = record.worldId();
    String host = record.serverHost();
    Integer port = record.serverPort();
    boolean hasRemote = host != null && !host.isBlank() && port != null && port > 0;
    if (hasRemote && isRemoteDestination(host, port)) {
      return "Remote (" + host + ":" + port + ")";
    }
    if (worldId == null || worldId.isBlank()) {
      return hasRemote ? "Remote" : "Unknown";
    }
    return worldId;
  }

  private boolean isRemoteDestination(String host, int port) {
    String localHost = PortalRuntime.serverConfig().getHost();
    int localPort = PortalRuntime.serverConfig().getPort();
    if (localHost == null || localHost.isBlank() || localPort <= 0) {
      return true;
    }
    return !(host.equalsIgnoreCase(localHost) && port == localPort);
  }

  private void setField(Class<?> type, Object instance, String fieldName, Object value) throws Exception {
    Field field = type.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(instance, value);
  }

  @Override
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
}
