package com.kubelize.securewarps.net;

import com.kubelize.securewarps.config.HttpClientConfig;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SecureHttpClient {
  private final HttpClientConfig config;
  private final HttpClient client;
  private final String sharedSecret;

  public SecureHttpClient(HttpClientConfig config) {
    this.config = config;
    this.client = HttpClient.newBuilder()
        .sslContext(buildSslContext())
        .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis()))
        .build();
    this.sharedSecret = resolveSharedSecret();
  }

  public HttpResponse<String> get(String path) throws Exception {
    return sendSigned("GET", path, null);
  }

  public HttpResponse<String> post(String path, String jsonBody) throws Exception {
    return sendSigned("POST", path, jsonBody);
  }

  public HttpResponse<String> delete(String path) throws Exception {
    return sendSigned("DELETE", path, null);
  }

  private HttpResponse<String> sendSigned(String method, String path, String body) throws Exception {
    String payload = body == null ? "" : body;
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(config.getBaseUrl() + path))
        .timeout(Duration.ofMillis(config.getRequestTimeoutMillis()))
        .method(method, payload.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(payload));

    if (sharedSecret != null && !sharedSecret.isBlank()) {
      String timestamp = String.valueOf(Instant.now().getEpochSecond());
      String nonce = UUID.randomUUID().toString();
      String bodyHash = HttpAuth.sha256Hex(payload.getBytes());
      String signature = HttpAuth.sign(sharedSecret, method, path, timestamp, nonce, bodyHash);
      builder.header(HttpAuth.HEADER_TIMESTAMP, timestamp);
      builder.header(HttpAuth.HEADER_NONCE, nonce);
      builder.header(HttpAuth.HEADER_SIGNATURE, signature);
    }

    if (!payload.isEmpty()) {
      builder.header("Content-Type", "application/json");
    }

    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String resolveSharedSecret() {
    String envKey = config.getSharedSecretEnv();
    if (envKey != null && !envKey.isBlank()) {
      String envValue = System.getenv(envKey);
      if (envValue != null && !envValue.isBlank()) {
        return envValue;
      }
    }
    return config.getSharedSecret();
  }

  private SSLContext buildSslContext() {
    try {
      SSLContext context = SSLContext.getInstance("TLS");

      TrustManagerFactory tmf = null;
      if (config.getTrustStorePath() != null && !config.getTrustStorePath().isBlank()) {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(config.getTrustStorePath())) {
          char[] pass = config.getTrustStorePassword() == null ? null : config.getTrustStorePassword().toCharArray();
          trustStore.load(in, pass);
        }
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
      }

      KeyManagerFactory kmf = null;
      if (config.getKeyStorePath() != null && !config.getKeyStorePath().isBlank()) {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(config.getKeyStorePath())) {
          char[] pass = config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray();
          keyStore.load(in, pass);
        }
        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] pass = config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray();
        kmf.init(keyStore, pass);
      }

      context.init(kmf == null ? null : kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
      return context;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize TLS context", e);
    }
  }
}
