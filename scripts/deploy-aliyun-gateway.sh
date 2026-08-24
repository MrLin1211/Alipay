#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORKBENCH_BIN="${WORKBENCH_BIN:-/private/tmp/workbench-cli-bin/workbench}"
INSTANCE_ID="${INSTANCE_ID:-i-2zec2aiw5htqmv050z75}"
DIST_DIR="${DIST_DIR:-$ROOT_DIR/dist-production-aliyun-gateway}"
BOOTSTRAP_DIR="${BOOTSTRAP_DIR:-/private/tmp/alipay-gateway-aliyun-bootstrap}"
SKIP_BOOTSTRAP_EXPORT="${SKIP_BOOTSTRAP_EXPORT:-false}"
STAGE_DIR=$(mktemp -d /private/tmp/alipay-gateway-aliyun-stage.XXXXXX)
PACKAGE_PATH=/private/tmp/alipay-gateway-aliyun-release.tar.gz
REMOTE_PACKAGE=/tmp/alipay-gateway-aliyun-release.tar.gz
REMOTE_RELEASE=/tmp/alipay-gateway-aliyun-release

cleanup_local() {
  rm -rf "$STAGE_DIR" "$PACKAGE_PATH"
  if [ "$SKIP_BOOTSTRAP_EXPORT" != "true" ]; then
    rm -rf "$BOOTSTRAP_DIR"
  fi
}
trap cleanup_local EXIT INT TERM

test -x "$WORKBENCH_BIN"

if [ "$SKIP_BOOTSTRAP_EXPORT" = "true" ]; then
  test -s "$BOOTSTRAP_DIR/gateway-source.env"
  test -s "$BOOTSTRAP_DIR/testpay-source.env"
  test -s "$BOOTSTRAP_DIR/bootstrap.sql"
  test -s "$BOOTSTRAP_DIR/manifest.txt"
else
  sh "$ROOT_DIR/scripts/export-gateway-bootstrap.sh"
fi
sh "$ROOT_DIR/scripts/build-aliyun-gateway.sh"

cp -R "$DIST_DIR/." "$STAGE_DIR/"
mkdir -p "$STAGE_DIR/bootstrap"
cp "$BOOTSTRAP_DIR/gateway-source.env" "$STAGE_DIR/bootstrap/gateway-source.env"
cp "$BOOTSTRAP_DIR/testpay-source.env" "$STAGE_DIR/bootstrap/testpay-source.env"
cp "$BOOTSTRAP_DIR/bootstrap.sql" "$STAGE_DIR/bootstrap/bootstrap.sql"
cp "$BOOTSTRAP_DIR/manifest.txt" "$STAGE_DIR/bootstrap/manifest.txt"
chmod 600 "$STAGE_DIR/bootstrap/"*

tar -czf "$PACKAGE_PATH" -C "$STAGE_DIR" .
chmod 600 "$PACKAGE_PATH"

"$WORKBENCH_BIN" upload "$PACKAGE_PATH" "$REMOTE_PACKAGE" --force -i "$INSTANCE_ID"
set +e
"$WORKBENCH_BIN" exec -i "$INSTANCE_ID" --timeout 1800 \
  -c "rm -rf '$REMOTE_RELEASE' && mkdir -p '$REMOTE_RELEASE' && tar -xzf '$REMOTE_PACKAGE' -C '$REMOTE_RELEASE' && chmod 700 '$REMOTE_RELEASE/bootstrap' && sh '$REMOTE_RELEASE/deploy/install.sh' '$REMOTE_RELEASE'" \
  --output text
DEPLOY_STATUS=$?
set -e

"$WORKBENCH_BIN" exec -i "$INSTANCE_ID" --timeout 60 \
  -c "rm -rf '$REMOTE_RELEASE' '$REMOTE_PACKAGE'" --output text >/dev/null 2>&1 || true

if [ "$DEPLOY_STATUS" -ne 0 ]; then
  exit "$DEPLOY_STATUS"
fi

echo "Aliyun gateway deployment completed."
