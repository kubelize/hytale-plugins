package com.kubelize.securewarps.portal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

final class PortalBlockStateUtil {
  private PortalBlockStateUtil() {
  }

  static void setInteractionState(Ref<ChunkStore> blockRef, String state) {
    if (blockRef == null || state == null || state.isBlank()) {
      return;
    }
    Store<ChunkStore> store = blockRef.getStore();
    BlockModule.BlockStateInfo info = store.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
    if (info == null) {
      return;
    }
    Ref<ChunkStore> chunkRef = info.getChunkRef();
    if (chunkRef == null || !chunkRef.isValid()) {
      return;
    }
    WorldChunk chunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
    if (chunk == null) {
      return;
    }
    int index = info.getIndex();
    int x = ChunkUtil.xFromBlockInColumn(index);
    int y = ChunkUtil.yFromBlockInColumn(index);
    int z = ChunkUtil.zFromBlockInColumn(index);
    setInteractionState(chunk, x, y, z, state);
    info.markNeedsSaving();
  }

  static void setInteractionState(World world, BlockPosition base, String state) {
    if (world == null || base == null || state == null || state.isBlank()) {
      return;
    }
    Ref<ChunkStore> blockRef = com.hypixel.hytale.server.core.modules.block.BlockModule.getBlockEntity(world, base.x, base.y, base.z);
    if (blockRef != null) {
      setInteractionState(blockRef, state);
      return;
    }
    ChunkStore chunkStore = world.getChunkStore();
    Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(base.x, base.z));
    if (chunkRef == null || !chunkRef.isValid()) {
      return;
    }
    Store<ChunkStore> store = chunkStore.getStore();
    WorldChunk chunk = store.getComponent(chunkRef, WorldChunk.getComponentType());
    if (chunk == null) {
      return;
    }
    int index = ChunkUtil.indexBlockInColumn(base.x, base.y, base.z);
    int x = ChunkUtil.xFromBlockInColumn(index);
    int y = ChunkUtil.yFromBlockInColumn(index);
    int z = ChunkUtil.zFromBlockInColumn(index);
    setInteractionState(chunk, x, y, z, state);
  }

  private static void setInteractionState(WorldChunk chunk, int x, int y, int z, String state) {
    BlockType blockType = chunk.getBlockType(x, y, z);
    if (blockType == null) {
      return;
    }
    String currentState = blockType.getStateForBlock(blockType);
    if (state.equals(currentState)) {
      return;
    }
    BlockType target = blockType.getBlockForState(state);
    if (target == null) {
      return;
    }
    chunk.setBlockInteractionState(x, y, z, target, state, true);
  }
}
