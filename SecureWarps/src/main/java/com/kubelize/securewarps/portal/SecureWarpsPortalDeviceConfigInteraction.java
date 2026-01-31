package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.builtin.teleport.WarpListPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class SecureWarpsPortalDeviceConfigInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsPortalDeviceConfigInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsPortalDeviceConfigInteraction.class, SecureWarpsPortalDeviceConfigInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Configure Portal_Device target warp (teleporter UI)")
          .build();

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

    if (!player.hasPermission(PortalRuntime.permissions().getAdmin())) {
      return;
    }

    BlockPosition target = context.getTargetBlock();
    if (target == null) {
      return;
    }
    World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
    BlockPosition base = world.getBaseBlock(target);

    Map<String, Warp> warpMap = new HashMap<>(TeleporterWarpCache.getSnapshot());
    if (warpMap.isEmpty()) {
      playerRef.sendMessage(Message.raw("No warps available to assign."));
      return;
    }

    PageManager pageManager = player.getPageManager();
    if (pageManager.getCustomPage() != null) {
      return;
    }

    CustomUIPage page = new WarpListPage(playerRef, warpMap, selected -> {
      String warpName = selected == null ? "" : selected.trim();
      if (warpName.isEmpty()) {
        playerRef.sendMessage(Message.raw("No warp selected."));
        return;
      }
      savePortalTarget(PortalRuntime.database(), playerRef, world, base, warpName);
    });

    pageManager.openCustomPage(entityRef, commandBuffer.getStore(), page);
  }

  private void savePortalTarget(DatabaseManager databaseManager,
                                PlayerRef playerRef,
                                World world,
                                BlockPosition base,
                                String warpName) {
    databaseManager.savePortalTarget(world.getName(), base.x, base.y, base.z, warpName)
        .thenRun(() -> GameThread.run(playerRef, () -> {
          PortalBlockStateUtil.setInteractionState(world, base, "Active");
          ensureTeleporterWarp(world, base, warpName);
          playerRef.sendMessage(Message.raw("Portal configured to warp: " + warpName));
        }))
        .exceptionally(err -> {
          PortalRuntime.logger().at(Level.WARNING).withCause(ErrorUtil.rootCause(err))
              .log("[SecureWarps] Failed to save portal target.");
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(
              ErrorUtil.isTimeout(err) ? "Portal save timed out." : "Failed to save portal target.")));
          return null;
        });
  }

  private void ensureTeleporterWarp(World world, BlockPosition base, String warpName) {
    Ref<ChunkStore> blockEntityRef = BlockModule.getBlockEntity(world, base.x, base.y, base.z);
    if (blockEntityRef == null) {
      return;
    }
    Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
    Teleporter teleporter = chunkStore.getComponent(blockEntityRef, Teleporter.getComponentType());
    if (teleporter == null) {
      teleporter = new Teleporter();
      chunkStore.putComponent(blockEntityRef, Teleporter.getComponentType(), teleporter);
    }
    teleporter.setWarp(warpName);
  }
}
