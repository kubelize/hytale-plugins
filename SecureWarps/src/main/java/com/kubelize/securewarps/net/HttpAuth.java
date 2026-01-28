package com.kubelize.securewarps.net;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HttpAuth {
  public static final String HEADER_TIMESTAMP = "X-SW-Timestamp";
  public static final String HEADER_NONCE = "X-SW-Nonce";
  public static final String HEADER_SIGNATURE = "X-SW-Signature";

  private HttpAuth() {}

  public static String sha256Hex(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(data));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash payload", e);
    }
  }

  public static String sign(String secret, String method, String path, String timestamp, String nonce, String bodyHashHex) {
    try {
      String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + bodyHashHex;
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(signature);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to sign request", e);
    }
  }

  public static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }
}
