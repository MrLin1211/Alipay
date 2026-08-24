#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE_REMOTE="${SOURCE_REMOTE:-root@119.45.58.43}"
SOURCE_IDENTITY="${SOURCE_IDENTITY:-$ROOT_DIR/root.pem}"
BOOTSTRAP_DIR="${BOOTSTRAP_DIR:-/private/tmp/alipay-gateway-aliyun-bootstrap}"
REMOTE_DIR="/tmp/alipay-gateway-bootstrap-export-$$"

rm -rf "$BOOTSTRAP_DIR"
mkdir -p "$BOOTSTRAP_DIR"
chmod 700 "$BOOTSTRAP_DIR"

cleanup_remote() {
  ssh -i "$SOURCE_IDENTITY" -o IdentitiesOnly=yes "$SOURCE_REMOTE" "rm -rf '$REMOTE_DIR'" >/dev/null 2>&1 || true
}
trap cleanup_remote EXIT INT TERM

ssh -i "$SOURCE_IDENTITY" -o IdentitiesOnly=yes "$SOURCE_REMOTE" sh -s -- "$REMOTE_DIR" <<'REMOTE_SCRIPT'
set -eu

EXPORT_DIR="$1"
COMMON_ENV=/etc/alipay/common.env
TESTPAY_ENV=/etc/alipay/testpay.env

test -s "$COMMON_ENV"
test -s "$TESTPAY_ENV"
mkdir -p "$EXPORT_DIR"
chmod 700 "$EXPORT_DIR"

grep -E '^(GATEWAY_ADMIN_|GATEWAY_CLIENT_|ZHENBAOGE_)' "$COMMON_ENV" > "$EXPORT_DIR/gateway-source.env" || true
grep -E '^(TESTPAY_GATEWAY_APP_ID|TESTPAY_GATEWAY_APP_SECRET)=' "$TESTPAY_ENV" > "$EXPORT_DIR/testpay-source.env"
chmod 600 "$EXPORT_DIR/"*.env

set -a
. "$COMMON_ENV"
. "$TESTPAY_ENV"
set +a

require_env() {
  eval "value=\${$1:-}"
  if [ -z "$value" ]; then
    echo "Required source setting is missing: $1" >&2
    exit 1
  fi
}

for key in \
  GATEWAY_DATASOURCE_URL \
  GATEWAY_DATASOURCE_USERNAME \
  GATEWAY_DATASOURCE_PASSWORD \
  GATEWAY_ADMIN_USERNAME \
  GATEWAY_ADMIN_PASSWORD \
  ZHENBAOGE_EXTERNAL_ID \
  ZHENBAOGE_MD5_KEY \
  ZHENBAOGE_AES_KEY \
  TESTPAY_GATEWAY_APP_ID \
  TESTPAY_GATEWAY_APP_SECRET
do
  require_env "$key"
done

DB=$(printf '%s' "$GATEWAY_DATASOURCE_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')
APP_ID="$TESTPAY_GATEWAY_APP_ID"
MYSQL="mysql -h127.0.0.1 -P3306 -u$GATEWAY_DATASOURCE_USERNAME $DB -N -B"
DUMP="mysqldump --no-tablespaces --single-transaction --skip-add-locks --no-create-info --skip-triggers --compact -h127.0.0.1 -P3306 -u$GATEWAY_DATASOURCE_USERNAME $DB"

APP_COUNT=$(MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL -e "SELECT COUNT(*) FROM gateway_app WHERE app_id='$APP_ID'")
CLIENT_IDS=$(MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL -e "SELECT GROUP_CONCAT(id) FROM gateway_client WHERE app_id='$APP_ID'")
if [ "$APP_COUNT" != "1" ] || [ -z "$CLIENT_IDS" ] || [ "$CLIENT_IDS" = "NULL" ]; then
  echo "The production test application or its client relation was not found." >&2
  exit 1
fi

{
  printf '%s\n' 'SET FOREIGN_KEY_CHECKS=0;'
  MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $DUMP gateway_app --where="app_id='$APP_ID'"
  MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $DUMP gateway_client --where="app_id='$APP_ID'"
  MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $DUMP gateway_client_user --where="client_id IN ($CLIENT_IDS)"
  printf '%s\n' 'SET FOREIGN_KEY_CHECKS=1;'
} > "$EXPORT_DIR/bootstrap.sql"
chmod 600 "$EXPORT_DIR/bootstrap.sql"

APP_NAME=$(MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL -e "SELECT app_name FROM gateway_app WHERE app_id='$APP_ID'")
CLIENT_COUNT=$(MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL -e "SELECT COUNT(*) FROM gateway_client WHERE app_id='$APP_ID'")
USER_COUNT=$(MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL -e "SELECT COUNT(*) FROM gateway_client_user WHERE client_id IN ($CLIENT_IDS)")
printf 'app_name=%s\nclients=%s\nusers=%s\n' "$APP_NAME" "$CLIENT_COUNT" "$USER_COUNT" > "$EXPORT_DIR/manifest.txt"
REMOTE_SCRIPT

scp -i "$SOURCE_IDENTITY" -o IdentitiesOnly=yes -r "$SOURCE_REMOTE:$REMOTE_DIR/." "$BOOTSTRAP_DIR/"
chmod 600 "$BOOTSTRAP_DIR/"*

test -s "$BOOTSTRAP_DIR/gateway-source.env"
test -s "$BOOTSTRAP_DIR/testpay-source.env"
test -s "$BOOTSTRAP_DIR/bootstrap.sql"
test -s "$BOOTSTRAP_DIR/manifest.txt"

echo "Bootstrap export completed: $BOOTSTRAP_DIR"
cat "$BOOTSTRAP_DIR/manifest.txt"
