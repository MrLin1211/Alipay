#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DIST_DIR="$ROOT_DIR/dist-production"
REMOTE="${REMOTE:-root@119.45.58.43}"
REMOTE_RELEASE_DIR="${REMOTE_RELEASE_DIR:-/tmp/alipay-release}"
PACKAGE_PATH="${PACKAGE_PATH:-/tmp/alipay-release.tar.gz}"
SSH_IDENTITY="${SSH_IDENTITY:-}"
MODE="all"
SKIP_BUILD=0

usage() {
  cat <<'EOF'
Usage:
  sh scripts/deploy-production.sh [options]

Options:
  --all             Build and deploy frontend and backend. Default.
  --frontend-only   Deploy only mall, merchant, admin, client, and testpay frontend files.
  --app-only        Build and deploy only the mall-miniapp H5 frontend.
  --app-with-config Build and deploy mall-miniapp H5, nginx config, and its certificate.
  --backend-only    Deploy only backend jar files and restart services.
  --config-only     Deploy only systemd and nginx config files.
  --with-config     Deploy frontend, backend, systemd, and nginx config files.
  --skip-build      Reuse existing dist-production instead of running build-production.sh.
  -h, --help        Show this help.

Environment:
  REMOTE=root@119.45.58.43
  REMOTE_RELEASE_DIR=/tmp/alipay-release
  PACKAGE_PATH=/tmp/alipay-release.tar.gz
  SSH_IDENTITY=/path/to/private-key.pem
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
    --app-only)
      MODE="app"
      ;;
    --app-with-config)
      MODE="app_config"
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
  case "$MODE" in
    app|app_config) sh "$ROOT_DIR/scripts/build-miniapp-h5-production.sh" ;;
    *) sh "$ROOT_DIR/scripts/build-production.sh" ;;
  esac
elif [ ! -d "$DIST_DIR" ]; then
  echo "Missing $DIST_DIR. Run without --skip-build first." >&2
  exit 1
fi

PACKAGE_SOURCE="$DIST_DIR"
APP_PACKAGE_DIR=""
case "$MODE" in
  app|app_config)
    APP_PACKAGE_DIR=$(mktemp -d /tmp/alipay-app-release.XXXXXX)
    mkdir -p "$APP_PACKAGE_DIR/frontend"
    cp -R "$DIST_DIR/frontend/app" "$APP_PACKAGE_DIR/frontend/app"
    cp -R "$DIST_DIR/deploy" "$APP_PACKAGE_DIR/deploy"
    PACKAGE_SOURCE="$APP_PACKAGE_DIR"
    ;;
esac

tar -czf "$PACKAGE_PATH" -C "$PACKAGE_SOURCE" .
[ -z "$APP_PACKAGE_DIR" ] || rm -rf "$APP_PACKAGE_DIR"
if [ -n "$SSH_IDENTITY" ]; then
  scp -i "$SSH_IDENTITY" -o IdentitiesOnly=yes "$PACKAGE_PATH" "$REMOTE:/tmp/alipay-release.tar.gz"
else
  scp "$PACKAGE_PATH" "$REMOTE:/tmp/alipay-release.tar.gz"
fi

if [ -n "$SSH_IDENTITY" ]; then
  SSH_COMMAND="ssh -i $SSH_IDENTITY -o IdentitiesOnly=yes"
else
  SSH_COMMAND="ssh"
fi

$SSH_COMMAND "$REMOTE" sh -s -- "$MODE" "$REMOTE_RELEASE_DIR" <<'REMOTE_SCRIPT'
set -eu

MODE="$1"
RELEASE_DIR="$2"
TS=$(date +%Y%m%d-%H%M%S)

