#!/usr/bin/env sh
set -eu

RELEASE_DIR=${1:-/tmp/alipay-gateway-aliyun-release}
BASE_DIR=/opt/alipay-gateway-chengxinkeji
BACKUP_ROOT=/opt/alipay-gateway-chengxinkeji-backups
WEB_ROOT=/www/wwwroot/alipay-gateway-chengxinkeji
ENV_DIR=/etc/alipay-gateway-chengxinkeji
VHOST_FILE=/www/server/panel/vhost/nginx/alipay-gateway-chengxinkeji.conf
CERT_ROOT=/www/server/panel/vhost/cert
MYSQL_SERVICE=chengxinkeji-payment-gateway-mysql.service
GATEWAY_SERVICE=chengxinkeji-payment-gateway-api.service
TESTPAY_SERVICE=chengxinkeji-gateway-pay-test-api.service
MYSQL_PORT=13306
GATEWAY_PORT=18090
TESTPAY_PORT=18091
TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="$BACKUP_ROOT/$TS"

test -s "$RELEASE_DIR/backend/payment-gateway-api.jar"
test -s "$RELEASE_DIR/backend/gateway-pay-test-api.jar"
test -s "$RELEASE_DIR/bootstrap/gateway-source.env"
test -s "$RELEASE_DIR/bootstrap/testpay-source.env"
test -s "$RELEASE_DIR/bootstrap/bootstrap.sql"

if [ ! -x /www/server/mysql/bin/mysqld ] || [ ! -x /www/server/mysql/bin/mysql ]; then
  echo "BaoTa MySQL binaries were not found; refusing to modify the existing MySQL installation." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | head -1 | grep -Eq 'version "(17|18|19|2[0-9])'; then
  export DEBIAN_FRONTEND=noninteractive
  export NEEDRESTART_MODE=l
  apt-get update
  apt-get install -y --no-install-recommends openjdk-17-jre-headless
fi

if ! id alipaygw >/dev/null 2>&1; then
  useradd --system --home-dir "$BASE_DIR" --shell /usr/sbin/nologin alipaygw
fi

systemctl stop "$TESTPAY_SERVICE" 2>/dev/null || true
systemctl stop "$GATEWAY_SERVICE" 2>/dev/null || true

port_is_listening() {
  ss -lnt | awk 'NR > 1 {print $4}' | grep -Eq "[:.]$1$"
}

for port in "$GATEWAY_PORT" "$TESTPAY_PORT"; do
  if port_is_listening "$port"; then
    echo "Port $port is already used by another service; deployment stopped." >&2
    exit 1
  fi
done

if port_is_listening "$MYSQL_PORT" && ! systemctl is-active --quiet "$MYSQL_SERVICE"; then
  echo "Port $MYSQL_PORT is already used by another service; deployment stopped." >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR" "$BASE_DIR/apps" "$BASE_DIR/runtime/payment-gateway-api" \
  "$BASE_DIR/runtime/gateway-pay-test-api" "$BASE_DIR/mysql" "$WEB_ROOT/admin" \
  "$WEB_ROOT/client" "$WEB_ROOT/testpay" "$ENV_DIR"

[ -f "$BASE_DIR/apps/payment-gateway-api.jar" ] && cp "$BASE_DIR/apps/payment-gateway-api.jar" "$BACKUP_DIR/" || true
[ -f "$BASE_DIR/apps/gateway-pay-test-api.jar" ] && cp "$BASE_DIR/apps/gateway-pay-test-api.jar" "$BACKUP_DIR/" || true
[ -f "$VHOST_FILE" ] && cp "$VHOST_FILE" "$BACKUP_DIR/nginx.conf" || true
[ -f "$ENV_DIR/gateway.env" ] && cp "$ENV_DIR/gateway.env" "$BACKUP_DIR/gateway.env" || true
[ -f "$ENV_DIR/testpay.env" ] && cp "$ENV_DIR/testpay.env" "$BACKUP_DIR/testpay.env" || true
tar -czf "$BACKUP_DIR/admin.tar.gz" -C "$WEB_ROOT/admin" . 2>/dev/null || true
tar -czf "$BACKUP_DIR/client.tar.gz" -C "$WEB_ROOT/client" . 2>/dev/null || true
tar -czf "$BACKUP_DIR/testpay.tar.gz" -C "$WEB_ROOT/testpay" . 2>/dev/null || true
chmod -R go-rwx "$BACKUP_DIR"

cp "$RELEASE_DIR/deploy/mysql/my.cnf" "$BASE_DIR/mysql/my.cnf"
chown -R alipaygw:alipaygw "$BASE_DIR/mysql" "$BASE_DIR/runtime"
chmod 750 "$BASE_DIR/mysql"

cp "$RELEASE_DIR/deploy/systemd/"*.service /etc/systemd/system/
systemctl daemon-reload

