package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.page.TeleporterSettingsPage;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.protocol.BlockPosition;

public class SecureWarpsTeleporterSettingsPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
  private final boolean create = true;
  private final TeleporterSettingsPage.Mode mode = TeleporterSettingsPage.Mode.FULL;
  private final String activeState = "Active";

  @Override
  public CustomUIPage tryCreate(Ref<EntityStore> entityRef,
                                ComponentAccessor<EntityStore> accessor,
                                PlayerRef playerRef,
                                InteractionContext context) {
    BlockPosition target = context.getTargetBlock();
    if (target == null) {
      return null;
    }
    Store<EntityStore> store = entityRef.getStore();
    World world = ((EntityStore) store.getExternalData()).getWorld();
    ChunkStore chunkStore = world.getChunkStore();
    Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(target.x, target.z));
    BlockComponentChunk blockChunk = null;
    if (chunkRef != null) {
      blockChunk = chunkStore.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());
    }
    if (blockChunk == null) {
      return null;
    }
    int index = ChunkUtil.indexBlockInColumn(target.x, target.y, target.z);
    Ref<ChunkStore> blockRef = blockChunk.getEntityReference(index);
    if (blockRef == null || !blockRef.isValid()) {
      if (!create) {
        return null;
      }
      Holder<ChunkStore> holder = ChunkStore.REGISTRY.newHolder();
      holder.putComponent(BlockModule.BlockStateInfo.getComponentType(), new BlockModule.BlockStateInfo(index, chunkRef));
      holder.ensureComponent(Teleporter.getComponentType());
      blockRef = chunkStore.getStore().addEntity(holder, AddReason.SPAWN);
    }
    TeleporterWarpCache.seedTeleportPlugin();
    return new SecureWarpsTeleporterSettingsPage(playerRef, blockRef, mode, activeState);
  }
}
