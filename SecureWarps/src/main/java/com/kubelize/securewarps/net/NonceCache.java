package com.kubelize.securewarps.net;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NonceCache {
  private final ConcurrentHashMap<String, Long> nonces = new ConcurrentHashMap<>();

  public boolean registerIfFresh(String nonce, long timestampSeconds, int allowedSkewSeconds) {
    long now = Instant.now().getEpochSecond();
    if (Math.abs(now - timestampSeconds) > allowedSkewSeconds) {
      return false;
    }
    return nonces.putIfAbsent(nonce, timestampSeconds) == null;
  }

  public void pruneOlderThan(long cutoffSeconds) {
    Iterator<Map.Entry<String, Long>> it = nonces.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Long> entry = it.next();
      if (entry.getValue() < cutoffSeconds) {
        it.remove();
      }
    }
  }
}
