package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.portals.components.PortalDeviceConfig;
import com.hypixel.hytale.builtin.portals.ui.PortalDevicePageSupplier;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class SecureWarpsPortalDeviceAdminInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsPortalDeviceAdminInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsPortalDeviceAdminInteraction.class, SecureWarpsPortalDeviceAdminInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Open Portal_Device UI for admins")
          .build();

  private final PortalDevicePageSupplier supplier = new PortalDevicePageSupplier();
  private volatile boolean initialized;

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
      playerRef.sendMessage(Message.raw("You do not have permission to configure this portal."));
      return;
    }

    initSupplierConfig();

    PageManager pageManager = player.getPageManager();
    if (pageManager.getCustomPage() != null) {
      return;
    }

    CustomUIPage page = supplier.tryCreate(entityRef, commandBuffer, playerRef, context);
    if (page != null) {
      pageManager.openCustomPage(entityRef, commandBuffer.getStore(), page);
    }
  }

  private void initSupplierConfig() {
    if (initialized) {
      return;
    }
    PortalDeviceConfig config = new PortalDeviceConfig();
    setField(PortalDeviceConfig.class, config, "returnBlock", "Portal_Return");
    setField(PortalDevicePageSupplier.class, supplier, "config", config);
    initialized = true;
  }

  private void setField(Class<?> type, Object instance, String fieldName, Object value) {
    try {
      Field field = type.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(instance, value);
    } catch (Exception e) {
      PortalRuntime.logger().at(Level.WARNING).withCause(e)
          .log("[SecureWarps] Failed to set portal device config field: " + fieldName);
    }
  }
}
