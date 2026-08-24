#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="$ROOT_DIR/dist-production-gateway"
REMOTE="${REMOTE:-root@119.45.58.43}"
REMOTE_RELEASE_DIR="${REMOTE_RELEASE_DIR:-/tmp/alipay-gateway-release}"
PACKAGE_PATH="${PACKAGE_PATH:-/tmp/alipay-gateway-release.tar.gz}"
SSH_IDENTITY="${SSH_IDENTITY:-$ROOT_DIR/root.pem}"
RESET_GATEWAY_DATA="${RESET_GATEWAY_DATA:-false}"

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/backend" "$DIST_DIR/frontend/admin" "$DIST_DIR/frontend/client" "$DIST_DIR/frontend/testpay"

cd "$ROOT_DIR/payment-gateway-api"
mvn -q -DskipTests clean package
cp target/payment-gateway-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/payment-gateway-api.jar"

cd "$ROOT_DIR/gateway-pay-test-api"
mvn -q -DskipTests clean package
cp target/gateway-pay-test-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/gateway-pay-test-api.jar"

cd "$ROOT_DIR/payment-gateway-admin"
VITE_API_BASE_URL="https://api.linsy.online" npm run build
cp -R dist/. "$DIST_DIR/frontend/admin/"

cd "$ROOT_DIR/gateway-client-admin"
VITE_API_BASE_URL="https://api.linsy.online" npm run build
cp -R dist/. "$DIST_DIR/frontend/client/"

cd "$ROOT_DIR/gateway-pay-test"
VITE_GATEWAY_BASE_URL="https://api.linsy.online" npm run build
cp -R dist/. "$DIST_DIR/frontend/testpay/"

mkdir -p "$DIST_DIR/deploy/nginx" "$DIST_DIR/deploy/systemd" "$DIST_DIR/deploy/ssl"
cp "$ROOT_DIR/deploy/nginx/alipay.conf" "$DIST_DIR/deploy/nginx/alipay.conf"
cp "$ROOT_DIR/deploy/systemd/alipay-payment-gateway-api.service" "$DIST_DIR/deploy/systemd/alipay-payment-gateway-api.service"
cp "$ROOT_DIR/deploy/systemd/alipay-gateway-pay-test-api.service" "$DIST_DIR/deploy/systemd/alipay-gateway-pay-test-api.service"

copy_ssl() {
  local_dir="$1"
  target_dir="$2"
  if [ -d "$ROOT_DIR/ssl/$local_dir" ]; then
    mkdir -p "$DIST_DIR/deploy/ssl/$target_dir"
    cp "$ROOT_DIR/ssl/$local_dir/"*.crt "$DIST_DIR/deploy/ssl/$target_dir/"
    cp "$ROOT_DIR/ssl/$local_dir/"*.key "$DIST_DIR/deploy/ssl/$target_dir/"
    chmod 600 "$DIST_DIR/deploy/ssl/$target_dir/"*.key
  fi
}

copy_ssl "app.linsy.online_nginx" "app.linsy.online"
copy_ssl "mall.linsy.online_nginx" "mall.linsy.online_nginx"
copy_ssl "admin.linsy.online_nginx" "admin.linsy.online_nginx"
copy_ssl "api.linsy.online_nginx" "api.linsy.online_nginx"
copy_ssl "client.linsy.online_nginx" "client.linsy.online_nginx"
copy_ssl "testpay.linsy.online_nginx" "testpay.linsy.online_nginx"

tar -czf "$PACKAGE_PATH" -C "$DIST_DIR" .
scp -i "$SSH_IDENTITY" -o IdentitiesOnly=yes "$PACKAGE_PATH" "$REMOTE:/tmp/alipay-gateway-release.tar.gz"

ssh -i "$SSH_IDENTITY" -o IdentitiesOnly=yes "$REMOTE" sh -s -- "$REMOTE_RELEASE_DIR" "$RESET_GATEWAY_DATA" <<'REMOTE_SCRIPT'
set -eu

RELEASE_DIR="$1"
RESET_GATEWAY_DATA="$2"
TS=$(date +%Y%m%d-%H%M%S)

