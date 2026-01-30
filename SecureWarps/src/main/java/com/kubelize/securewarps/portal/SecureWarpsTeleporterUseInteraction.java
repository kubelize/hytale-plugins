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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;

public class SecureWarpsTeleporterUseInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsTeleporterUseInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsTeleporterUseInteraction.class, SecureWarpsTeleporterUseInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Open teleporter settings or SecureWarps selector")
          .build();

  private final SecureWarpsTeleporterSettingsPageSupplier vanillaSupplier = new SecureWarpsTeleporterSettingsPageSupplier();

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

    PageManager pageManager = player.getPageManager();
    if (pageManager.getCustomPage() != null) {
      return;
    }

    if (player.hasPermission(PortalRuntime.permissions().getAdmin()) && isPortalKey(context.getHeldItem())) {
      openSecureWarpsSelector(commandBuffer, playerRef, player, context);
      return;
    }

    CustomUIPage page = vanillaSupplier.tryCreate(entityRef, commandBuffer.getStore(), playerRef, context);
    if (page != null) {
      pageManager.openCustomPage(entityRef, commandBuffer.getStore(), page);
    }
  }

  private void openSecureWarpsSelector(CommandBuffer<EntityStore> commandBuffer,
                                       PlayerRef playerRef,
                                       Player player,
                                       InteractionContext context) {
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
      teleporter = new Teleporter();
      chunkStore.putComponent(blockEntityRef, Teleporter.getComponentType(), teleporter);
    }
    Teleporter teleporterRef = teleporter;

    Map<String, Warp> warpMap = new HashMap<>(TeleporterWarpCache.getSnapshot());
    if (warpMap.isEmpty()) {
      playerRef.sendMessage(Message.raw("No warps available to assign."));
      return;
    }

    CustomUIPage page = new WarpListPage(playerRef, warpMap, selected -> {
      String warpName = selected == null ? "" : selected.trim();
      if (warpName.isEmpty()) {
        playerRef.sendMessage(Message.raw("No warp selected."));
        return;
      }
      teleporterRef.setWarp(warpName);
      PortalBlockStateUtil.setInteractionState(world, base, "Active");
      playerRef.sendMessage(Message.raw("Teleporter configured: " + warpName));
    });

    player.getPageManager().openCustomPage(playerRef.getReference(), commandBuffer.getStore(), page);
  }

  private boolean isPortalKey(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    String itemId = stack.getItemId();
    return itemId != null && itemId.toLowerCase().contains("portalkey");
  }
}
