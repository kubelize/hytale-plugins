package com.kubelize.securewarps.inventory;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.server.core.inventory.Inventory;
import org.bson.BsonDocument;
import org.bson.BsonValue;

public final class InventorySnapshotUtil {
  private InventorySnapshotUtil() {}

  public static BsonDocument encode(Inventory inventory) {
    BsonValue encoded = Inventory.CODEC.encode(inventory, ExtraInfo.THREAD_LOCAL.get());
    return new BsonDocument("inventory", encoded);
  }

  public static Inventory decode(BsonDocument document) {
    BsonDocument inventoryDoc = document.getDocument("inventory");
    return Inventory.CODEC.decode(inventoryDoc, ExtraInfo.THREAD_LOCAL.get());
  }
}
