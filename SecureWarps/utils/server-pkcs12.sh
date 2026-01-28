#!/usr/bin/env bash
set -euo pipefail

HOST="${1:-localhost}"
OUT_DIR="${2:-$HOME/server/mods/com.kubelize_SecureWarps}"
PASS="${3:-}"

if [ -z "$PASS" ]; then
  echo "Enter a strong password for the keystore:"
  read -r -s PASS
  echo
fi

mkdir -p "$OUT_DIR"
P12="$OUT_DIR/server.p12"

keytool -genkeypair -alias securewarps \
  -keyalg RSA -keysize 2048 -validity 365 \
  -keystore "$P12" -storetype PKCS12 \
  -dname "CN=$HOST" \
  -storepass "$PASS" -keypass "$PASS"

echo
echo "Keystore created: $P12"
echo "Use these config values:"
echo "KeyStorePath: $P12"
echo "KeyStorePassword: (your password)"