#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="$ROOT_DIR/dist-production"

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/backend" "$DIST_DIR/frontend"

cd "$ROOT_DIR/mall-api"
mvn -q -DskipTests package
cp target/mall-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/mall-api.jar"

cd "$ROOT_DIR/merchant-api"
mvn -q -DskipTests package
cp target/merchant-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/merchant-api.jar"

cd "$ROOT_DIR/payment-gateway-api"
mvn -q -DskipTests package
cp target/payment-gateway-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/payment-gateway-api.jar"

cd "$ROOT_DIR/mall"
VITE_API_BASE_URL="https://api.linsy.online" npm run build
mkdir -p "$DIST_DIR/frontend/mall"
cp -R dist/. "$DIST_DIR/frontend/mall/"

cd "$ROOT_DIR/merchant"
VITE_API_BASE_URL="https://api.linsy.online" npm run build
mkdir -p "$DIST_DIR/frontend/merchant"
cp -R dist/. "$DIST_DIR/frontend/merchant/"

cd "$ROOT_DIR/payment-gateway-admin"
VITE_API_BASE_URL="https://api.linsy.online" npm run build
mkdir -p "$DIST_DIR/frontend/admin"
cp -R dist/. "$DIST_DIR/frontend/admin/"

cp -R "$ROOT_DIR/deploy" "$DIST_DIR/deploy"

echo "Production artifacts written to $DIST_DIR"
