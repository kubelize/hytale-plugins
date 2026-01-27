package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PermissionsConfig {
  public static final BuilderCodec<PermissionsConfig> CODEC =
      BuilderCodec.builder(PermissionsConfig.class, PermissionsConfig::new)
          .append(new KeyedCodec("Set", Codec.STRING), (o, i) -> o.set = i, o -> o.set).add()
          .append(new KeyedCodec("Use", Codec.STRING), (o, i) -> o.use = i, o -> o.use).add()
          .append(new KeyedCodec("List", Codec.STRING), (o, i) -> o.list = i, o -> o.list).add()
          .append(new KeyedCodec("Delete", Codec.STRING), (o, i) -> o.delete = i, o -> o.delete).add()
          .append(new KeyedCodec("Admin", Codec.STRING), (o, i) -> o.admin = i, o -> o.admin).add()
          .build();

  private String set = "securewarps.set";
  private String use = "securewarps.use";
  private String list = "securewarps.list";
  private String delete = "securewarps.delete";
  private String admin = "securewarps.admin";

  public String getSet() {
    return set;
  }

  public String getUse() {
    return use;
  }

  public String getList() {
    return list;
  }

  public String getDelete() {
    return delete;
  }

  public String getAdmin() {
    return admin;
  }
}
