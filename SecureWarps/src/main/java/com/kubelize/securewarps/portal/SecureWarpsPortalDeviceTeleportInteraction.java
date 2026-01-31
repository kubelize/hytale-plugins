package com.kubelize.securewarps.portal;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

public class SecureWarpsPortalDeviceTeleportInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsPortalDeviceTeleportInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsPortalDeviceTeleportInteraction.class, SecureWarpsPortalDeviceTeleportInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Teleport via SecureWarps from Portal_Device")
          .build();

  private final SecureWarpsTeleporterInteraction delegate = new SecureWarpsTeleporterInteraction();

  @Override
  protected void firstRun(InteractionType interactionType,
                          InteractionContext context,
                          CooldownHandler cooldownHandler) {
    delegate.firstRun(interactionType, context, cooldownHandler);
  }
}
