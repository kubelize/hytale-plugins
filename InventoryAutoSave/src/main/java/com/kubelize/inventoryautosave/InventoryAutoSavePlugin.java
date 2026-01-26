package com.kubelize.inventoryautosave;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.djctavia.InventoryManagerAPIPlugin;
import dev.djctavia.config.InventoryManagerConfig;
import dev.djctavia.inventory.InventoryStorageManager;
import java.util.logging.Level;

public class InventoryAutoSavePlugin extends JavaPlugin {
  private InventoryStorageManager storageManager;

  public InventoryAutoSavePlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    InventoryManagerAPIPlugin apiPlugin = InventoryManagerAPIPlugin.get();
    if (apiPlugin == null) {
      getLogger().at(Level.WARNING).log("[InventoryAutoSave] InventoryManagerAPIPlugin.get() returned null. Plugin will be inactive.");
      return;
    }

    this.storageManager = apiPlugin.getInventoryStorageManager();
    getLogger().at(Level.INFO).log("[InventoryAutoSave] InventoryAutoSave enabled. Storage manager ready.");
    InventoryManagerConfig config = apiPlugin.getPluginConfig().get();
    if (config != null) {
      getLogger().at(Level.INFO).log("[InventoryAutoSave] InventoryManager config: storageType=" 
          + config.getStorageType() + ", dir=" + config.getStorageDirectory()
          + ", clearOnSave=" + config.isClearInventoryOnSave()
          + ", deleteOnLoad=" + config.isDeleteFileOnLoad());
    }

    getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::onPlayerConnect);
    getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
    getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
  }

  private void onPlayerConnect(PlayerConnectEvent event) {
    if (storageManager == null) {
      return;
    }

    PlayerRef playerRef = event.getPlayerRef();
    getLogger().at(Level.INFO).log("[InventoryAutoSave] PlayerConnectEvent: " + playerRef.getUsername() + " (" + playerRef.getUuid() + ")");
  }

  private void onPlayerReady(PlayerReadyEvent event) {
    if (storageManager == null) {
      return;
    }

    PlayerRef playerRef = event.getPlayer().getPlayerRef();

    getLogger().at(Level.INFO).log("[InventoryAutoSave] Restoring inventory for " + playerRef.getUsername() + " (" + playerRef.getUuid() + ")");
    storageManager.restoreInventory(event.getPlayerRef());
  }

  private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    if (storageManager == null) {
      return;
    }

    PlayerRef playerRef = event.getPlayerRef();
    getLogger().at(Level.INFO).log("[InventoryAutoSave] Saving inventory for " + playerRef.getUsername() + " (" + playerRef.getUuid() + ")");
    Player player = playerRef.getComponent(Player.getComponentType());
    if (player != null) {
      storageManager.saveInventory(playerRef.getUuid(), player.getInventory());
      getLogger().at(Level.INFO).log("[InventoryAutoSave] Saved inventory via UUID + Inventory.");
      return;
    }

    getLogger().at(Level.INFO).log("[InventoryAutoSave] Player component missing, saving via EntityStore ref.");
    storageManager.saveInventory(playerRef.getReference());
  }
}
