package com.kubelize.securewarps.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldJoinCooldown {
  private static final long COOLDOWN_MS = 10_000L;
  private static final ConcurrentHashMap<UUID, Long> JOINS = new ConcurrentHashMap<>();

  private WorldJoinCooldown() {
  }

  public static void markJoin(UUID uuid) {
    if (uuid == null) {
      return;
    }
    JOINS.put(uuid, System.currentTimeMillis());
  }

  public static boolean shouldIgnore(UUID uuid) {
    if (uuid == null) {
      return false;
    }
    Long ts = JOINS.get(uuid);
    if (ts == null) {
      return false;
    }
    long now = System.currentTimeMillis();
    if (now - ts > COOLDOWN_MS) {
      JOINS.remove(uuid, ts);
      return false;
    }
    return true;
  }
}
