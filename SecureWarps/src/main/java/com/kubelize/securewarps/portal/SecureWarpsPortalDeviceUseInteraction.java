package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.portals.components.PortalDeviceConfig;
import com.hypixel.hytale.builtin.portals.ui.PortalDevicePageSupplier;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.logging.Level;

public class SecureWarpsPortalDeviceUseInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SecureWarpsPortalDeviceUseInteraction> CODEC =
      BuilderCodec.builder(SecureWarpsPortalDeviceUseInteraction.class, SecureWarpsPortalDeviceUseInteraction::new, SimpleInstantInteraction.CODEC)
          .documentation("Open portal device UI (admin SecureWarps, others vanilla)")
          .build();

  private static final String PORTAL_DEVICE_ON_STATE = "Active";
  private static final String PORTAL_DEVICE_SPAWNING_STATE = "Spawning";
  private static final String PORTAL_DEVICE_OFF_STATE = "default";
  private static final String PORTAL_DEVICE_RETURN_BLOCK = "Portal_Return";

  private final PortalDevicePageSupplier vanillaSupplier = createVanillaSupplier();

  @Override
  protected void firstRun(com.hypixel.hytale.protocol.InteractionType interactionType,
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
      // Admins configure SecureWarps target.
      new SecureWarpsPortalDeviceConfigInteraction()
          .firstRun(interactionType, context, cooldownHandler);
      return;
    }

    try {
      CustomUIPage page = vanillaSupplier.tryCreate(entityRef, commandBuffer.getStore(), playerRef, context);
      if (page != null) {
        pageManager.openCustomPage(entityRef, commandBuffer.getStore(), page);
      }
    } catch (RuntimeException e) {
      PortalRuntime.logger().at(Level.WARNING).withCause(e).log("[SecureWarps] Failed to open vanilla portal UI.");
    }
  }

  private static PortalDevicePageSupplier createVanillaSupplier() {
    PortalDevicePageSupplier supplier = new PortalDevicePageSupplier();
    try {
      PortalDeviceConfig config = new PortalDeviceConfig();
      setConfigField(config, "onState", PORTAL_DEVICE_ON_STATE);
      setConfigField(config, "spawningState", PORTAL_DEVICE_SPAWNING_STATE);
      setConfigField(config, "offState", PORTAL_DEVICE_OFF_STATE);
      setConfigField(config, "returnBlock", PORTAL_DEVICE_RETURN_BLOCK);
      setSupplierConfig(supplier, config);
    } catch (ReflectiveOperationException e) {
      PortalRuntime.logger().at(Level.WARNING).withCause(e).log("[SecureWarps] Failed to init PortalDevicePageSupplier config.");
    }
    return supplier;
  }

  private static void setConfigField(PortalDeviceConfig config, String fieldName, String value)
      throws ReflectiveOperationException {
    java.lang.reflect.Field field = PortalDeviceConfig.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(config, value);
  }

  private static void setSupplierConfig(PortalDevicePageSupplier supplier, PortalDeviceConfig config)
      throws ReflectiveOperationException {
    java.lang.reflect.Field field = PortalDevicePageSupplier.class.getDeclaredField("config");
    field.setAccessible(true);
    field.set(supplier, config);
  }

  private static boolean isPortalKey(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return false;
    }
    String itemId = stack.getItemId();
    return itemId != null && itemId.toLowerCase().contains("portalkey");
  }
}
