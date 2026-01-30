package com.kubelize.securewarps.portal;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class RemoteTransferGuard {
  private static final long TRANSFER_TTL_MS = 10_000L;
  private static final ConcurrentHashMap<UUID, Long> IN_FLIGHT = new ConcurrentHashMap<>();

  private RemoteTransferGuard() {
  }

  static boolean tryMark(UUID uuid) {
    if (uuid == null) {
      return true;
    }
    long now = System.currentTimeMillis();
    Long existing = IN_FLIGHT.put(uuid, now);
    if (existing == null) {
      return true;
    }
    if (now - existing > TRANSFER_TTL_MS) {
      IN_FLIGHT.put(uuid, now);
      return true;
    }
    return false;
  }

  static void clear(UUID uuid) {
    if (uuid == null) {
      return;
    }
    IN_FLIGHT.remove(uuid);
  }
}
