package com.kubelize.securewarps.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class DatabaseConfig {
  public static final BuilderCodec<DatabaseConfig> CODEC =
      BuilderCodec.<DatabaseConfig>builder(DatabaseConfig.class, DatabaseConfig::new)
          .append(new KeyedCodec<String>("Host", Codec.STRING), (DatabaseConfig o, String i) -> o.host = i, o -> o.host).add()
          .append(new KeyedCodec<Integer>("Port", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.port = i, o -> o.port).add()
          .append(new KeyedCodec<String>("Database", Codec.STRING), (DatabaseConfig o, String i) -> o.database = i, o -> o.database).add()
          .append(new KeyedCodec<String>("Username", Codec.STRING), (DatabaseConfig o, String i) -> o.username = i, o -> o.username).add()
          .append(new KeyedCodec<String>("Password", Codec.STRING), (DatabaseConfig o, String i) -> o.password = i, o -> o.password).add()
          .append(new KeyedCodec<String>("PasswordEnv", Codec.STRING), (DatabaseConfig o, String i) -> o.passwordEnv = i, o -> o.passwordEnv).add()
          .append(new KeyedCodec<String>("SslMode", Codec.STRING), (DatabaseConfig o, String i) -> o.sslMode = i, o -> o.sslMode).add()
          .append(new KeyedCodec<String>("SslRootCert", Codec.STRING), (DatabaseConfig o, String i) -> o.sslRootCert = i, o -> o.sslRootCert).add()
          .append(new KeyedCodec<String>("SslCert", Codec.STRING), (DatabaseConfig o, String i) -> o.sslCert = i, o -> o.sslCert).add()
          .append(new KeyedCodec<String>("SslKey", Codec.STRING), (DatabaseConfig o, String i) -> o.sslKey = i, o -> o.sslKey).add()
          .append(new KeyedCodec<String>("SslKeyPassword", Codec.STRING), (DatabaseConfig o, String i) -> o.sslKeyPassword = i, o -> o.sslKeyPassword).add()
          .append(new KeyedCodec<Integer>("ConnectTimeoutMillis", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.connectTimeoutMillis = i, o -> o.connectTimeoutMillis).add()
          .append(new KeyedCodec<Integer>("MaxPoolSize", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.maxPoolSize = i, o -> o.maxPoolSize).add()
          .append(new KeyedCodec<Integer>("MinIdle", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.minIdle = i, o -> o.minIdle).add()
          .append(new KeyedCodec<Integer>("DbOperationTimeoutMillis", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.dbOperationTimeoutMillis = i, o -> o.dbOperationTimeoutMillis).add()
          .append(new KeyedCodec<Integer>("DbExecutorThreads", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.dbExecutorThreads = i, o -> o.dbExecutorThreads).add()
          .append(new KeyedCodec<Integer>("DbExecutorQueueSize", Codec.INTEGER), (DatabaseConfig o, Integer i) -> o.dbExecutorQueueSize = i, o -> o.dbExecutorQueueSize).add()
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
  private int dbExecutorQueueSize = 1000;

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

  public int getDbExecutorQueueSize() {
    return dbExecutorQueueSize;
  }
}
