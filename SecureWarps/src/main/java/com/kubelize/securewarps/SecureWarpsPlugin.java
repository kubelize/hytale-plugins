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
import com.kubelize.securewarps.portal.PortalBootstrap;
import com.kubelize.securewarps.portal.PortalRuntime;
import com.kubelize.securewarps.portal.PortalTargetActivationService;
import com.kubelize.securewarps.portal.TeleporterWarpSyncService;
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
  private TeleporterWarpSyncService teleporterWarpSyncService;
  private PortalTargetActivationService portalTargetActivationService;

  public SecureWarpsPlugin(JavaPluginInit init) {
    super(init);
    this.config = this.withConfig("SecureWarps", SecureWarpsConfig.CODEC);
  }

  @Override
  protected void setup() {
    try {
      saveDefaultConfigIfNeeded();
      exportAssetPackIfNeeded();
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

    PortalRuntime.init(databaseManager, cfg.getServer(), cfg.getPermissions(), getLogger());
    PortalBootstrap.register(this);
    this.teleporterWarpSyncService = new TeleporterWarpSyncService(databaseManager);
    this.teleporterWarpSyncService.start();
    this.portalTargetActivationService = new PortalTargetActivationService(databaseManager, getLogger());
    this.portalTargetActivationService.start();

    this.inventorySyncService = new InventorySyncService(cfg.getInventory(), databaseManager, getLogger());
    this.inventorySyncService.start();
    getEventRegistry().registerGlobal(PlayerReadyEvent.class, inventorySyncService::onPlayerReady);
    getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, inventorySyncService::onPlayerDisconnect);

    this.crossServerWarpListener = new CrossServerWarpListener(databaseManager);
    getEventRegistry().registerGlobal(PlayerSetupConnectEvent.class, crossServerWarpListener::onSetupConnect);
    getEventRegistry().registerGlobal(PlayerReadyEvent.class, crossServerWarpListener::onPlayerReady);
    getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, crossServerWarpListener::onPlayerDisconnect);

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
    if (this.teleporterWarpSyncService != null) {
      this.teleporterWarpSyncService.close();
    }
    if (this.portalTargetActivationService != null) {
      this.portalTargetActivationService.close();
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

  private void exportAssetPackIfNeeded() {
    Path dataDir = this.getDataDirectory();
    try {
      Files.createDirectories(dataDir);
    } catch (Exception e) {
      getLogger().at(Level.WARNING).withCause(e).log("[SecureWarps] Failed to create data directory for asset pack.");
      return;
    }

    copyResourceToFile("/assetpack-manifest.json", dataDir.resolve("manifest.json"), false);
    copyResourceToFile(
        "/Server/Item/Items/Electrum/Portal/Teleporter.json",
        dataDir.resolve("Server/Item/Items/Electrum/Portal/Teleporter.json"),
        true
    );
    copyResourceToFile(
        "/Server/Item/Items/Portal/SecureWarp_Portal_Device.json",
        dataDir.resolve("Server/Item/Items/Portal/SecureWarp_Portal_Device.json"),
        true
    );
  }

  private void copyResourceToFile(String resourcePath, Path target, boolean replace) {
    try (var in = SecureWarpsPlugin.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        getLogger().at(Level.WARNING).log("[SecureWarps] Missing asset resource: " + resourcePath);
        return;
      }
      Files.createDirectories(target.getParent());
      if (!replace && Files.exists(target)) {
        return;
      }
      if (replace) {
        Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } else {
        Files.copy(in, target);
      }
    } catch (Exception e) {
      getLogger().at(Level.WARNING).withCause(e)
          .log("[SecureWarps] Failed to write asset resource: " + resourcePath);
    }
  }
}