mkdir -p "$RELEASE_DIR" /var/www/alipay/backups /opt/alipay/backups
rm -rf "$RELEASE_DIR"/*
tar -xzf /tmp/alipay-release.tar.gz -C "$RELEASE_DIR"

deploy_frontend() {
  mkdir -p /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin /var/www/alipay/client /var/www/alipay/testpay

  tar -czf "/var/www/alipay/backups/mall-$TS.tar.gz" -C /var/www/alipay/mall . 2>/dev/null || true
  tar -czf "/var/www/alipay/backups/merchant-$TS.tar.gz" -C /var/www/alipay/merchant . 2>/dev/null || true
  tar -czf "/var/www/alipay/backups/admin-$TS.tar.gz" -C /var/www/alipay/admin . 2>/dev/null || true
  tar -czf "/var/www/alipay/backups/client-$TS.tar.gz" -C /var/www/alipay/client . 2>/dev/null || true
  tar -czf "/var/www/alipay/backups/testpay-$TS.tar.gz" -C /var/www/alipay/testpay . 2>/dev/null || true

  rm -rf /var/www/alipay/mall/* /var/www/alipay/merchant/* /var/www/alipay/admin/* /var/www/alipay/client/* /var/www/alipay/testpay/*
  cp -a "$RELEASE_DIR/frontend/mall/." /var/www/alipay/mall/
  cp -a "$RELEASE_DIR/frontend/merchant/." /var/www/alipay/merchant/
  cp -a "$RELEASE_DIR/frontend/admin/." /var/www/alipay/admin/
  cp -a "$RELEASE_DIR/frontend/client/." /var/www/alipay/client/
  cp -a "$RELEASE_DIR/frontend/testpay/." /var/www/alipay/testpay/
  chown -R nginx:nginx /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin /var/www/alipay/client /var/www/alipay/testpay
}

deploy_app() {
  mkdir -p /var/www/alipay/app
  tar -czf "/var/www/alipay/backups/app-$TS.tar.gz" -C /var/www/alipay/app . 2>/dev/null || true
  rm -rf /var/www/alipay/app/*
  cp -a "$RELEASE_DIR/frontend/app/." /var/www/alipay/app/
  chown -R nginx:nginx /var/www/alipay/app
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

  if [ -d "$RELEASE_DIR/deploy/ssl/app.linsy.online" ]; then
    mkdir -p /etc/nginx/ssl/app.linsy.online
    cp "$RELEASE_DIR/deploy/ssl/app.linsy.online/app.linsy.online_bundle.crt" /etc/nginx/ssl/app.linsy.online/
    cp "$RELEASE_DIR/deploy/ssl/app.linsy.online/app.linsy.online.key" /etc/nginx/ssl/app.linsy.online/
    chmod 600 /etc/nginx/ssl/app.linsy.online/app.linsy.online.key
  fi

  if [ -d "$RELEASE_DIR/deploy/ssl/client.linsy.online_nginx" ]; then
    mkdir -p /etc/nginx/ssl/client.linsy.online_nginx
    cp "$RELEASE_DIR/deploy/ssl/client.linsy.online_nginx/client.linsy.online_bundle.crt" /etc/nginx/ssl/client.linsy.online_nginx/
    cp "$RELEASE_DIR/deploy/ssl/client.linsy.online_nginx/client.linsy.online.key" /etc/nginx/ssl/client.linsy.online_nginx/
    chmod 600 /etc/nginx/ssl/client.linsy.online_nginx/client.linsy.online.key
  fi

  if [ -d "$RELEASE_DIR/deploy/ssl/testpay.linsy.online_nginx" ]; then
    mkdir -p /etc/nginx/ssl/testpay.linsy.online_nginx
    cp "$RELEASE_DIR/deploy/ssl/testpay.linsy.online_nginx/testpay.linsy.online_bundle.crt" /etc/nginx/ssl/testpay.linsy.online_nginx/
    cp "$RELEASE_DIR/deploy/ssl/testpay.linsy.online_nginx/testpay.linsy.online.key" /etc/nginx/ssl/testpay.linsy.online_nginx/
    chmod 600 /etc/nginx/ssl/testpay.linsy.online_nginx/testpay.linsy.online.key
  fi

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
    deploy_app
    deploy_backend
    ;;
  all_config)
    deploy_frontend
    deploy_app
    deploy_backend
    deploy_config
    ;;
  frontend)
    deploy_frontend
    ;;
  app)
    deploy_app
    ;;
  app_config)
    deploy_app
    deploy_config
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
case "$MODE" in
  backend|all|all_config)
    systemctl is-active payment-gateway-api merchant-api mall-api
    ;;
esac
curl -fsS -I https://mall.linsy.online >/dev/null
if [ "$MODE" = "app" ] || [ "$MODE" = "app_config" ] || [ "$MODE" = "all_config" ]; then
  curl -fsS -I https://app.linsy.online >/dev/null
fi
curl -fsS -I https://merchant.linsy.online >/dev/null
curl -fsS -I https://admin.linsy.online >/dev/null
curl -fsS -I https://client.linsy.online >/dev/null
curl -fsS -I https://testpay.linsy.online >/dev/null
curl -fsS https://api.linsy.online/api/mall/catalog/categories >/dev/null

echo "Deploy completed: mode=$MODE ts=$TS"
REMOTE_SCRIPT

echo "Production deploy completed."
