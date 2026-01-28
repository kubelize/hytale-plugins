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
import com.kubelize.securewarps.util.ErrorUtil;
import com.kubelize.securewarps.util.GameThread;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RemoteWarpSetCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;
  @Nonnull
  private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Warp name", (ArgumentType) ArgTypes.STRING);
  @Nonnull
  private final RequiredArg<String> hostArg = this.withRequiredArg("host", "Target host", (ArgumentType) ArgTypes.STRING);
  @Nonnull
  private final RequiredArg<Integer> portArg = this.withRequiredArg("port", "Target port", (ArgumentType) ArgTypes.INTEGER);

  public RemoteWarpSetCommand(DatabaseManager databaseManager, String permission) {
    super("set", "Assign a remote server to an existing warp");
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
    String host = hostArg.get(commandContext);
    Integer port = portArg.get(commandContext);
    if (name == null || name.isBlank() || host == null || host.isBlank() || port == null || port <= 0) {
      commandContext.sendMessage(Message.raw("Usage: /rwarp set <name> <host> <port>"));
      return;
    }

    databaseManager.updateWarpServer(name, host, port)
        .thenAccept(updated -> GameThread.run(playerRef, () -> {
          if (updated) {
            playerRef.sendMessage(Message.raw("Warp updated: " + name + " -> " + host + ":" + port));
            return;
          }
          createWarpFromPlayer(store, ref, playerRef, world, name, host, port);
        }))
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to update warp server " + name, ErrorUtil.rootCause(err));
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err) ? "Warp update timed out." : "Failed to update warp server.")));
          return null;
        });
  }

  private void createWarpFromPlayer(Store<EntityStore> store,
                                    Ref<EntityStore> ref,
                                    PlayerRef playerRef,
                                    World world,
                                    String name,
                                    String host,
                                    int port) {
    TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
    UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
    if (transformComponent == null || uuidComponent == null) {
      playerRef.sendMessage(Message.raw("Failed to read player position to create warp."));
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
        host,
        port
    );

    databaseManager.saveWarp(warp)
        .thenRun(() -> GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(
            "Warp created: " + name + " -> " + host + ":" + port))))
        .exceptionally(err -> {
          Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to create warp " + name, ErrorUtil.rootCause(err));
          GameThread.run(playerRef, () -> playerRef.sendMessage(Message.raw(ErrorUtil.isTimeout(err) ? "Warp create timed out." : "Failed to create warp.")));
          return null;
        });
  }
}
