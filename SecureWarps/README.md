# SecureWarps

SecureWarps is a Hytale server plugin (Java 25) that provides:

- Warp management backed by PostgreSQL.
- Inventory snapshots stored in PostgreSQL (DB-authoritative between sessions).
- TLS-first HTTP client and HTTPS server with optional mTLS and signed requests.
- Async DB operations with timeouts to avoid blocking the game thread.

## Features

- `/warp set <name>` — save a warp at your current position.
- `/warp go <name>` — teleport to a local warp (cross‑world via Universe load).
- `/rwarp go <name>` — teleport to a remote warp (cross‑server transfer).
- `/rwarp set <name> <host> <port>` — admin-only: assign a remote server to a warp; creates it at your current position if missing.
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
gradle jar
```

Output:

```
SecureWarps/build/libs/SecureWarps-0.1.0.jar
```

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
  "DbExecutorThreads": 4,
  "DbExecutorQueueSize": 1000
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

### Portals (placeable teleporters)

SecureWarps attaches to the existing built-in portal items:

- `Teleporter` — uses the built-in Teleporter dialog to select a warp name, but teleports using SecureWarps.
- `Portal_Device` — uses the built-in Portal_Device dialog, but only admins can open it.

Teleporting respects `securewarps.use`. Admin-only configuration uses `securewarps.admin`.

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
  "MaxNonceEntries": 10000,
  "ThreadPoolSize": 4
}
```

If `HttpServer.Enabled` is true, a shared secret is required.

### Server identity (required for cross‑server warps)

```json
"Server": {
  "Host": "hytale-lobby.example.com",
  "Port": 8443
}
```

This is the address **other servers should connect to** for cross‑server warps.
SecureWarps stores this on every `/warp set` so it knows which server owns that warp.

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

## Cross-server setup (quick checklist)

1. Enable HTTPS server on each server that should accept warp writes (`HttpServer.Enabled: true`).
2. Use a real TLS certificate (or your own CA) and configure `KeyStorePath` / `KeyStorePassword`.
3. Set a shared secret on all servers (same value in `SharedSecret` or `SharedSecretEnv`).
4. Point the HTTP client on each server at the correct peer URL (`HttpClient.BaseUrl`).
5. Open firewalls to allow HTTPS traffic on the configured port.
6. (Optional) Turn on mTLS by setting `RequireClientCert: true` and configuring `TrustStorePath` / `TrustStorePassword`.

## Cross-server setup (detailed)

### 1) Build + install

From this repo:

```bash
gradle -p SecureWarps jar
```

Copy `SecureWarps/build/libs/SecureWarps-0.1.0.jar` into each server’s `mods/` directory.

Start each server once, then stop it. SecureWarps will create:

```
.../universe/<your-universe>/plugins/SecureWarps/SecureWarps.json
```

If you don’t see it, search:

```bash
find ~/server -name "SecureWarps.json" -maxdepth 6
```

### 2) Configure PostgreSQL (shared DB)

All servers must point to the same database.

Example `Database` section:

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
  "ConnectTimeoutMillis": 10000,
  "MaxPoolSize": 10,
  "MinIdle": 2,
  "DbOperationTimeoutMillis": 5000,
  "DbExecutorThreads": 4,
  "DbExecutorQueueSize": 1000
}
```

Set the password on each server:

```bash
export SECUREWARPS_DB_PASSWORD="your_db_password"
```

### 3) Create TLS certs for HTTPS

You need a TLS cert per server. Use a public cert or your own CA.

Self-signed (PKCS12):

```bash
./make-keystore.sh your-hostname.example.com /path/to/plugin/folder
```

### 4) Enable HTTPS server

Edit `SecureWarps.json` on each server:

```json
"HttpServer": {
  "Enabled": true,
  "BindHost": "0.0.0.0",
  "Port": 8443,
  "KeyStorePath": "/path/to/server.p12",
  "KeyStorePassword": "changeit",
  "RequireClientCert": false,
  "SharedSecretEnv": "SECUREWARPS_SHARED_SECRET"
}
```

### 5) Configure HTTP client

Point each server at its peer:

```json
"HttpClient": {
  "BaseUrl": "https://peer-server.example.com:8443",
  "SharedSecretEnv": "SECUREWARPS_SHARED_SECRET"
}
```

If your HTTPS cert is signed by a private CA, set `TrustStorePath` and `TrustStorePassword`.

### 6) Shared secret

The shared secret is a **pre‑shared key** used to sign every inter‑server HTTP request (HMAC‑SHA256).
Each server uses the secret to:

- **Sign** outgoing requests (client).
- **Verify** incoming requests (server).

All servers in the cross‑server mesh must use the **exact same value**.

Set the same secret on all servers:

```bash
export SECUREWARPS_SHARED_SECRET="long-random-string"
```

### 7) Open firewalls

Allow TCP 8443 between servers.

### 8) Restart + test

Restart all servers. On Server A:

- `/warp set test`
- `/warp list`

On Server B:

- `/warp list`
- `/warp go test`

If Server B can see and teleport, cross-server is working.

### Cross-server warp behavior

- `/warp set <name>` stores the **local server Host/Port** on the warp record.
- `/warp setremote <name> <host> <port>` updates the server address for an existing warp (admin-only).
- `/warp go <name>` only teleports to local warps.
- `/rwarp go <name>` uses the stored Host/Port to transfer to a remote server.
- `/rwarp set <name> <host> <port>` updates the server address for a warp; if it doesn’t exist, it is created at your current position (admin-only).
- The referral payload is the warp name; the destination server loads the warp from the shared DB and teleports the player.

If you don’t set `"Server": { "Host", "Port" }` on all servers, cross‑server warps will be rejected.

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
