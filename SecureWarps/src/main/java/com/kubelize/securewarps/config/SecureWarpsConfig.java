package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class SecureWarpsConfig {
  public static final BuilderCodec<SecureWarpsConfig> CODEC =
      BuilderCodec.builder(SecureWarpsConfig.class, SecureWarpsConfig::new)
          .append(new KeyedCodec("Database", DatabaseConfig.CODEC), (o, i) -> o.database = i, o -> o.database).add()
          .append(new KeyedCodec("Inventory", InventoryConfig.CODEC), (o, i) -> o.inventory = i, o -> o.inventory).add()
          .append(new KeyedCodec("HttpClient", HttpClientConfig.CODEC), (o, i) -> o.httpClient = i, o -> o.httpClient).add()
          .append(new KeyedCodec("HttpServer", HttpServerConfig.CODEC), (o, i) -> o.httpServer = i, o -> o.httpServer).add()
          .append(new KeyedCodec("Permissions", PermissionsConfig.CODEC), (o, i) -> o.permissions = i, o -> o.permissions).add()
          .build();

  private DatabaseConfig database = new DatabaseConfig();
  private InventoryConfig inventory = new InventoryConfig();
  private HttpClientConfig httpClient = new HttpClientConfig();
  private HttpServerConfig httpServer = new HttpServerConfig();
  private PermissionsConfig permissions = new PermissionsConfig();

  public DatabaseConfig getDatabase() {
    return database;
  }

  public InventoryConfig getInventory() {
    return inventory;
  }

  public HttpClientConfig getHttpClient() {
    return httpClient;
  }

  public HttpServerConfig getHttpServer() {
    return httpServer;
  }

  public PermissionsConfig getPermissions() {
    return permissions;
  }
}