FRESH_DATABASE=false
if [ ! -d "$BASE_DIR/mysql/data/mysql" ]; then
  FRESH_DATABASE=true
  mkdir -p "$BASE_DIR/mysql/data"
  chown -R alipaygw:alipaygw "$BASE_DIR/mysql"
  /www/server/mysql/bin/mysqld --defaults-file="$BASE_DIR/mysql/my.cnf" --initialize-insecure --user=alipaygw
fi

systemctl enable "$MYSQL_SERVICE"
systemctl restart "$MYSQL_SERVICE"
MYSQL_SECRET_FILE="$ENV_DIR/mysql-root.env"
if [ "$FRESH_DATABASE" = "true" ]; then
  for i in $(seq 1 60); do
    if /www/server/mysql/bin/mysqladmin --protocol=socket --socket="$BASE_DIR/mysql/mysql.sock" -uroot ping >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  /www/server/mysql/bin/mysqladmin --protocol=socket --socket="$BASE_DIR/mysql/mysql.sock" -uroot ping >/dev/null
  DB_ROOT_PASSWORD=$(openssl rand -hex 24)
  DB_APP_PASSWORD=$(openssl rand -hex 24)
  /www/server/mysql/bin/mysql --protocol=socket --socket="$BASE_DIR/mysql/mysql.sock" -uroot <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '$DB_ROOT_PASSWORD';
CREATE DATABASE alipay_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'alipay_gateway'@'127.0.0.1' IDENTIFIED BY '$DB_APP_PASSWORD';
GRANT ALL PRIVILEGES ON alipay_gateway.* TO 'alipay_gateway'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
  printf "MYSQL_ROOT_PASSWORD='%s'\nMYSQL_APP_PASSWORD='%s'\n" "$DB_ROOT_PASSWORD" "$DB_APP_PASSWORD" > "$MYSQL_SECRET_FILE"
  chmod 600 "$MYSQL_SECRET_FILE"
else
  test -s "$MYSQL_SECRET_FILE"
  set -a
  . "$MYSQL_SECRET_FILE"
  set +a
  for i in $(seq 1 60); do
    if MYSQL_PWD="$MYSQL_ROOT_PASSWORD" /www/server/mysql/bin/mysqladmin --protocol=socket --socket="$BASE_DIR/mysql/mysql.sock" -uroot ping >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" /www/server/mysql/bin/mysqladmin --protocol=socket --socket="$BASE_DIR/mysql/mysql.sock" -uroot ping >/dev/null
fi

set -a
. "$MYSQL_SECRET_FILE"
set +a

cp "$RELEASE_DIR/bootstrap/gateway-source.env" "$ENV_DIR/gateway.env"
cat >> "$ENV_DIR/gateway.env" <<EOF
SPRING_PROFILES_ACTIVE='prod'
SERVER_ADDRESS='127.0.0.1'
PAYMENT_GATEWAY_PORT='$GATEWAY_PORT'
GATEWAY_DATASOURCE_URL='jdbc:mysql://127.0.0.1:$MYSQL_PORT/alipay_gateway?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
GATEWAY_DATASOURCE_USERNAME='alipay_gateway'
GATEWAY_DATASOURCE_PASSWORD='$MYSQL_APP_PASSWORD'
GATEWAY_DEMO_APP_ENABLED='false'
ZHENBAOGE_NOTIFY_URL='https://api.chengxinkeji.xin/api/gateway/zhenbaoge/notify'
EOF

cp "$RELEASE_DIR/bootstrap/testpay-source.env" "$ENV_DIR/testpay.env"
cat >> "$ENV_DIR/testpay.env" <<EOF
TESTPAY_SERVER_ADDRESS='127.0.0.1'
TESTPAY_SERVER_PORT='$TESTPAY_PORT'
TESTPAY_GATEWAY_BASE_URL='https://api.chengxinkeji.xin'
TESTPAY_RETURN_URL='https://testpay.chengxinkeji.xin/pay-result.html'
TESTPAY_BUSINESS_NOTIFY_URL='https://testpay.chengxinkeji.xin/api/testpay/notify'
EOF

chown root:alipaygw "$ENV_DIR/gateway.env" "$ENV_DIR/testpay.env"
chmod 640 "$ENV_DIR/gateway.env" "$ENV_DIR/testpay.env"

cp "$RELEASE_DIR/backend/payment-gateway-api.jar" "$BASE_DIR/apps/payment-gateway-api.jar"
cp "$RELEASE_DIR/backend/gateway-pay-test-api.jar" "$BASE_DIR/apps/gateway-pay-test-api.jar"
chown root:alipaygw "$BASE_DIR/apps/"*.jar
chmod 640 "$BASE_DIR/apps/"*.jar

