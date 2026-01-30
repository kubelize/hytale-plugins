package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.page.TeleporterSettingsPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SecureWarpsTeleporterSettingsPage extends TeleporterSettingsPage {
  private final Ref<ChunkStore> blockRef;
  private final String activeState;

  public SecureWarpsTeleporterSettingsPage(PlayerRef playerRef,
                                           Ref<ChunkStore> blockRef,
                                           Mode mode,
                                           String activeState) {
    super(playerRef, blockRef, mode, activeState);
    this.blockRef = blockRef;
    this.activeState = activeState;
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> entityRef,
                              Store<EntityStore> store,
                              PageEventData data) {
    TeleporterWarpCache.seedTeleportPlugin();
    super.handleDataEvent(entityRef, store, data);
    if (blockRef == null) {
      return;
    }
    Store<ChunkStore> chunkStore = blockRef.getStore();
    Teleporter teleporter = chunkStore.getComponent(blockRef, Teleporter.getComponentType());
    if (teleporter == null) {
      return;
    }
    String warp = teleporter.getWarp();
    if (warp == null || warp.isBlank()) {
      return;
    }
    String state = (activeState == null || activeState.isBlank()) ? "Active" : activeState;
    PortalBlockStateUtil.setInteractionState(blockRef, state);
  }

  @Override
  public void build(Ref<EntityStore> entityRef,
                    com.hypixel.hytale.server.core.ui.builder.UICommandBuilder commands,
                    com.hypixel.hytale.server.core.ui.builder.UIEventBuilder events,
                    Store<EntityStore> store) {
    TeleporterWarpCache.seedTeleportPlugin();
    super.build(entityRef, commands, events, store);
  }
}
