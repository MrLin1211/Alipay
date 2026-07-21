#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="${DIST_DIR:-$ROOT_DIR/dist-production}"
HBUILDERX_DIR="${HBUILDERX_DIR:-/Applications/HBuilderX.app/Contents/HBuilderX}"
UNI_CLI_DIR="$HBUILDERX_DIR/plugins/uniapp-cli-vite"
NODE_BIN="$HBUILDERX_DIR/plugins/node/node"
APP_SSL_DIR="$ROOT_DIR/ssl/app.linsy.online_nginx"

if [ ! -x "$NODE_BIN" ] || [ ! -f "$UNI_CLI_DIR/vite.config.js" ]; then
  echo "HBuilderX uni-app compiler not found: $HBUILDERX_DIR" >&2
  exit 1
fi
if [ ! -f "$APP_SSL_DIR/app.linsy.online_bundle.crt" ] || [ ! -f "$APP_SSL_DIR/app.linsy.online.key" ]; then
  echo "Missing app.linsy.online certificate or private key in $APP_SSL_DIR" >&2
  exit 1
fi

rm -rf "$DIST_DIR/frontend/app"
mkdir -p "$DIST_DIR/frontend/app"

cd "$UNI_CLI_DIR"
env \
  UNI_PLATFORM=h5 \
  UNI_INPUT_DIR="$ROOT_DIR/mall-miniapp" \
  UNI_HBUILDERX_PLUGINS="$HBUILDERX_DIR/plugins" \
  NODE_ENV=production \
  "$NODE_BIN" node_modules/vite/bin/vite.js build \
    --config vite.config.js \
    --outDir "$DIST_DIR/frontend/app" \
    --emptyOutDir

# HBuilderX CLI production builds do not consistently copy the project's
# absolute /static assets. H5 tabBar iconPath resolves to /static/tabbar/**,
# so explicitly include the directory in the deploy artifact.
mkdir -p "$DIST_DIR/frontend/app/static"
cp -R "$ROOT_DIR/mall-miniapp/static/." "$DIST_DIR/frontend/app/static/"

rm -rf "$DIST_DIR/deploy"
cp -R "$ROOT_DIR/deploy" "$DIST_DIR/deploy"
mkdir -p "$DIST_DIR/deploy/ssl/app.linsy.online"
cp "$APP_SSL_DIR/app.linsy.online_bundle.crt" "$DIST_DIR/deploy/ssl/app.linsy.online/"
cp "$APP_SSL_DIR/app.linsy.online.key" "$DIST_DIR/deploy/ssl/app.linsy.online/"
chmod 600 "$DIST_DIR/deploy/ssl/app.linsy.online/app.linsy.online.key"

echo "app.linsy.online H5 artifacts written to $DIST_DIR/frontend/app"
