#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="${DIST_DIR:-$ROOT_DIR/dist-production-aliyun-gateway}"
API_BASE_URL="${API_BASE_URL:-https://api.chengxinkeji.xin}"

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/backend" "$DIST_DIR/frontend/admin" "$DIST_DIR/frontend/client" "$DIST_DIR/frontend/testpay"

cd "$ROOT_DIR/payment-gateway-api"
mvn -q -DskipTests clean package
cp target/payment-gateway-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/payment-gateway-api.jar"

cd "$ROOT_DIR/gateway-pay-test-api"
mvn -q -DskipTests clean package
cp target/gateway-pay-test-api-0.0.1-SNAPSHOT.jar "$DIST_DIR/backend/gateway-pay-test-api.jar"

cd "$ROOT_DIR/payment-gateway-admin"
VITE_API_BASE_URL="$API_BASE_URL" npm run build
cp -R dist/. "$DIST_DIR/frontend/admin/"

cd "$ROOT_DIR/gateway-client-admin"
VITE_API_BASE_URL="$API_BASE_URL" npm run build
cp -R dist/. "$DIST_DIR/frontend/client/"

cd "$ROOT_DIR/gateway-pay-test"
npm run build
cp -R dist/. "$DIST_DIR/frontend/testpay/"

mkdir -p "$DIST_DIR/deploy/nginx" "$DIST_DIR/deploy/systemd" "$DIST_DIR/deploy/mysql" "$DIST_DIR/deploy/ssl"
cp "$ROOT_DIR/deploy/aliyun/nginx/alipay-gateway-chengxinkeji.conf" "$DIST_DIR/deploy/nginx/"
cp "$ROOT_DIR/deploy/aliyun/systemd/"*.service "$DIST_DIR/deploy/systemd/"
cp "$ROOT_DIR/deploy/aliyun/mysql/my.cnf" "$DIST_DIR/deploy/mysql/"
cp "$ROOT_DIR/deploy/aliyun/install.sh" "$DIST_DIR/deploy/install.sh"

copy_certificate() {
  source_dir="$1"
  domain="$2"
  mkdir -p "$DIST_DIR/deploy/ssl/$domain"
  cp "$ROOT_DIR/ssl/aliyun/$source_dir/$domain.pem" "$DIST_DIR/deploy/ssl/$domain/fullchain.pem"
  cp "$ROOT_DIR/ssl/aliyun/$source_dir/$domain.key" "$DIST_DIR/deploy/ssl/$domain/privkey.pem"
  chmod 600 "$DIST_DIR/deploy/ssl/$domain/privkey.pem"
}

copy_certificate "26780387_admin.chengxinkeji.xin_nginx" "admin.chengxinkeji.xin"
copy_certificate "26780397_api.chengxinkeji.xin_nginx" "api.chengxinkeji.xin"
copy_certificate "26780400_testpay.chengxinkeji.xin_nginx" "testpay.chengxinkeji.xin"
copy_certificate "26780401_client.chengxinkeji.xin_nginx" "client.chengxinkeji.xin"

echo "Aliyun gateway artifacts written to $DIST_DIR"
