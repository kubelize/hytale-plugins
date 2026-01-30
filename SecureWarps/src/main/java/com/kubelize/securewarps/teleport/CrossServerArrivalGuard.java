package com.kubelize.securewarps.teleport;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CrossServerArrivalGuard {
  private static final long ARRIVAL_IGNORE_MS = 30_000L;
  private static final ConcurrentHashMap<UUID, Long> ARRIVALS = new ConcurrentHashMap<>();

  private CrossServerArrivalGuard() {
  }

  public static void markArrival(UUID uuid) {
    if (uuid == null) {
      return;
    }
    ARRIVALS.put(uuid, System.currentTimeMillis());
  }

  public static boolean shouldIgnore(UUID uuid) {
    if (uuid == null) {
      return false;
    }
    Long ts = ARRIVALS.get(uuid);
    if (ts == null) {
      return false;
    }
    long now = System.currentTimeMillis();
    if (now - ts > ARRIVAL_IGNORE_MS) {
      ARRIVALS.remove(uuid, ts);
      return false;
    }
    return true;
  }
}
