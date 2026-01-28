package com.kubelize.securewarps.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kubelize.securewarps.config.HttpServerConfig;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.concurrent.ThreadUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class SecureHttpServer implements AutoCloseable {
  private final HttpServerConfig config;
  private final DatabaseManager databaseManager;
  private final HytaleLogger logger;
  private final ObjectMapper mapper = new ObjectMapper();
  private final NonceCache nonceCache;
  private HttpsServer server;
  private ScheduledExecutorService noncePruner;
  private String sharedSecret;

  public SecureHttpServer(HttpServerConfig config, DatabaseManager databaseManager, HytaleLogger logger) {
    this.config = config;
    this.databaseManager = databaseManager;
    this.logger = logger;
    this.sharedSecret = resolveSharedSecret();
    this.nonceCache = new NonceCache(config.getMaxNonceEntries());
  }

  public void start() throws Exception {
    if (sharedSecret == null || sharedSecret.isBlank()) {
      throw new IllegalStateException("HTTP server requires a SharedSecret or SharedSecretEnv");
    }

    InetSocketAddress address = new InetSocketAddress(config.getBindHost(), config.getPort());
    server = HttpsServer.create(address, 0);
    server.setHttpsConfigurator(new HttpsConfigurator(buildSslContext()) {
      @Override
      public void configure(HttpsParameters params) {
        params.setNeedClientAuth(config.isRequireClientCert());
        params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
      }
    });

    server.createContext("/health", exchange -> respond(exchange, 200, "ok"));
    server.createContext("/warps", new WarpsHandler());
    server.createContext("/warps/", new WarpsHandler());

    server.setExecutor(Executors.newFixedThreadPool(Math.max(2, config.getThreadPoolSize()), ThreadUtil.daemonCounted("SecureWarps-HTTP")));
    server.start();

    noncePruner = Executors.newSingleThreadScheduledExecutor();
    noncePruner.scheduleAtFixedRate(() -> {
      long cutoff = Instant.now().getEpochSecond() - config.getClockSkewSeconds();
      nonceCache.pruneOlderThan(cutoff);
    }, 30, 30, TimeUnit.SECONDS);

    logger.at(Level.INFO).log("[SecureWarps] HTTPS server started on " + config.getBindHost() + ":" + config.getPort());
  }

  @Override
  public void close() {
    if (server != null) {
      server.stop(0);
    }
    if (noncePruner != null) {
      noncePruner.shutdownNow();
    }
  }

  private class WarpsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      try {
        if (!authenticate(exchange)) {
          respond(exchange, 401, "unauthorized");
          return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && "/warps".equals(path)) {
          handleList(exchange);
          return;
        }

        if (path.startsWith("/warps/")) {
          String name = path.substring("/warps/".length());
          if (name.isBlank()) {
            respond(exchange, 400, "warp name required");
            return;
          }

          if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange, name);
            return;
          }

          if ("DELETE".equalsIgnoreCase(method)) {
            handleDelete(exchange, name);
            return;
          }
        }

        if ("POST".equalsIgnoreCase(method) && "/warps".equals(path)) {
          handleUpsert(exchange);
          return;
        }

        respond(exchange, 404, "not found");
      } catch (Exception e) {
        logger.at(Level.WARNING).withCause(e).log("[SecureWarps] HTTP handler failed");
        respond(exchange, 500, "server error");
      }
    }
  }

  private void handleList(HttpExchange exchange) {
    databaseManager.listWarps()
        .thenAccept(warps -> respondJson(exchange, 200, warps))
        .exceptionally(err -> {
          respond(exchange, 500, "db error");
          return null;
        });
  }

  private void handleGet(HttpExchange exchange, String name) {
    databaseManager.getWarpByName(name)
        .thenAccept(warp -> {
          if (warp.isEmpty()) {
            respond(exchange, 404, "not found");
          } else {
            respondJson(exchange, 200, warp.get());
          }
        })
        .exceptionally(err -> {
          respond(exchange, 500, "db error");
          return null;
        });
  }

  private void handleDelete(HttpExchange exchange, String name) {
    databaseManager.deleteWarpByName(name)
        .thenAccept(deleted -> respond(exchange, deleted ? 200 : 404, deleted ? "deleted" : "not found"))
        .exceptionally(err -> {
          respond(exchange, 500, "db error");
          return null;
        });
  }

  private void handleUpsert(HttpExchange exchange) throws IOException {
    byte[] body = maybeReadBody(exchange, config.getMaxBodyBytes());
    WarpRecord warp = mapper.readValue(body, WarpRecord.class);

    if (warp.name() == null || warp.name().isBlank()) {
      respond(exchange, 400, "warp name required");
      return;
    }

    WarpRecord normalized = new WarpRecord(
        warp.id() == null ? UUID.randomUUID() : warp.id(),
        warp.name(),
        warp.ownerUuid(),
        warp.worldId(),
        warp.x(),
        warp.y(),
        warp.z(),
        warp.rotX(),
        warp.rotY(),
        warp.rotZ(),
        warp.serverHost(),
        warp.serverPort()
    );

    databaseManager.saveWarp(normalized)
        .thenRun(() -> respond(exchange, 200, "ok"))
        .exceptionally(err -> {
          respond(exchange, 500, "db error");
          return null;
        });
  }

  private boolean authenticate(HttpExchange exchange) throws IOException {
    String timestamp = exchange.getRequestHeaders().getFirst(HttpAuth.HEADER_TIMESTAMP);
    String nonce = exchange.getRequestHeaders().getFirst(HttpAuth.HEADER_NONCE);
    String signature = exchange.getRequestHeaders().getFirst(HttpAuth.HEADER_SIGNATURE);

    if (timestamp == null || nonce == null || signature == null) {
      return false;
    }

    long ts;
    try {
      ts = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      return false;
    }

    if (!nonceCache.registerIfFresh(nonce, ts, config.getClockSkewSeconds())) {
      return false;
    }

    byte[] body = maybeReadBody(exchange, config.getMaxBodyBytes());
    String bodyHash = HttpAuth.sha256Hex(body);
    String expected = HttpAuth.sign(sharedSecret, exchange.getRequestMethod(), exchange.getRequestURI().getPath(), timestamp, nonce, bodyHash);

    if (!HttpAuth.constantTimeEquals(expected, signature)) {
      return false;
    }

    exchange.setAttribute("cachedBody", body);
    return true;
  }

  private static byte[] maybeReadBody(HttpExchange exchange, int maxBytes) throws IOException {
    Object cached = exchange.getAttribute("cachedBody");
    if (cached instanceof byte[] bytes) {
      return bytes;
    }

    String method = exchange.getRequestMethod();
    String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
    boolean hasBody = contentLength != null && !contentLength.equals("0");
    if (!hasBody && ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
      return new byte[0];
    }

    try (InputStream in = exchange.getRequestBody(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int read;
      int total = 0;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > maxBytes) {
          throw new IOException("request body too large");
        }
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    }
  }

  private void respond(HttpExchange exchange, int status, String body) {
    try {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
    } catch (IOException ignored) {
    } finally {
      exchange.close();
    }
  }

  private void respondJson(HttpExchange exchange, int status, Object data) {
    try {
      byte[] bytes = mapper.writeValueAsBytes(data);
      exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
    } catch (IOException ignored) {
    } finally {
      exchange.close();
    }
  }

  private SSLContext buildSslContext() throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream in = new FileInputStream(config.getKeyStorePath())) {
      keyStore.load(in, config.getKeyStorePassword().toCharArray());
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, config.getKeyStorePassword().toCharArray());

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

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
    return sslContext;
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
}
