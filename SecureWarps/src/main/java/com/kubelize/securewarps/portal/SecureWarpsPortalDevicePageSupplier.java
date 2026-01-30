package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.portals.components.PortalDeviceConfig;
import com.hypixel.hytale.builtin.portals.ui.PortalDevicePageSupplier;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class SecureWarpsPortalDevicePageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
  private final PortalDevicePageSupplier delegate = new PortalDevicePageSupplier();
  private volatile boolean initialized;

  @Override
  public CustomUIPage tryCreate(Ref<EntityStore> entityRef,
                                ComponentAccessor<EntityStore> accessor,
                                PlayerRef playerRef,
                                InteractionContext context) {
    if (playerRef == null) {
      return null;
    }
    Ref<EntityStore> playerEntityRef = playerRef.getReference();
    if (playerEntityRef == null) {
      return null;
    }
    Player player = accessor.getComponent(playerEntityRef, Player.getComponentType());
    if (player == null) {
      return null;
    }
    if (!player.hasPermission(PortalRuntime.permissions().getAdmin())) {
      playerRef.sendMessage(Message.raw("You do not have permission to configure this portal."));
      return null;
    }

    initSupplierConfig();
    return delegate.tryCreate(entityRef, accessor, playerRef, context);
  }

  private void initSupplierConfig() {
    if (initialized) {
      return;
    }
    PortalDeviceConfig config = new PortalDeviceConfig();
    setField(PortalDeviceConfig.class, config, "returnBlock", "Portal_Return");
    setField(PortalDevicePageSupplier.class, delegate, "config", config);
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
