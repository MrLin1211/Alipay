#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="$ROOT_DIR/dist-production"
REMOTE="${REMOTE:-root@119.45.58.43}"
REMOTE_RELEASE_DIR="${REMOTE_RELEASE_DIR:-/tmp/alipay-release}"
PACKAGE_PATH="${PACKAGE_PATH:-/tmp/alipay-release.tar.gz}"
MODE="all"
SKIP_BUILD=0

usage() {
  cat <<'EOF'
Usage:
  sh scripts/deploy-production.sh [options]

Options:
  --all             Build and deploy frontend and backend. Default.
  --frontend-only   Deploy only mall, merchant, and admin frontend files.
  --backend-only    Deploy only backend jar files and restart services.
  --config-only     Deploy only systemd and nginx config files.
  --with-config     Deploy frontend, backend, systemd, and nginx config files.
  --skip-build      Reuse existing dist-production instead of running build-production.sh.
  -h, --help        Show this help.

Environment:
  REMOTE=root@119.45.58.43
  REMOTE_RELEASE_DIR=/tmp/alipay-release
  PACKAGE_PATH=/tmp/alipay-release.tar.gz
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --all)
      MODE="all"
      ;;
    --frontend-only)
      MODE="frontend"
      ;;
    --backend-only)
      MODE="backend"
      ;;
    --config-only)
      MODE="config"
      ;;
    --with-config)
      MODE="all_config"
      ;;
    --skip-build)
      SKIP_BUILD=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
  shift
done

if [ "$SKIP_BUILD" -eq 0 ]; then
  sh "$ROOT_DIR/scripts/build-production.sh"
elif [ ! -d "$DIST_DIR" ]; then
  echo "Missing $DIST_DIR. Run without --skip-build first." >&2
  exit 1
fi

tar -czf "$PACKAGE_PATH" -C "$DIST_DIR" .
scp "$PACKAGE_PATH" "$REMOTE:/tmp/alipay-release.tar.gz"

ssh "$REMOTE" sh -s -- "$MODE" "$REMOTE_RELEASE_DIR" <<'REMOTE_SCRIPT'
set -eu

MODE="$1"
RELEASE_DIR="$2"
TS=$(date +%Y%m%d-%H%M%S)

mkdir -p "$RELEASE_DIR" /var/www/alipay/backups /opt/alipay/backups
rm -rf "$RELEASE_DIR"/*
tar -xzf /tmp/alipay-release.tar.gz -C "$RELEASE_DIR"

deploy_frontend() {
  mkdir -p /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin

  tar -czf "/var/www/alipay/backups/mall-$TS.tar.gz" -C /var/www/alipay/mall . 2>/dev/null || true
  tar -czf "/var/www/alipay/backups/merchant-$TS.tar.gz" -C /var/www/alipay/merchant . 2>/dev/null || true
  tar -czf "/var/www/alipay/backups/admin-$TS.tar.gz" -C /var/www/alipay/admin . 2>/dev/null || true

  rm -rf /var/www/alipay/mall/* /var/www/alipay/merchant/* /var/www/alipay/admin/*
  cp -a "$RELEASE_DIR/frontend/mall/." /var/www/alipay/mall/
  cp -a "$RELEASE_DIR/frontend/merchant/." /var/www/alipay/merchant/
  cp -a "$RELEASE_DIR/frontend/admin/." /var/www/alipay/admin/
  chown -R nginx:nginx /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin
}

deploy_backend() {
  mkdir -p /opt/alipay

  [ -f /opt/alipay/mall-api.jar ] && cp /opt/alipay/mall-api.jar "/opt/alipay/backups/mall-api-$TS.jar" || true
  [ -f /opt/alipay/merchant-api.jar ] && cp /opt/alipay/merchant-api.jar "/opt/alipay/backups/merchant-api-$TS.jar" || true
  [ -f /opt/alipay/payment-gateway-api.jar ] && cp /opt/alipay/payment-gateway-api.jar "/opt/alipay/backups/payment-gateway-api-$TS.jar" || true

  cp "$RELEASE_DIR/backend/mall-api.jar" /opt/alipay/mall-api.jar
  cp "$RELEASE_DIR/backend/merchant-api.jar" /opt/alipay/merchant-api.jar
  cp "$RELEASE_DIR/backend/payment-gateway-api.jar" /opt/alipay/payment-gateway-api.jar
  chown alipay:alipay /opt/alipay/*.jar

  systemctl restart payment-gateway-api
  systemctl restart merchant-api
  systemctl restart mall-api
}

deploy_config() {
  cp "$RELEASE_DIR/deploy/systemd/"*.service /etc/systemd/system/
  systemctl daemon-reload

  if [ -f /etc/nginx/conf.d/alipay.conf ]; then
    cp /etc/nginx/conf.d/alipay.conf "/var/www/alipay/backups/nginx-alipay-$TS.conf"
  fi
  cp "$RELEASE_DIR/deploy/nginx/alipay.conf" /etc/nginx/conf.d/alipay.conf
  nginx -t
  systemctl reload nginx
}

case "$MODE" in
  all)
    deploy_frontend
    deploy_backend
    ;;
  all_config)
    deploy_frontend
    deploy_backend
    deploy_config
    ;;
  frontend)
    deploy_frontend
    ;;
  backend)
    deploy_backend
    ;;
  config)
    deploy_config
    ;;
  *)
    echo "Unknown deploy mode: $MODE" >&2
    exit 1
    ;;
esac

systemctl is-active nginx
systemctl is-active payment-gateway-api merchant-api mall-api
curl -fsS -I https://mall.linsy.online >/dev/null
curl -fsS -I https://merchant.linsy.online >/dev/null
curl -fsS -I https://admin.linsy.online >/dev/null
curl -fsS https://api.linsy.online/api/mall/catalog/categories >/dev/null

echo "Deploy completed: mode=$MODE ts=$TS"
REMOTE_SCRIPT

echo "Production deploy completed."
