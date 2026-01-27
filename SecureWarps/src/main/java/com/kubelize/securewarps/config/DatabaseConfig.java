package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DatabaseConfig {
  public static final BuilderCodec<DatabaseConfig> CODEC =
      BuilderCodec.builder(DatabaseConfig.class, DatabaseConfig::new)
          .append(new KeyedCodec("Host", Codec.STRING), (o, i) -> o.host = i, o -> o.host).add()
          .append(new KeyedCodec("Port", Codec.INTEGER), (o, i) -> o.port = i, o -> o.port).add()
          .append(new KeyedCodec("Database", Codec.STRING), (o, i) -> o.database = i, o -> o.database).add()
          .append(new KeyedCodec("Username", Codec.STRING), (o, i) -> o.username = i, o -> o.username).add()
          .append(new KeyedCodec("Password", Codec.STRING), (o, i) -> o.password = i, o -> o.password).add()
          .append(new KeyedCodec("PasswordEnv", Codec.STRING), (o, i) -> o.passwordEnv = i, o -> o.passwordEnv).add()
          .append(new KeyedCodec("SslMode", Codec.STRING), (o, i) -> o.sslMode = i, o -> o.sslMode).add()
          .append(new KeyedCodec("SslRootCert", Codec.STRING), (o, i) -> o.sslRootCert = i, o -> o.sslRootCert).add()
          .append(new KeyedCodec("SslCert", Codec.STRING), (o, i) -> o.sslCert = i, o -> o.sslCert).add()
          .append(new KeyedCodec("SslKey", Codec.STRING), (o, i) -> o.sslKey = i, o -> o.sslKey).add()
          .append(new KeyedCodec("SslKeyPassword", Codec.STRING), (o, i) -> o.sslKeyPassword = i, o -> o.sslKeyPassword).add()
          .append(new KeyedCodec("ConnectTimeoutMillis", Codec.INTEGER), (o, i) -> o.connectTimeoutMillis = i, o -> o.connectTimeoutMillis).add()
          .append(new KeyedCodec("MaxPoolSize", Codec.INTEGER), (o, i) -> o.maxPoolSize = i, o -> o.maxPoolSize).add()
          .append(new KeyedCodec("MinIdle", Codec.INTEGER), (o, i) -> o.minIdle = i, o -> o.minIdle).add()
          .append(new KeyedCodec("DbOperationTimeoutMillis", Codec.INTEGER), (o, i) -> o.dbOperationTimeoutMillis = i, o -> o.dbOperationTimeoutMillis).add()
          .append(new KeyedCodec("DbExecutorThreads", Codec.INTEGER), (o, i) -> o.dbExecutorThreads = i, o -> o.dbExecutorThreads).add()
          .build();

  private String host = "localhost";
  private int port = 5432;
  private String database = "securewarps";
  private String username = "securewarps";
  private String password = "";
  private String passwordEnv = "";

  // TLS defaults to verify-full for internet safety.
  private String sslMode = "verify-full";
  private String sslRootCert = "";
  private String sslCert = "";
  private String sslKey = "";
  private String sslKeyPassword = "";

  private int connectTimeoutMillis = 10000;
  private int maxPoolSize = 10;
  private int minIdle = 2;
  private int dbOperationTimeoutMillis = 5000;
  private int dbExecutorThreads = 4;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getDatabase() {
    return database;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getPasswordEnv() {
    return passwordEnv;
  }

  public String getSslMode() {
    return sslMode;
  }

  public String getSslRootCert() {
    return sslRootCert;
  }

  public String getSslCert() {
    return sslCert;
  }

  public String getSslKey() {
    return sslKey;
  }

  public String getSslKeyPassword() {
    return sslKeyPassword;
  }

  public int getConnectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public int getMinIdle() {
    return minIdle;
  }

  public int getDbOperationTimeoutMillis() {
    return dbOperationTimeoutMillis;
  }

  public int getDbExecutorThreads() {
    return dbExecutorThreads;
  }
}
