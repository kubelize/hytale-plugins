package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class SecureWarpsConfig {
  public static final BuilderCodec<SecureWarpsConfig> CODEC =
      BuilderCodec.<SecureWarpsConfig>builder(SecureWarpsConfig.class, SecureWarpsConfig::new)
          .append(new KeyedCodec<DatabaseConfig>("Database", DatabaseConfig.CODEC), (SecureWarpsConfig o, DatabaseConfig i) -> o.database = i, o -> o.database).add()
          .append(new KeyedCodec<InventoryConfig>("Inventory", InventoryConfig.CODEC), (SecureWarpsConfig o, InventoryConfig i) -> o.inventory = i, o -> o.inventory).add()
          .append(new KeyedCodec<HttpClientConfig>("HttpClient", HttpClientConfig.CODEC), (SecureWarpsConfig o, HttpClientConfig i) -> o.httpClient = i, o -> o.httpClient).add()
          .append(new KeyedCodec<HttpServerConfig>("HttpServer", HttpServerConfig.CODEC), (SecureWarpsConfig o, HttpServerConfig i) -> o.httpServer = i, o -> o.httpServer).add()
          .append(new KeyedCodec<ServerConfig>("Server", ServerConfig.CODEC), (SecureWarpsConfig o, ServerConfig i) -> o.server = i, o -> o.server).add()
          .append(new KeyedCodec<PermissionsConfig>("Permissions", PermissionsConfig.CODEC), (SecureWarpsConfig o, PermissionsConfig i) -> o.permissions = i, o -> o.permissions).add()
          .build();

  private DatabaseConfig database = new DatabaseConfig();
  private InventoryConfig inventory = new InventoryConfig();
  private HttpClientConfig httpClient = new HttpClientConfig();
  private HttpServerConfig httpServer = new HttpServerConfig();
  private ServerConfig server = new ServerConfig();
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

  public ServerConfig getServer() {
    return server;
  }

  public PermissionsConfig getPermissions() {
    return permissions;
  }
}
