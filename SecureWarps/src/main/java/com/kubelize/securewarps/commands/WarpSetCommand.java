package com.kubelize.securewarps.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WarpSetCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;
  @Nonnull
  private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Warp name", (ArgumentType) ArgTypes.STRING);

  public WarpSetCommand(DatabaseManager databaseManager, String permission) {
    super("set", "Set a warp at your current position");
    this.databaseManager = databaseManager;
    this.requirePermission(permission);
  }

  @Override
  protected void execute(@NonNullDecl CommandContext commandContext,
                         @NonNullDecl Store<EntityStore> store,
                         @NonNullDecl Ref<EntityStore> ref,
                         @NonNullDecl PlayerRef playerRef,
                         @NonNullDecl World world) {
    String name = nameArg.get(commandContext);
    if (name == null || name.isBlank()) {
      playerRef.sendMessage(Message.raw("Warp name is required."));
      return;
    }

    TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    if (transformComponent == null || uuidComponent == null) {
      playerRef.sendMessage(Message.raw("Failed to read player position."));
      return;
    }

    Transform transform = transformComponent.getTransform();
    Vector3f rotation = transform.getRotation();

    WarpRecord warp = new WarpRecord(
        UUID.randomUUID(),
        name,
        uuidComponent.getUuid(),
        world.getName(),
        transform.getPosition().x,
        transform.getPosition().y,
        transform.getPosition().z,
        rotation.x,
        rotation.y,
        rotation.z
    );

    databaseManager.saveWarp(warp)
        .thenRun(() -> playerRef.sendMessage(Message.raw("Warp saved: " + name)))
        .exceptionally(err -> {
          playerRef.sendMessage(Message.raw("Failed to save warp: " + name));
          return null;
        });
  }
}
