package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HttpServerConfig {
  public static final BuilderCodec<HttpServerConfig> CODEC =
      BuilderCodec.<HttpServerConfig>builder(HttpServerConfig.class, HttpServerConfig::new)
          .append(new KeyedCodec<Boolean>("Enabled", Codec.BOOLEAN), (HttpServerConfig o, Boolean i) -> o.enabled = i, o -> o.enabled).add()
          .append(new KeyedCodec<String>("BindHost", Codec.STRING), (HttpServerConfig o, String i) -> o.bindHost = i, o -> o.bindHost).add()
          .append(new KeyedCodec<Integer>("Port", Codec.INTEGER), (HttpServerConfig o, Integer i) -> o.port = i, o -> o.port).add()
          .append(new KeyedCodec<String>("KeyStorePath", Codec.STRING), (HttpServerConfig o, String i) -> o.keyStorePath = i, o -> o.keyStorePath).add()
          .append(new KeyedCodec<String>("KeyStorePassword", Codec.STRING), (HttpServerConfig o, String i) -> o.keyStorePassword = i, o -> o.keyStorePassword).add()
          .append(new KeyedCodec<String>("TrustStorePath", Codec.STRING), (HttpServerConfig o, String i) -> o.trustStorePath = i, o -> o.trustStorePath).add()
          .append(new KeyedCodec<String>("TrustStorePassword", Codec.STRING), (HttpServerConfig o, String i) -> o.trustStorePassword = i, o -> o.trustStorePassword).add()
          .append(new KeyedCodec<Boolean>("RequireClientCert", Codec.BOOLEAN), (HttpServerConfig o, Boolean i) -> o.requireClientCert = i, o -> o.requireClientCert).add()
          .append(new KeyedCodec<String>("SharedSecret", Codec.STRING), (HttpServerConfig o, String i) -> o.sharedSecret = i, o -> o.sharedSecret).add()
          .append(new KeyedCodec<String>("SharedSecretEnv", Codec.STRING), (HttpServerConfig o, String i) -> o.sharedSecretEnv = i, o -> o.sharedSecretEnv).add()
          .append(new KeyedCodec<Integer>("MaxBodyBytes", Codec.INTEGER), (HttpServerConfig o, Integer i) -> o.maxBodyBytes = i, o -> o.maxBodyBytes).add()
          .append(new KeyedCodec<Integer>("ClockSkewSeconds", Codec.INTEGER), (HttpServerConfig o, Integer i) -> o.clockSkewSeconds = i, o -> o.clockSkewSeconds).add()
          .append(new KeyedCodec<Integer>("MaxNonceEntries", Codec.INTEGER), (HttpServerConfig o, Integer i) -> o.maxNonceEntries = i, o -> o.maxNonceEntries).add()
          .append(new KeyedCodec<Integer>("ThreadPoolSize", Codec.INTEGER), (HttpServerConfig o, Integer i) -> o.threadPoolSize = i, o -> o.threadPoolSize).add()
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
  private int maxNonceEntries = 10000;
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

  public int getMaxNonceEntries() {
    return maxNonceEntries;
  }

  public int getThreadPoolSize() {
    return threadPoolSize;
  }
}
