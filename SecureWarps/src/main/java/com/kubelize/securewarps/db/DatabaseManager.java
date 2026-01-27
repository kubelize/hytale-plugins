package com.kubelize.securewarps.db;

import com.kubelize.securewarps.config.DatabaseConfig;
import com.kubelize.securewarps.inventory.InventorySnapshotUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BsonDocument;

public class DatabaseManager implements AutoCloseable {
  private final DatabaseConfig config;
  private final Logger logger;
  private HikariDataSource dataSource;
  private ExecutorService executor;

  public DatabaseManager(DatabaseConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  public void start() {
    HikariConfig hikari = new HikariConfig();
    hikari.setJdbcUrl(buildJdbcUrl());
    hikari.setUsername(config.getUsername());
    hikari.setPassword(resolvePassword());
    hikari.setMaximumPoolSize(config.getMaxPoolSize());
    hikari.setMinimumIdle(config.getMinIdle());
    hikari.setConnectionTimeout(config.getConnectTimeoutMillis());

    Properties dsProps = new Properties();
    String sslMode = config.getSslMode();
    if (sslMode != null && !sslMode.isBlank()) {
      dsProps.setProperty("sslmode", sslMode);
    }

    addIfPresent(dsProps, "sslrootcert", config.getSslRootCert());
    addIfPresent(dsProps, "sslcert", config.getSslCert());
    addIfPresent(dsProps, "sslkey", config.getSslKey());
    addIfPresent(dsProps, "sslpassword", config.getSslKeyPassword());

    hikari.setDataSourceProperties(dsProps);

    this.executor = Executors.newFixedThreadPool(Math.max(2, config.getDbExecutorThreads()));
    this.dataSource = new HikariDataSource(hikari);

    logger.at(Level.INFO).log("[SecureWarps] Database initialized with TLS mode: " + safe(sslMode));
    initializeSchema();
  }

  public CompletableFuture<Void> saveWarp(WarpRecord warp) {
    String sql = "INSERT INTO warps (id, name, owner_uuid, world_id, x, y, z, rot_x, rot_y, rot_z, updated_at)\n"
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)\n"
        + "ON CONFLICT (name) DO UPDATE SET\n"
        + "  owner_uuid = EXCLUDED.owner_uuid,\n"
        + "  world_id = EXCLUDED.world_id,\n"
        + "  x = EXCLUDED.x,\n"
        + "  y = EXCLUDED.y,\n"
        + "  z = EXCLUDED.z,\n"
        + "  rot_x = EXCLUDED.rot_x,\n"
        + "  rot_y = EXCLUDED.rot_y,\n"
        + "  rot_z = EXCLUDED.rot_z,\n"
        + "  updated_at = CURRENT_TIMESTAMP";

    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setObject(1, warp.id());
        stmt.setString(2, warp.name());
        stmt.setObject(3, warp.ownerUuid());
        stmt.setString(4, warp.worldId());
        stmt.setDouble(5, warp.x());
        stmt.setDouble(6, warp.y());
        stmt.setDouble(7, warp.z());
        stmt.setFloat(8, warp.rotX());
        stmt.setFloat(9, warp.rotY());
        stmt.setFloat(10, warp.rotZ());
        stmt.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to save warp: " + warp.name(), e);
      }
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<Optional<WarpRecord>> getWarpByName(String name) {
    String sql = "SELECT id, name, owner_uuid, world_id, x, y, z, rot_x, rot_y, rot_z FROM warps WHERE name = ?";
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, name);
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            return Optional.empty();
          }
          return Optional.of(new WarpRecord(
              (UUID) rs.getObject("id"),
              rs.getString("name"),
              (UUID) rs.getObject("owner_uuid"),
              rs.getString("world_id"),
              rs.getDouble("x"),
              rs.getDouble("y"),
              rs.getDouble("z"),
              rs.getFloat("rot_x"),
              rs.getFloat("rot_y"),
              rs.getFloat("rot_z")
          ));
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch warp: " + name, e);
      }
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<List<WarpRecord>> listWarps() {
    String sql = "SELECT id, name, owner_uuid, world_id, x, y, z, rot_x, rot_y, rot_z FROM warps ORDER BY name";
    return CompletableFuture.supplyAsync(() -> {
      List<WarpRecord> results = new ArrayList<>();
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            results.add(new WarpRecord(
                (UUID) rs.getObject("id"),
                rs.getString("name"),
                (UUID) rs.getObject("owner_uuid"),
                rs.getString("world_id"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("rot_x"),
                rs.getFloat("rot_y"),
                rs.getFloat("rot_z")
            ));
          }
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to list warps", e);
      }
      return results;
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<Boolean> deleteWarpByName(String name) {
    String sql = "DELETE FROM warps WHERE name = ?";
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, name);
        return stmt.executeUpdate() > 0;
      } catch (SQLException e) {
        throw new RuntimeException("Failed to delete warp: " + name, e);
      }
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<Void> saveInventory(UUID playerUuid, BsonDocument snapshot) {
    String sql = "INSERT INTO inventories (player_uuid, data, updated_at)\n"
        + "VALUES (?, ?::jsonb, CURRENT_TIMESTAMP)\n"
        + "ON CONFLICT (player_uuid) DO UPDATE SET\n"
        + "  data = EXCLUDED.data,\n"
        + "  updated_at = CURRENT_TIMESTAMP";

    return CompletableFuture.runAsync(() -> {
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setObject(1, playerUuid);
        stmt.setString(2, snapshot.toJson());
        stmt.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to save inventory for " + playerUuid, e);
      }
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<Optional<com.hypixel.hytale.server.core.inventory.Inventory>> loadInventory(UUID playerUuid) {
    String sql = "SELECT data FROM inventories WHERE player_uuid = ?";
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setObject(1, playerUuid);
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next()) {
            return Optional.empty();
          }
          String json = rs.getString("data");
          BsonDocument doc = BsonDocument.parse(json);
          return Optional.of(InventorySnapshotUtil.decode(doc));
        }
      } catch (SQLException e) {
        throw new RuntimeException("Failed to load inventory for " + playerUuid, e);
      }
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<Boolean> deleteInventory(UUID playerUuid) {
    String sql = "DELETE FROM inventories WHERE player_uuid = ?";
    return CompletableFuture.supplyAsync(() -> {
      try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setObject(1, playerUuid);
        return stmt.executeUpdate() > 0;
      } catch (SQLException e) {
        throw new RuntimeException("Failed to delete inventory for " + playerUuid, e);
      }
    }, executor).orTimeout(config.getDbOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
  }

  private void initializeSchema() {
    String warpsSql = "CREATE TABLE IF NOT EXISTS warps (\n"
        + "  id UUID PRIMARY KEY,\n"
        + "  name VARCHAR(64) UNIQUE NOT NULL,\n"
        + "  owner_uuid UUID NOT NULL,\n"
        + "  world_id VARCHAR(64) NOT NULL,\n"
        + "  x DOUBLE PRECISION NOT NULL,\n"
        + "  y DOUBLE PRECISION NOT NULL,\n"
        + "  z DOUBLE PRECISION NOT NULL,\n"
        + "  rot_x FLOAT NOT NULL,\n"
        + "  rot_y FLOAT NOT NULL,\n"
        + "  rot_z FLOAT NOT NULL,\n"
        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
        + "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n"
        + ")";

    String inventoriesSql = "CREATE TABLE IF NOT EXISTS inventories (\n"
        + "  player_uuid UUID PRIMARY KEY,\n"
        + "  data JSONB NOT NULL,\n"
        + "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n"
        + "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n"
        + ")";

    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
      stmt.execute(warpsSql);
      stmt.execute(inventoriesSql);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to initialize schema", e);
    }
  }

  private String buildJdbcUrl() {
    return "jdbc:postgresql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase();
  }

  private String resolvePassword() {
    String envKey = config.getPasswordEnv();
    if (envKey != null && !envKey.isBlank()) {
      String envValue = System.getenv(envKey);
      if (envValue != null && !envValue.isBlank()) {
        return envValue;
      }
    }
    return config.getPassword();
  }

  private static void addIfPresent(Properties props, String key, String value) {
    if (value != null && !value.isBlank()) {
      props.setProperty(key, value);
    }
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "(default)" : value;
  }

  @Override
  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
}
