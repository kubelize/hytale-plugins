package com.kubelize.securewarps.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class GameThread {
  private GameThread() {}

  public static void run(PlayerRef playerRef, Runnable action) {
    if (playerRef == null) {
      action.run();
      return;
    }
    Ref<EntityStore> ref = playerRef.getReference();
    if (ref == null || !ref.isValid()) {
      action.run();
      return;
    }
    EntityStore entityStore = ref.getStore().getExternalData();
    World world = entityStore == null ? null : entityStore.getWorld();
    if (world == null) {
      action.run();
      return;
    }
    if (world.isInThread()) {
      action.run();
    } else {
      world.execute(action);
    }
  }

  public static void run(CommandContext context, Runnable action) {
    if (context != null && context.isPlayer()) {
      Ref<EntityStore> ref = context.senderAsPlayerRef();
      if (ref != null && ref.isValid()) {
        EntityStore entityStore = ref.getStore().getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world != null) {
          if (world.isInThread()) {
            action.run();
          } else {
            world.execute(action);
          }
          return;
        }
      }
    }
    action.run();
  }
}
