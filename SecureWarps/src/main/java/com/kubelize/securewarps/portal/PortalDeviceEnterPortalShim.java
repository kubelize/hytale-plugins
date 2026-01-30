package com.kubelize.securewarps.portal;

import com.hypixel.hytale.builtin.portals.interactions.EnterPortalInteraction;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class PortalDeviceEnterPortalShim extends EnterPortalInteraction {
  void invoke(World world,
              CommandBuffer<EntityStore> commandBuffer,
              InteractionType interactionType,
              InteractionContext context,
              ItemStack heldItem,
              Vector3i block,
              CooldownHandler cooldownHandler) {
    interactWithBlock(world, commandBuffer, interactionType, context, heldItem, block, cooldownHandler);
  }
}
