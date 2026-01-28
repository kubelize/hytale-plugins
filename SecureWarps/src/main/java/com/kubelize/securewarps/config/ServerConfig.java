package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ServerConfig {
  public static final BuilderCodec<ServerConfig> CODEC =
      BuilderCodec.<ServerConfig>builder(ServerConfig.class, ServerConfig::new)
          .append(new KeyedCodec<String>("Host", Codec.STRING), (ServerConfig o, String i) -> o.host = i, o -> o.host).add()
          .append(new KeyedCodec<Integer>("Port", Codec.INTEGER), (ServerConfig o, Integer i) -> o.port = i, o -> o.port).add()
          .build();

  private String host = "";
  private int port = 0;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }
}
