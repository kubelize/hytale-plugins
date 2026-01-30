package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SecureWarpsTeleporterInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsTeleporterInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsTeleporterInteraction.class, SecureWarpsTeleporterInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Teleport via SecureWarps using Teleporter warp name")
          .build();

  private PortalWarpService warpService;

  @Override
  protected void firstRun(InteractionType interactionType,
                          InteractionContext context,
                          CooldownHandler cooldownHandler) {
    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
    if (commandBuffer == null) {
      return;
    }

    Ref<EntityStore> entityRef = context.getEntity();
    PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
    if (playerRef == null) {
      return;
    }
    Ref<EntityStore> playerEntityRef = playerRef.getReference();
    if (playerEntityRef == null) {
      return;
    }
    Player player = commandBuffer.getComponent(playerEntityRef, Player.getComponentType());
    if (player == null) {
      return;
    }
    if (!player.hasPermission(PortalRuntime.permissions().getUse())) {
      playerRef.sendMessage(Message.raw("You do not have permission to use teleporters."));
      return;
    }

    BlockPosition target = context.getTargetBlock();
    if (target == null) {
      return;
    }
    World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
    BlockPosition base = world.getBaseBlock(target);
    Ref<ChunkStore> blockEntityRef = BlockModule.getBlockEntity(world, base.x, base.y, base.z);
    if (blockEntityRef == null) {
      playerRef.sendMessage(Message.raw("Teleporter not configured."));
      return;
    }

    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    Teleporter teleporter = chunkStore.getComponent(blockEntityRef, Teleporter.getComponentType());
    if (teleporter == null) {
      playerRef.sendMessage(Message.raw("Teleporter not configured."));
      return;
    }

    String warpName = teleporter.getWarp();
    if (warpName == null || warpName.isBlank()) {
      playerRef.sendMessage(Message.raw("Teleporter not configured."));
      return;
    }

    getWarpService().teleport(commandBuffer.getStore(), playerEntityRef, playerRef, player, warpName.trim());
  }

  private PortalWarpService getWarpService() {
    if (warpService == null) {
      warpService = new PortalWarpService(PortalRuntime.database(), PortalRuntime.serverConfig());
    }
    return warpService;
  }
}
