# SecureWarps

SecureWarps is a Hytale server plugin (Java 25) that provides:

- Warp management backed by PostgreSQL.
- Inventory snapshots stored in PostgreSQL (DB-authoritative between sessions).
- TLS-first HTTP client and HTTPS server with optional mTLS and signed requests.
- Async DB operations with timeouts to avoid blocking the game thread.

## Features

- `/warp set <name>` — save a warp at your current position.
- `/warp go <name>` — teleport to a warp (supports cross-world via Universe load).
- `/warp list` — list all warps.
- `/warp delete <name>` — delete a warp.
- `/invdb delete <uuid>` — admin-only delete of a player's inventory snapshot.

Inventory snapshots are **never** deleted automatically. They are only removed via the admin command.

## Requirements

- Java 25
- PostgreSQL 12+ (or compatible)

## Build

From `SecureWarps/`:

```bash
./gradlew shadowJar
```

Output:

```
SecureWarps/build/libs/SecureWarps-0.1.0.jar
```

The fat jar includes HikariCP + PostgreSQL + Jackson.

## Install

1. Copy the jar to your Hytale plugins directory.
2. Start the server once to generate `SecureWarps.json`.
3. Edit `SecureWarps.json` with DB + TLS settings.
4. Restart the server.

## Configuration

`SecureWarps.json` is generated on first run.

### Database

```json
"Database": {
  "Host": "db.example.com",
  "Port": 5432,
  "Database": "securewarps",
  "Username": "securewarps",
  "Password": "",
  "PasswordEnv": "SECUREWARPS_DB_PASSWORD",
  "SslMode": "verify-full",
  "SslRootCert": "/path/to/ca.pem",
  "SslCert": "",
  "SslKey": "",
  "SslKeyPassword": "",
  "ConnectTimeoutMillis": 10000,
  "MaxPoolSize": 10,
  "MinIdle": 2,
  "DbOperationTimeoutMillis": 5000,
  "DbExecutorThreads": 4
}
```

Notes:
- Use `SslMode: "verify-full"` for internet-facing DB connections.
- Set `PasswordEnv` to avoid storing plaintext passwords in config.

### Inventory snapshots

```json
"Inventory": {
  "Enabled": true,
  "LoadOnReady": true,
  "SaveOnDisconnect": true,
  "SavePeriodically": true,
  "SaveIntervalSeconds": 60
}
```

Behavior:
- Loads inventory on player ready.
- Saves on disconnect and periodically (default 60s).
- No automatic deletion. Admin-only deletion via `/invdb delete <uuid>`.

### Permissions

```json
"Permissions": {
  "Set": "securewarps.set",
  "Use": "securewarps.use",
  "List": "securewarps.list",
  "Delete": "securewarps.delete",
  "Admin": "securewarps.admin"
}
```

### HTTP client (optional)

```json
"HttpClient": {
  "BaseUrl": "https://api.example.com",
  "ConnectTimeoutMillis": 5000,
  "RequestTimeoutMillis": 10000,
  "TrustStorePath": "",
  "TrustStorePassword": "",
  "KeyStorePath": "",
  "KeyStorePassword": "",
  "SharedSecret": "",
  "SharedSecretEnv": "SECUREWARPS_SHARED_SECRET"
}
```

### HTTPS server (optional)

```json
"HttpServer": {
  "Enabled": false,
  "BindHost": "0.0.0.0",
  "Port": 8443,
  "KeyStorePath": "/path/to/server.p12",
  "KeyStorePassword": "changeit",
  "TrustStorePath": "",
  "TrustStorePassword": "",
  "RequireClientCert": false,
  "SharedSecret": "",
  "SharedSecretEnv": "SECUREWARPS_SHARED_SECRET",
  "MaxBodyBytes": 1048576,
  "ClockSkewSeconds": 60,
  "ThreadPoolSize": 4
}
```

If `HttpServer.Enabled` is true, a shared secret is required.

## HTTP API (when enabled)

All routes except `/health` require signed requests.

- `GET /health` → `ok`
- `GET /warps`
- `GET /warps/{name}`
- `POST /warps` (JSON body)
- `DELETE /warps/{name}`

### Request signing

Headers:

- `X-SW-Timestamp`: unix epoch seconds
- `X-SW-Nonce`: random UUID
- `X-SW-Signature`: hex HMAC-SHA256

Signature payload:

```
METHOD\nPATH\nTIMESTAMP\nNONCE\nSHA256(body)
```

HMAC key is `SharedSecret` (or `SharedSecretEnv`).

## Testing

### Local DB

1. Start Postgres locally.
2. Create DB + user:
   ```sql
   CREATE DATABASE securewarps;
   CREATE USER securewarps WITH ENCRYPTED PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE securewarps TO securewarps;
   ```
3. Update `SecureWarps.json` with connection info.
4. Start server and run:
   - `/warp set test`
   - `/warp list`
   - `/warp go test`
   - `/warp delete test`

### Inventory snapshot checks

1. Join server, change inventory.
2. Wait for periodic save or disconnect.
3. Rejoin and confirm inventory is restored.

### HTTPS server

- Enable `HttpServer` and configure PKCS12 keystore.
- Use curl with signed headers or a small client script to test endpoints.

## Security notes

- For remote DB access, use `sslmode=verify-full` with a valid CA cert.
- Prefer private networking/VPN over public DB exposure.
- Keep `SharedSecret` out of the config via env vars when possible.

## License

MIT (see `LICENSE`).
