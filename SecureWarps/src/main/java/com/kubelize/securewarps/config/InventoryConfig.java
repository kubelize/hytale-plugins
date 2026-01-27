package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class InventoryConfig {
  public static final BuilderCodec<InventoryConfig> CODEC =
      BuilderCodec.builder(InventoryConfig.class, InventoryConfig::new)
          .append(new KeyedCodec("Enabled", Codec.BOOLEAN), (o, i) -> o.enabled = i, o -> o.enabled).add()
          .append(new KeyedCodec("LoadOnReady", Codec.BOOLEAN), (o, i) -> o.loadOnReady = i, o -> o.loadOnReady).add()
          .append(new KeyedCodec("SaveOnDisconnect", Codec.BOOLEAN), (o, i) -> o.saveOnDisconnect = i, o -> o.saveOnDisconnect).add()
          .append(new KeyedCodec("SavePeriodically", Codec.BOOLEAN), (o, i) -> o.savePeriodically = i, o -> o.savePeriodically).add()
          .append(new KeyedCodec("SaveIntervalSeconds", Codec.INTEGER), (o, i) -> o.saveIntervalSeconds = i, o -> o.saveIntervalSeconds).add()
          .build();

  private boolean enabled = true;
  private boolean loadOnReady = true;
  private boolean saveOnDisconnect = true;
  private boolean savePeriodically = true;
  private int saveIntervalSeconds = 60;

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isLoadOnReady() {
    return loadOnReady;
  }

  public boolean isSaveOnDisconnect() {
    return saveOnDisconnect;
  }

  public boolean isSavePeriodically() {
    return savePeriodically;
  }

  public int getSaveIntervalSeconds() {
    return saveIntervalSeconds;
  }
}
