package com.kubelize.securewarps.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.config.ServerConfig;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WarpSetCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;
  private final ServerConfig serverConfig;
  @Nonnull
  private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Warp name", CommandArgUtil.typed(ArgTypes.STRING));

  public WarpSetCommand(DatabaseManager databaseManager, ServerConfig serverConfig, String permission) {
    super("set", "Set a warp at your current position");
    this.databaseManager = databaseManager;
    this.serverConfig = serverConfig;
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
        rotation.z,
        serverConfig.getHost().isBlank() ? null : serverConfig.getHost(),
        serverConfig.getPort() <= 0 ? null : serverConfig.getPort()
    );

    databaseManager.saveWarp(warp)
        .thenRun(() -> GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw("Warp saved: " + name))))
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to save warp " + name, ErrorUtil.rootCause(err));
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err) ? "Warp save timed out." : "Failed to save warp: " + name)));
          return null;
        });
  }
}
