package com.kubelize.securewarps.portal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import com.kubelize.securewarps.teleport.CrossServerArrivalGuard;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.logging.Level;

public class SecureWarpsPortalDeviceTeleportInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsPortalDeviceTeleportInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsPortalDeviceTeleportInteraction.class, SecureWarpsPortalDeviceTeleportInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Teleport via SecureWarps from Portal_Device")
          .build();

  private PortalWarpService warpService;
  private final PortalDeviceEnterPortalShim fallbackPortal = new PortalDeviceEnterPortalShim();
  private static final long TELEPORT_DEBOUNCE_MS = 1500;
  private static final ConcurrentHashMap<UUID, Long> LAST_TELEPORT_MS = new ConcurrentHashMap<>();

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
    if (CrossServerArrivalGuard.shouldIgnore(playerRef.getUuid())) {
      return;
    }
    if (isDebounced(playerRef)) {
      return;
    }
    BlockPosition target = context.getTargetBlock();
    if (target == null) {
      return;
    }
    World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
    BlockPosition base = world.getBaseBlock(target);

    PortalRuntime.database().getPortalTarget(world.getName(), base.x, base.y, base.z)
        .thenAccept(result -> GameThread.run(playerRef, () -> handleTeleport(commandBuffer, interactionType, context, playerEntityRef, playerRef, player, world, base, result, cooldownHandler)))
        .exceptionally(err -> {
          PortalRuntime.logger().at(Level.WARNING).withCause(ErrorUtil.rootCause(err))
              .log("[SecureWarps] Failed to load portal target.");
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(
              ErrorUtil.isTimeout(err) ? "Portal lookup timed out." : "Failed to load portal target.")));
          return null;
        });
  }

  private boolean isDebounced(PlayerRef playerRef) {
    UUID uuid = playerRef.getUuid();
    if (uuid == null) {
      return false;
    }
    long now = System.currentTimeMillis();
    Long last = LAST_TELEPORT_MS.put(uuid, now);
    return last != null && (now - last) < TELEPORT_DEBOUNCE_MS;
  }

  private void handleTeleport(CommandBuffer<EntityStore> commandBuffer,
                              InteractionType interactionType,
                              InteractionContext context,
                              Ref<EntityStore> playerEntityRef,
                              PlayerRef playerRef,
                              Player player,
                              World world,
                              BlockPosition base,
                              Optional<String> result,
                              CooldownHandler cooldownHandler) {
    if (result.isEmpty() || result.get() == null || result.get().isBlank()) {
      runVanillaPortal(commandBuffer, interactionType, context, world, base, cooldownHandler);
      return;
    }
    getWarpService().teleport(commandBuffer.getStore(), playerEntityRef, playerRef, player, result.get().trim());
  }

  private PortalWarpService getWarpService() {
    if (warpService == null) {
      warpService = new PortalWarpService(PortalRuntime.database(), PortalRuntime.serverConfig());
    }
    return warpService;
  }

  private void runVanillaPortal(CommandBuffer<EntityStore> commandBuffer,
                                InteractionType interactionType,
                                InteractionContext context,
                                World world,
                                BlockPosition base,
                                CooldownHandler cooldownHandler) {
    ItemStack heldItem = context.getHeldItem();
    Vector3i block = new Vector3i(base.x, base.y, base.z);
    fallbackPortal.invoke(world, commandBuffer, interactionType, context, heldItem, block, cooldownHandler);
  }
}
