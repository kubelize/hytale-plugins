package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class HttpClientConfig {
  public static final BuilderCodec<HttpClientConfig> CODEC =
      BuilderCodec.builder(HttpClientConfig.class, HttpClientConfig::new)
          .append(new KeyedCodec("BaseUrl", Codec.STRING), (o, i) -> o.baseUrl = i, o -> o.baseUrl).add()
          .append(new KeyedCodec("ConnectTimeoutMillis", Codec.INTEGER), (o, i) -> o.connectTimeoutMillis = i, o -> o.connectTimeoutMillis).add()
          .append(new KeyedCodec("RequestTimeoutMillis", Codec.INTEGER), (o, i) -> o.requestTimeoutMillis = i, o -> o.requestTimeoutMillis).add()
          .append(new KeyedCodec("TrustStorePath", Codec.STRING), (o, i) -> o.trustStorePath = i, o -> o.trustStorePath).add()
          .append(new KeyedCodec("TrustStorePassword", Codec.STRING), (o, i) -> o.trustStorePassword = i, o -> o.trustStorePassword).add()
          .append(new KeyedCodec("KeyStorePath", Codec.STRING), (o, i) -> o.keyStorePath = i, o -> o.keyStorePath).add()
          .append(new KeyedCodec("KeyStorePassword", Codec.STRING), (o, i) -> o.keyStorePassword = i, o -> o.keyStorePassword).add()
          .append(new KeyedCodec("SharedSecret", Codec.STRING), (o, i) -> o.sharedSecret = i, o -> o.sharedSecret).add()
          .append(new KeyedCodec("SharedSecretEnv", Codec.STRING), (o, i) -> o.sharedSecretEnv = i, o -> o.sharedSecretEnv).add()
          .build();

  private String baseUrl = "";
  private int connectTimeoutMillis = 5000;
  private int requestTimeoutMillis = 10000;

  // Optional for mTLS and private CA trust.
  private String trustStorePath = "";
  private String trustStorePassword = "";
  private String keyStorePath = "";
  private String keyStorePassword = "";

  private String sharedSecret = "";
  private String sharedSecretEnv = "";

  public String getBaseUrl() {
    return baseUrl;
  }

  public int getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public int getRequestTimeoutMillis() {
    return requestTimeoutMillis;
  }

  public String getTrustStorePath() {
    return trustStorePath;
  }

  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public String getSharedSecret() {
    return sharedSecret;
  }

  public String getSharedSecretEnv() {
    return sharedSecretEnv;
  }
}
