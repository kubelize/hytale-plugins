package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HttpServerConfig {
  public static final BuilderCodec<HttpServerConfig> CODEC =
      BuilderCodec.builder(HttpServerConfig.class, HttpServerConfig::new)
          .append(new KeyedCodec("Enabled", Codec.BOOLEAN), (o, i) -> o.enabled = i, o -> o.enabled).add()
          .append(new KeyedCodec("BindHost", Codec.STRING), (o, i) -> o.bindHost = i, o -> o.bindHost).add()
          .append(new KeyedCodec("Port", Codec.INTEGER), (o, i) -> o.port = i, o -> o.port).add()
          .append(new KeyedCodec("KeyStorePath", Codec.STRING), (o, i) -> o.keyStorePath = i, o -> o.keyStorePath).add()
          .append(new KeyedCodec("KeyStorePassword", Codec.STRING), (o, i) -> o.keyStorePassword = i, o -> o.keyStorePassword).add()
          .append(new KeyedCodec("TrustStorePath", Codec.STRING), (o, i) -> o.trustStorePath = i, o -> o.trustStorePath).add()
          .append(new KeyedCodec("TrustStorePassword", Codec.STRING), (o, i) -> o.trustStorePassword = i, o -> o.trustStorePassword).add()
          .append(new KeyedCodec("RequireClientCert", Codec.BOOLEAN), (o, i) -> o.requireClientCert = i, o -> o.requireClientCert).add()
          .append(new KeyedCodec("SharedSecret", Codec.STRING), (o, i) -> o.sharedSecret = i, o -> o.sharedSecret).add()
          .append(new KeyedCodec("SharedSecretEnv", Codec.STRING), (o, i) -> o.sharedSecretEnv = i, o -> o.sharedSecretEnv).add()
          .append(new KeyedCodec("MaxBodyBytes", Codec.INTEGER), (o, i) -> o.maxBodyBytes = i, o -> o.maxBodyBytes).add()
          .append(new KeyedCodec("ClockSkewSeconds", Codec.INTEGER), (o, i) -> o.clockSkewSeconds = i, o -> o.clockSkewSeconds).add()
          .append(new KeyedCodec("ThreadPoolSize", Codec.INTEGER), (o, i) -> o.threadPoolSize = i, o -> o.threadPoolSize).add()
          .build();

  private boolean enabled = false;
  private String bindHost = "0.0.0.0";
  private int port = 8443;

  private String keyStorePath = "";
  private String keyStorePassword = "";
  private String trustStorePath = "";
  private String trustStorePassword = "";
  private boolean requireClientCert = false;

  private String sharedSecret = "";
  private String sharedSecretEnv = "";

  private int maxBodyBytes = 1024 * 1024;
  private int clockSkewSeconds = 60;
  private int threadPoolSize = 4;

  public boolean isEnabled() {
    return enabled;
  }

  public String getBindHost() {
    return bindHost;
  }

  public int getPort() {
    return port;
  }

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public String getTrustStorePath() {
    return trustStorePath;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public boolean isRequireClientCert() {
    return requireClientCert;
  }

  public String getSharedSecret() {
    return sharedSecret;
  }

  public String getSharedSecretEnv() {
    return sharedSecretEnv;
  }

  public int getMaxBodyBytes() {
    return maxBodyBytes;
  }

  public int getClockSkewSeconds() {
    return clockSkewSeconds;
  }

  public int getThreadPoolSize() {
    return threadPoolSize;
  }
}