rm -rf "$WEB_ROOT/admin"/* "$WEB_ROOT/client"/* "$WEB_ROOT/testpay"/*
cp -a "$RELEASE_DIR/frontend/admin/." "$WEB_ROOT/admin/"
cp -a "$RELEASE_DIR/frontend/client/." "$WEB_ROOT/client/"
cp -a "$RELEASE_DIR/frontend/testpay/." "$WEB_ROOT/testpay/"
chown -R www:www "$WEB_ROOT"

systemctl enable "$GATEWAY_SERVICE"
systemctl restart "$GATEWAY_SERVICE"
TABLE_READY=false
for i in $(seq 1 60); do
  if MYSQL_PWD="$MYSQL_APP_PASSWORD" /www/server/mysql/bin/mysql -h127.0.0.1 -P"$MYSQL_PORT" -ualipay_gateway alipay_gateway -N -B \
      -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='alipay_gateway' AND TABLE_NAME='gateway_app'" 2>/dev/null | grep -q '^1$'; then
    TABLE_READY=true
    break
  fi
  sleep 1
done
if [ "$TABLE_READY" != "true" ] || ! systemctl is-active --quiet "$GATEWAY_SERVICE"; then
  journalctl -u "$GATEWAY_SERVICE" -n 80 --no-pager >&2 || true
  echo "Payment gateway API failed to start or initialize its schema." >&2
  exit 1
fi

BOOTSTRAP_MARKER="$BASE_DIR/.bootstrap-imported"
if [ ! -f "$BOOTSTRAP_MARKER" ]; then
  systemctl stop "$GATEWAY_SERVICE"
  MYSQL_PWD="$MYSQL_APP_PASSWORD" /www/server/mysql/bin/mysql -h127.0.0.1 -P"$MYSQL_PORT" -ualipay_gateway alipay_gateway \
    < "$RELEASE_DIR/bootstrap/bootstrap.sql"
  set -a
  . "$RELEASE_DIR/bootstrap/testpay-source.env"
  set +a
  MYSQL_PWD="$MYSQL_APP_PASSWORD" /www/server/mysql/bin/mysql -h127.0.0.1 -P"$MYSQL_PORT" -ualipay_gateway alipay_gateway \
    -e "UPDATE gateway_app SET notify_url='https://testpay.chengxinkeji.xin/api/testpay/notify' WHERE app_id='$TESTPAY_GATEWAY_APP_ID'"
  touch "$BOOTSTRAP_MARKER"
  chown alipaygw:alipaygw "$BOOTSTRAP_MARKER"
  systemctl start "$GATEWAY_SERVICE"
fi

systemctl enable "$TESTPAY_SERVICE"
systemctl restart "$TESTPAY_SERVICE"
if ! systemctl is-active --quiet "$TESTPAY_SERVICE"; then
  journalctl -u "$TESTPAY_SERVICE" -n 80 --no-pager >&2 || true
  echo "Gateway pay test API failed to start." >&2
  exit 1
fi

for domain in admin.chengxinkeji.xin api.chengxinkeji.xin client.chengxinkeji.xin testpay.chengxinkeji.xin; do
  mkdir -p "$CERT_ROOT/$domain"
  cp "$RELEASE_DIR/deploy/ssl/$domain/fullchain.pem" "$CERT_ROOT/$domain/fullchain.pem"
  cp "$RELEASE_DIR/deploy/ssl/$domain/privkey.pem" "$CERT_ROOT/$domain/privkey.pem"
  chmod 600 "$CERT_ROOT/$domain/privkey.pem"
done

cp "$RELEASE_DIR/deploy/nginx/alipay-gateway-chengxinkeji.conf" "$VHOST_FILE"
if ! nginx -t; then
  if [ -f "$BACKUP_DIR/nginx.conf" ]; then
    cp "$BACKUP_DIR/nginx.conf" "$VHOST_FILE"
  else
    rm -f "$VHOST_FILE"
  fi
  nginx -t
  echo "New Nginx configuration failed validation and was rolled back." >&2
  exit 1
fi
nginx -s reload

systemctl is-active "$MYSQL_SERVICE"
systemctl is-active "$GATEWAY_SERVICE"
systemctl is-active "$TESTPAY_SERVICE"

MYSQL_PWD="$MYSQL_APP_PASSWORD" /www/server/mysql/bin/mysql -h127.0.0.1 -P"$MYSQL_PORT" -ualipay_gateway alipay_gateway -N -B -e "
SELECT 'gateway_app', COUNT(*) FROM gateway_app
UNION ALL SELECT 'gateway_client', COUNT(*) FROM gateway_client
UNION ALL SELECT 'gateway_client_user', COUNT(*) FROM gateway_client_user
UNION ALL SELECT 'gateway_pay_order', COUNT(*) FROM gateway_pay_order;
"

echo "Aliyun gateway deployment completed: backup=$BACKUP_DIR"
