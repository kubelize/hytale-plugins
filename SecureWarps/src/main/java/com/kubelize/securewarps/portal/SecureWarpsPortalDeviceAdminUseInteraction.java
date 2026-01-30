package com.kubelize.securewarps.portal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SecureWarpsPortalDeviceAdminUseInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsPortalDeviceAdminUseInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsPortalDeviceAdminUseInteraction.class, SecureWarpsPortalDeviceAdminUseInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Open SecureWarps portal config (admin + portal key only)")
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
    new SecureWarpsPortalDeviceConfigInteraction()
        .firstRun(interactionType, context, cooldownHandler);
  }
}
