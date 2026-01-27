package com.kubelize.securewarps.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kubelize.securewarps.db.DatabaseManager;
import com.kubelize.securewarps.db.WarpRecord;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WarpGoCommand extends AbstractPlayerCommand {
  private final DatabaseManager databaseManager;
  @Nonnull
  private final RequiredArg<String> nameArg = this.withRequiredArg("name", "Warp name", (ArgumentType) ArgTypes.STRING);

  public WarpGoCommand(DatabaseManager databaseManager, String permission) {
    super("go", "Teleport to a warp");
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

    databaseManager.getWarpByName(name)
        .thenCompose(result -> resolveWarpWorld(result))
        .thenAccept(result -> handleTeleport(store, ref, playerRef, name, result))
        .exceptionally(err -> {
          playerRef.sendMessage(Message.raw("Failed to load warp: " + name));
          return null;
        });
  }

  private CompletableFuture<Optional<WarpRecord>> resolveWarpWorld(Optional<WarpRecord> warp) {
    if (warp.isEmpty()) {
      return CompletableFuture.completedFuture(warp);
    }

    WarpRecord record = warp.get();
    World target = Universe.get().getWorld(record.worldId());
    if (target != null) {
      return CompletableFuture.completedFuture(warp);
    }

    if (!Universe.get().isWorldLoadable(record.worldId())) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    return Universe.get().loadWorld(record.worldId()).thenApply(world -> warp);
  }

  private void handleTeleport(Store<EntityStore> store,
                              Ref<EntityStore> ref,
                              PlayerRef playerRef,
                              String name,
                              Optional<WarpRecord> result) {
    if (result.isEmpty()) {
      playerRef.sendMessage(Message.raw("Warp not found or world unavailable: " + name));
      return;
    }

    WarpRecord warp = result.get();
    World targetWorld = Universe.get().getWorld(warp.worldId());
    if (targetWorld == null) {
      playerRef.sendMessage(Message.raw("Warp world not loaded: " + warp.worldId()));
      return;
    }

    Vector3d position = new Vector3d(warp.x(), warp.y(), warp.z());
    Vector3f rotation = new Vector3f(warp.rotX(), warp.rotY(), warp.rotZ());
    Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
    store.putComponent(ref, Teleport.getComponentType(), teleport);
    playerRef.sendMessage(Message.raw("Teleported to warp: " + name));
  }
}
