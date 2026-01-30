package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class TeleporterWarpCache {
  private static final Set<String> managed = new HashSet<>();
  private static volatile Map<String, Warp> snapshot = Collections.emptyMap();

  private TeleporterWarpCache() {
  }

  static void updateSnapshot(Map<String, Warp> warps) {
    snapshot = Collections.unmodifiableMap(new HashMap<>(warps));
  }

  static Map<String, Warp> getSnapshot() {
    return snapshot;
  }

  static void seedTeleportPlugin() {
    TeleportPlugin plugin = TeleportPlugin.get();
    if (plugin == null) {
      return;
    }
    Map<String, Warp> target = plugin.getWarps();
    if (target == null) {
      return;
    }
    Map<String, Warp> source = snapshot;
    synchronized (target) {
      for (Map.Entry<String, Warp> entry : source.entrySet()) {
        String key = normalizeKey(entry.getKey());
        target.put(key, entry.getValue());
        managed.add(key);
      }
    }
  }

  static void removeStale(Map<String, Warp> target, Set<String> nextManaged) {
    synchronized (target) {
      for (String existing : managed) {
        if (!nextManaged.contains(existing)) {
          target.remove(existing);
        }
      }
    }
    managed.clear();
    managed.addAll(nextManaged);
  }

  static String normalizeKey(String key) {
    return key == null ? "" : key.toLowerCase();
  }
}