mkdir -p "$RELEASE_DIR" /var/www/alipay/backups /opt/alipay/backups /opt/alipay/apps /opt/alipay/runtime/payment-gateway-api /etc/alipay /etc/nginx/ssl
rm -rf "$RELEASE_DIR"/*
tar -xzf /tmp/alipay-gateway-release.tar.gz -C "$RELEASE_DIR"

backup_dir="/var/www/alipay/backups/gateway-init-$TS"
mkdir -p "$backup_dir"
[ -f /etc/nginx/conf.d/alipay.conf ] && cp /etc/nginx/conf.d/alipay.conf "$backup_dir/alipay.conf" || true
[ -f /etc/alipay/common.env ] && cp /etc/alipay/common.env "$backup_dir/common.env" || true
[ -f /etc/alipay/testpay.env ] && cp /etc/alipay/testpay.env "$backup_dir/testpay.env" || true
[ -f /opt/alipay/apps/payment-gateway-api.jar ] && cp /opt/alipay/apps/payment-gateway-api.jar "$backup_dir/payment-gateway-api.jar" || true
[ -f /opt/alipay/apps/gateway-pay-test-api.jar ] && cp /opt/alipay/apps/gateway-pay-test-api.jar "$backup_dir/gateway-pay-test-api.jar" || true
tar -czf "$backup_dir/admin.tar.gz" -C /var/www/alipay/admin . 2>/dev/null || true
tar -czf "$backup_dir/client.tar.gz" -C /var/www/alipay/client . 2>/dev/null || true
tar -czf "$backup_dir/testpay.tar.gz" -C /var/www/alipay/testpay . 2>/dev/null || true

mkdir -p /var/www/alipay/admin /var/www/alipay/client /var/www/alipay/testpay
rm -rf /var/www/alipay/admin/* /var/www/alipay/client/* /var/www/alipay/testpay/*
cp -a "$RELEASE_DIR/frontend/admin/." /var/www/alipay/admin/
cp -a "$RELEASE_DIR/frontend/client/." /var/www/alipay/client/
cp -a "$RELEASE_DIR/frontend/testpay/." /var/www/alipay/testpay/
chown -R nginx:nginx /var/www/alipay/admin /var/www/alipay/client /var/www/alipay/testpay

rm -f /opt/alipay/apps/payment-gateway-api.jar
cp "$RELEASE_DIR/backend/payment-gateway-api.jar" /opt/alipay/apps/payment-gateway-api.jar
rm -f /opt/alipay/apps/gateway-pay-test-api.jar
cp "$RELEASE_DIR/backend/gateway-pay-test-api.jar" /opt/alipay/apps/gateway-pay-test-api.jar
mkdir -p /opt/alipay/runtime/gateway-pay-test-api
chown -R alipay:alipay /opt/alipay/apps /opt/alipay/runtime/payment-gateway-api /opt/alipay/runtime/gateway-pay-test-api

cp "$RELEASE_DIR/deploy/systemd/alipay-payment-gateway-api.service" /etc/systemd/system/alipay-payment-gateway-api.service
cp "$RELEASE_DIR/deploy/systemd/alipay-gateway-pay-test-api.service" /etc/systemd/system/alipay-gateway-pay-test-api.service
systemctl daemon-reload

if [ ! -s /etc/alipay/testpay.env ]; then
  echo "Missing required production file: /etc/alipay/testpay.env" >&2
  exit 1
fi

if [ ! -f /etc/alipay/common.env ]; then
  touch /etc/alipay/common.env
  chmod 600 /etc/alipay/common.env
fi

set -a
. /etc/alipay/common.env
set +a

set_env() {
  key="$1"
  val="$2"
  tmp=$(mktemp)
  grep -v "^$key=" /etc/alipay/common.env > "$tmp" || true
  printf "%s='%s'\n" "$key" "$val" >> "$tmp"
  cat "$tmp" > /etc/alipay/common.env
  rm -f "$tmp"
}

set_env SPRING_PROFILES_ACTIVE "${SPRING_PROFILES_ACTIVE:-prod}"
set_env SERVER_ADDRESS "${SERVER_ADDRESS:-127.0.0.1}"
set_env GATEWAY_DEMO_APP_ENABLED "false"
set_env ZHENBAOGE_HOST "${ZHENBAOGE_HOST:-https://pay.zhenbaoge.com}"
set_env ZHENBAOGE_EXTERNAL_ID "${ZHENBAOGE_EXTERNAL_ID:-}"
set_env ZHENBAOGE_MD5_KEY "${ZHENBAOGE_MD5_KEY:-}"
set_env ZHENBAOGE_AES_KEY "${ZHENBAOGE_AES_KEY:-}"
set_env ZHENBAOGE_NOTIFY_URL "${ZHENBAOGE_NOTIFY_URL:-https://api.linsy.online/api/gateway/zhenbaoge/notify}"
set_env ZHENBAOGE_ENABLED "${ZHENBAOGE_ENABLED:-true}"
chmod 600 /etc/alipay/common.env

set -a
. /etc/alipay/common.env
set +a
for key in ZHENBAOGE_EXTERNAL_ID ZHENBAOGE_MD5_KEY ZHENBAOGE_AES_KEY; do
  if [ -z "$(printenv "$key")" ]; then
    echo "Missing required production environment variable: $key" >&2
    exit 1
  fi
done

if [ -d "$RELEASE_DIR/deploy/ssl" ]; then
  cp -a "$RELEASE_DIR/deploy/ssl/." /etc/nginx/ssl/
  find /etc/nginx/ssl -name '*.key' -type f -exec chmod 600 {} \;
fi

cp "$RELEASE_DIR/deploy/nginx/alipay.conf" /etc/nginx/conf.d/alipay.conf
nginx -t
systemctl reload nginx

systemctl disable --now alipay-merchant-api.service merchant-api.service 2>/dev/null || true
systemctl enable alipay-payment-gateway-api.service
systemctl restart alipay-payment-gateway-api.service
for i in $(seq 1 30); do
  if MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" mysql -h 127.0.0.1 -P 3306 -u "$GATEWAY_DATASOURCE_USERNAME" "$(printf '%s' "$GATEWAY_DATASOURCE_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')" -N -B -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='gateway_client_session';" 2>/dev/null | grep -q '^1$'; then
    break
  fi
  sleep 2
done
systemctl is-active alipay-payment-gateway-api.service
systemctl enable alipay-gateway-pay-test-api.service
systemctl restart alipay-gateway-pay-test-api.service
systemctl is-active alipay-gateway-pay-test-api.service

set -a
. /etc/alipay/common.env
set +a
DB=$(printf '%s' "$GATEWAY_DATASOURCE_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')
if [ "$RESET_GATEWAY_DATA" = "true" ]; then
  MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" mysqldump --no-tablespaces -h 127.0.0.1 -P 3306 -u "$GATEWAY_DATASOURCE_USERNAME" "$DB" \
    gateway_admin_session gateway_app gateway_client gateway_client_user gateway_client_session \
    gateway_pay_order gateway_refund_order gateway_notify_record \
    > "$backup_dir/gateway-cleanup-tables.sql" 2>/dev/null || true

  MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" mysql -h 127.0.0.1 -P 3306 -u "$GATEWAY_DATASOURCE_USERNAME" "$DB" <<'SQL'
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM gateway_admin_session;
DELETE FROM gateway_client_session;
DELETE FROM gateway_client_user;
DELETE FROM gateway_client;
DELETE FROM gateway_app;
DELETE FROM gateway_refund_order;
DELETE FROM gateway_notify_record;
DELETE FROM gateway_pay_order;
ALTER TABLE gateway_admin_session AUTO_INCREMENT = 1;
ALTER TABLE gateway_client_session AUTO_INCREMENT = 1;
ALTER TABLE gateway_client_user AUTO_INCREMENT = 1;
ALTER TABLE gateway_client AUTO_INCREMENT = 1;
ALTER TABLE gateway_app AUTO_INCREMENT = 1;
ALTER TABLE gateway_refund_order AUTO_INCREMENT = 1;
ALTER TABLE gateway_notify_record AUTO_INCREMENT = 1;
ALTER TABLE gateway_pay_order AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;
SQL
fi

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" mysql -h 127.0.0.1 -P 3306 -u "$GATEWAY_DATASOURCE_USERNAME" "$DB" -N -B -e "
SELECT 'gateway_admin_user', COUNT(*) FROM gateway_admin_user
UNION ALL SELECT 'gateway_app', COUNT(*) FROM gateway_app
UNION ALL SELECT 'gateway_client', COUNT(*) FROM gateway_client
UNION ALL SELECT 'gateway_client_user', COUNT(*) FROM gateway_client_user
UNION ALL SELECT 'gateway_pay_order', COUNT(*) FROM gateway_pay_order
UNION ALL SELECT 'gateway_refund_order', COUNT(*) FROM gateway_refund_order
UNION ALL SELECT 'gateway_notify_record', COUNT(*) FROM gateway_notify_record
UNION ALL SELECT 'zhenbaoge_channel_config', COUNT(*) FROM zhenbaoge_channel_config;
"

curl -sS -I https://admin.linsy.online >/dev/null
curl -sS -I https://client.linsy.online >/dev/null
curl -sS -I https://testpay.linsy.online >/dev/null
curl -sS -o /dev/null -w 'api_status=%{http_code}\n' https://api.linsy.online/api/gateway/admin/channel/config

echo "gateway production deploy completed: backup=$backup_dir"
REMOTE_SCRIPT

echo "Gateway production deploy completed."
