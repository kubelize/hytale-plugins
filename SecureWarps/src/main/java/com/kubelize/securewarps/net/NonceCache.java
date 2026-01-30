package com.kubelize.securewarps.net;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class NonceCache {
  private final int maxEntries;
  private final Map<String, Long> nonces =
      new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
          return size() > maxEntries;
        }
      };

  public NonceCache(int maxEntries) {
    this.maxEntries = Math.max(1000, maxEntries);
  }

  public synchronized boolean registerIfFresh(
      String nonce, long timestampSeconds, int allowedSkewSeconds) {
    long now = Instant.now().getEpochSecond();
    if (Math.abs(now - timestampSeconds) > allowedSkewSeconds) {
      return false;
    }
    pruneOlderThan(now - allowedSkewSeconds);
    if (nonces.containsKey(nonce)) {
      return false;
    }
    // Store server time to prevent client-controlled eviction extension.
    nonces.put(nonce, now);
    return true;
  }

  public synchronized void pruneOlderThan(long cutoffSeconds) {
    Iterator<Map.Entry<String, Long>> it = nonces.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Long> entry = it.next();
      if (entry.getValue() < cutoffSeconds) {
        it.remove();
      }
    }
  }
}
