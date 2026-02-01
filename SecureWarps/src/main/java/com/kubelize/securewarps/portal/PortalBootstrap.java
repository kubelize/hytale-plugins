package com.kubelize.securewarps.portal;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
public final class PortalBootstrap {
  private PortalBootstrap() {
  }

  public static void register(JavaPlugin plugin) {
    plugin.getCodecRegistry(Interaction.CODEC)
        .register("SecureWarpsTeleporter", SecureWarpsTeleporterInteraction.class, SecureWarpsTeleporterInteraction.CODEC)
        .register("SecureWarpsTeleporterUse", SecureWarpsTeleporterUseInteraction.class, SecureWarpsTeleporterUseInteraction.CODEC)
        .register("SecureWarpsPortalDeviceAdminUse", SecureWarpsPortalDeviceAdminUseInteraction.class, SecureWarpsPortalDeviceAdminUseInteraction.CODEC)
        .register("SecureWarpsPortalDeviceConfig", SecureWarpsPortalDeviceConfigInteraction.class, SecureWarpsPortalDeviceConfigInteraction.CODEC)
        .register("SecureWarpsPortalDeviceTeleport", SecureWarpsPortalDeviceTeleportInteraction.class, SecureWarpsPortalDeviceTeleportInteraction.CODEC);
    OpenCustomUIInteraction.registerCustomPageSupplier(
        plugin,
        SecureWarpsTeleporterSettingsPageSupplier.class,
        "Teleporter",
        new SecureWarpsTeleporterSettingsPageSupplier()
    );
  }
}
