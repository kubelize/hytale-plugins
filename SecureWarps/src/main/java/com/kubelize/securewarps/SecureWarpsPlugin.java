package com.kubelize.securewarps;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.kubelize.securewarps.commands.RemoteWarpCommand;
import com.kubelize.securewarps.commands.WarpCommand;
import com.kubelize.securewarps.commands.inventory.InventoryAdminCommand;
import com.kubelize.securewarps.config.SecureWarpsConfig;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.inventory.InventorySyncService;
import com.kubelize.securewarps.net.SecureHttpClient;
import com.kubelize.securewarps.net.SecureHttpServer;
import com.kubelize.securewarps.teleport.CrossServerWarpListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class SecureWarpsPlugin extends JavaPlugin {
  private Config<SecureWarpsConfig> config;
  private DatabaseManager databaseManager;
  private SecureHttpClient httpClient;
  private SecureHttpServer httpServer;
  private InventorySyncService inventorySyncService;
  private CrossServerWarpListener crossServerWarpListener;

  public SecureWarpsPlugin(JavaPluginInit init) {
    super(init);
    this.config = this.withConfig("SecureWarps", SecureWarpsConfig.CODEC);
  }

  @Override
  protected void setup() {
    try {
      saveDefaultConfigIfNeeded();
    } catch (Exception e) {
      getLogger().at(Level.SEVERE).withCause(e).log("[SecureWarps] Failed to write default config.");
      return;
    }

    SecureWarpsConfig cfg;
    try {
      cfg = this.config.get();
    } catch (Exception e) {
      getLogger().at(Level.SEVERE).withCause(e).log("[SecureWarps] Failed to load config.");
      return;
    }
    this.databaseManager = new DatabaseManager(cfg.getDatabase(), getLogger());

    try {
      this.databaseManager.start();
    } catch (Exception e) {
      getLogger().at(Level.SEVERE).withCause(e).log("[SecureWarps] Failed to initialize database manager. Plugin will be inactive.");
      this.databaseManager = null;
      return;
    }

    this.inventorySyncService = new InventorySyncService(cfg.getInventory(), databaseManager, getLogger());
    this.inventorySyncService.start();
    getEventRegistry().registerGlobal(PlayerReadyEvent.class, inventorySyncService::onPlayerReady);
    getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, inventorySyncService::onPlayerDisconnect);

    this.crossServerWarpListener = new CrossServerWarpListener(databaseManager);
    getEventRegistry().registerGlobal(PlayerSetupConnectEvent.class, crossServerWarpListener::onSetupConnect);
    getEventRegistry().registerGlobal(PlayerReadyEvent.class, crossServerWarpListener::onPlayerReady);

    if (cfg.getHttpClient().getBaseUrl() != null && !cfg.getHttpClient().getBaseUrl().isBlank()) {
      try {
        this.httpClient = new SecureHttpClient(cfg.getHttpClient());
        getLogger().at(Level.INFO).log("[SecureWarps] HTTP client initialized with TLS support.");
      } catch (Exception e) {
        getLogger().at(Level.WARNING).withCause(e).log("[SecureWarps] Failed to initialize HTTP client.");
      }
    }

    if (cfg.getHttpServer().isEnabled()) {
      try {
        this.httpServer = new SecureHttpServer(cfg.getHttpServer(), databaseManager, getLogger());
        this.httpServer.start();
      } catch (Exception e) {
        getLogger().at(Level.SEVERE).withCause(e).log("[SecureWarps] Failed to initialize HTTP server.");
      }
    }

    getCommandRegistry().registerCommand(new WarpCommand(databaseManager, cfg.getServer(), cfg.getPermissions()));
    getCommandRegistry().registerCommand(new RemoteWarpCommand(databaseManager, cfg.getServer(), cfg.getPermissions()));
    getCommandRegistry().registerCommand(new InventoryAdminCommand(databaseManager, cfg.getPermissions()));

    getLogger().at(Level.INFO).log("[SecureWarps] SecureWarps enabled.");
  }

  @Override
  public void shutdown() {
    if (this.httpServer != null) {
      this.httpServer.close();
    }
    if (this.inventorySyncService != null) {
      this.inventorySyncService.close();
    }
    if (this.databaseManager != null) {
      this.databaseManager.close();
    }
  }

  private void saveDefaultConfigIfNeeded() {
    Path dataDir = this.getDataDirectory();
    Path configPath = dataDir.resolve("SecureWarps.json");
    try {
      Files.createDirectories(dataDir);
      if (Files.notExists(configPath)) {
        this.config.save();
        getLogger().at(Level.INFO).log("[SecureWarps] Wrote default config to " + configPath);
      }
    } catch (Exception e) {
      getLogger().at(Level.WARNING).withCause(e).log("[SecureWarps] Failed to write config to " + configPath);
    }
  }
}
