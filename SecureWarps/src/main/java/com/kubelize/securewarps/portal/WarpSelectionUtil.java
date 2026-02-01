package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.builtin.teleport.WarpListPage;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;

final class WarpSelectionUtil {
  private WarpSelectionUtil() {
  }

  static Target resolveTarget(CommandBuffer<EntityStore> commandBuffer, InteractionContext context) {
    BlockPosition target = context.getTargetBlock();
    if (target == null) {
      return null;
    }
    World world = ((EntityStore) commandBuffer.getExternalData()).getWorld();
    BlockPosition base = world.getBaseBlock(target);
    return new Target(world, base);
  }

  static boolean openWarpSelector(CommandBuffer<EntityStore> commandBuffer,
                                  InteractionContext context,
                                  PlayerRef playerRef,
                                  Player player,
                                  WarpSelectionHandler handler) {
    Target target = resolveTarget(commandBuffer, context);
    if (target == null) {
      return false;
    }
    Map<String, Warp> warpMap = new HashMap<>(TeleporterWarpCache.getSnapshot());
    if (warpMap.isEmpty()) {
      playerRef.sendMessage(Message.raw("No warps available to assign."));
      return false;
    }
    PageManager pageManager = player.getPageManager();
    if (pageManager.getCustomPage() != null) {
      return false;
    }
    CustomUIPage page = new WarpListPage(playerRef, warpMap, selected -> {
      String warpName = selected == null ? "" : selected.trim();
      if (warpName.isEmpty()) {
        playerRef.sendMessage(Message.raw("No warp selected."));
        return;
      }
      handler.onSelect(target, warpName);
    });
    pageManager.openCustomPage(playerRef.getReference(), commandBuffer.getStore(), page);
    return true;
  }

  interface WarpSelectionHandler {
    void onSelect(Target target, String warpName);
  }

  record Target(World world, BlockPosition base) {}
}
