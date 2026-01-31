package com.kubelize.securewarps.teleport;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import com.kubelize.securewarps.portal.PortalRuntime;
import com.kubelize.securewarps.util.GameThread;
import com.hypixel.hytale.server.core.util.concurrent.ThreadUtil;
import java.nio.charset.StandardCharsets;
import com.hypixel.hytale.server.core.inventory.Inventory;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrossServerWarpListener implements AutoCloseable {
  private static final long REFERRAL_TTL_MS = 30_000L;
  private static final long FIRST_ARRIVAL_DELAY_MS = 3000L;
  private final DatabaseManager databaseManager;
  private final ScheduledExecutorService scheduler;
  private final ConcurrentHashMap<UUID, PendingWarp> pendingWarps = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, Long> lastAppliedWarpTs = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<ArrivalKey, Boolean> firstArrivals = new ConcurrentHashMap<>();

  public CrossServerWarpListener(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(ThreadUtil.daemonCounted("SecureWarps-WarpDelay"));
  }

  public void onSetupConnect(PlayerSetupConnectEvent event) {
    if (!event.isReferralConnection()) {
      return;
    }
    byte[] data = event.getReferralData();
    if (data == null || data.length == 0) {
      return;
    }
    String payload = new String(data, StandardCharsets.UTF_8).trim();
    ReferralPayload referral = parseReferralPayload(payload);
    if (referral == null || referral.warpName() == null || referral.warpName().isBlank()) {
      return;
    }
    UUID uuid = event.getUuid();
    Long lastApplied = lastAppliedWarpTs.get(uuid);
    if (lastApplied != null && isStale(lastApplied)) {
      lastAppliedWarpTs.remove(uuid, lastApplied);
      lastApplied = null;
    }
    if (lastApplied != null && referral.timestamp() <= lastApplied) {
      return;
    }
    CrossServerArrivalGuard.markArrival(uuid);
    pendingWarps.compute(uuid, (key, existing) -> {
      if (existing == null || referral.timestamp() > existing.timestamp()) {
        return new PendingWarp(referral.warpName(), referral.timestamp());
      }
      return existing;
    });
  }

  public void onPlayerReady(PlayerReadyEvent event) {
    PlayerRef playerRef = getPlayerRef(event.getPlayerRef());
    if (playerRef == null || !playerRef.isValid()) {
      return;
    }
    Ref<EntityStore> playerEntityRef = playerRef.getReference();
    if (playerEntityRef == null || !playerEntityRef.isValid()) {
      return;
    }
    UUID uuid = playerRef.getUuid();
    PendingWarp pending = pendingWarps.get(uuid);
    if (pending == null) {
      return;
    }
    String warpName = pending.warpName();
    pendingWarps.remove(uuid);
    lastAppliedWarpTs.merge(uuid, pending.timestamp(), Math::max);

    databaseManager.getWarpByName(warpName)
        .thenCompose(this::resolveWarpWorld)
        .thenCompose(result -> databaseManager.loadInventory(uuid)
            .thenApply(inventory -> new WarpAndInventory(result, inventory)))
        .thenAccept(result -> {
          if (result.isEmpty()) {
            GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw("Warp not found or world unavailable: " + warpName)));
            return;
          }
          GameThread.run(playerRef, () -> {
            if (!playerRef.isValid()) {
              return;
            }
            Player live = playerRef.getComponent(Player.getComponentType());
            if (live == null) {
              return;
            }
            if (result.inventory().isPresent()) {
              live.setInventory(result.inventory().get());
              live.sendInventory();
            }
            WarpRecord warp = result.warp().get();
            ArrivalKey arrivalKey = new ArrivalKey(uuid, warp.worldId());
            if (firstArrivals.putIfAbsent(arrivalKey, Boolean.TRUE) == null) {
              scheduleTeleport(playerRef, playerEntityRef, warp, warpName, FIRST_ARRIVAL_DELAY_MS, true);
            } else {
              teleport(playerRef, playerEntityRef, warp, warpName);
            }
          });
        })
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to resolve cross-server warp " + warpName, err);
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw("Failed to complete cross-server warp.")));
          return null;
        });
  }

  public void onPlayerDisconnect(PlayerDisconnectEvent event) {
    UUID uuid = event.getPlayerRef().getUuid();
    if (uuid != null) {
      pendingWarps.remove(uuid);
    }
  }

  private static void teleport(PlayerRef playerRef, Ref<EntityStore> ref, WarpRecord warp, String name) {
    teleportInternal(playerRef, ref, warp, name, true);
  }

  private static void teleportInternal(PlayerRef playerRef,
                                       Ref<EntityStore> ref,
                                       WarpRecord warp,
                                       String name,
                                       boolean notify) {
    runOnRefWorld(ref, () -> {
      World target = Universe.get().getWorld(warp.worldId());
      if (target == null) {
        if (notify) {
          playerRef.sendMessage(Message.raw("Warp world not loaded: " + warp.worldId()));
        }
        return;
      }

      Vector3d position = new Vector3d(warp.x(), warp.y(), warp.z());
      Vector3f rotation = new Vector3f(warp.rotX(), warp.rotY(), warp.rotZ());
      Teleport teleport = Teleport.createForPlayer(target, position, rotation);

      Store<EntityStore> store = ref.getStore();
      store.putComponent(ref, Teleport.getComponentType(), teleport);
      if (notify) {
        playerRef.sendMessage(Message.raw("Teleported to warp: " + name));
      }
    });
  }

  private void scheduleTeleport(PlayerRef playerRef,
                                Ref<EntityStore> ref,
                                WarpRecord warp,
                                String name,
                                long delayMs,
                                boolean allowRetry) {
    if (!playerRef.isValid()) {
      return;
    }
    scheduler.schedule(() -> attemptTeleport(playerRef, ref, warp, name, allowRetry),
        delayMs,
        TimeUnit.MILLISECONDS);
  }

  private void attemptTeleport(PlayerRef playerRef,
                               Ref<EntityStore> ref,
                               WarpRecord warp,
                               String name,
                               boolean allowRetry) {
    if (!playerRef.isValid() || ref == null || !ref.isValid()) {
      return;
    }
    World target = Universe.get().getWorld(warp.worldId());
    if (target == null) {
      return;
    }
    teleportInternal(playerRef, ref, warp, name, true);

    if (!allowRetry) {
      return;
    }
    scheduler.schedule(() -> teleportInternal(playerRef, ref, warp, name, false),
        2000L,
        TimeUnit.MILLISECONDS);
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

  private static void runOnRefWorld(Ref<EntityStore> ref, Runnable action) {
    if (ref == null || !ref.isValid()) {
      return;
    }
    EntityStore entityStore = ref.getStore().getExternalData();
    World world = entityStore == null ? null : entityStore.getWorld();
    if (world == null) {
      action.run();
      return;
    }
    if (world.isInThread()) {
      action.run();
    } else {
      world.execute(action);
    }
  }

  private ReferralPayload parseReferralPayload(String payload) {
    if (payload == null || payload.isBlank()) {
      return null;
    }
    if (!payload.startsWith("sw1|")) {
      return null;
    }
    String[] parts = payload.split("\\|", 5);
    if (parts.length < 5) {
      return null;
    }
    long now = System.currentTimeMillis();
    long ts;
    int port;
    try {
      ts = Long.parseLong(parts[1]);
      port = Integer.parseInt(parts[3]);
    } catch (NumberFormatException e) {
      return null;
    }
    if (ts > now + 10_000L || now - ts > REFERRAL_TTL_MS) {
      return null;
    }
    String host = parts[2];
    String localHost = PortalRuntime.serverConfig().getHost();
    int localPort = PortalRuntime.serverConfig().getPort();
    if (localHost != null && !localHost.isBlank() && localPort > 0) {
      if (!host.equalsIgnoreCase(localHost) || port != localPort) {
        return null;
      }
    }
    return new ReferralPayload(parts[4].trim(), ts);
  }

  private static boolean isStale(long timestamp) {
    return System.currentTimeMillis() - timestamp > REFERRAL_TTL_MS;
  }

  private record PendingWarp(String warpName, long timestamp) {}

  private record ReferralPayload(String warpName, long timestamp) {}

  private record ArrivalKey(UUID uuid, String worldId) {}

  @Override
  public void close() {
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
