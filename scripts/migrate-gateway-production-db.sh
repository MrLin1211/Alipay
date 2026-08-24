#!/usr/bin/env sh
set -eu

if [ "${CONFIRM_GATEWAY_DB_MIGRATION:-}" != "yes" ]; then
  echo "Set CONFIRM_GATEWAY_DB_MIGRATION=yes to run this one-time migration." >&2
  exit 1
fi

set -a
. /etc/alipay/common.env
set +a

SOURCE_DB=$(printf '%s' "$GATEWAY_DATASOURCE_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')
TARGET_DB=payment_gateway

if [ "$SOURCE_DB" = "$TARGET_DB" ]; then
  echo "Gateway database is already isolated in $TARGET_DB."
  exit 0
fi

BACKUP_DIR="/var/www/alipay/backups/gateway-isolation-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"

MYSQL="mysql -h 127.0.0.1 -P 3306 -u $GATEWAY_DATASOURCE_USERNAME"
DUMP="mysqldump --no-tablespaces -h 127.0.0.1 -P 3306 -u $GATEWAY_DATASOURCE_USERNAME"

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $DUMP "$TARGET_DB" > "$BACKUP_DIR/payment_gateway-before.sql"
MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $DUMP "$SOURCE_DB" \
  gateway_admin_user gateway_admin_session gateway_app gateway_client gateway_client_user gateway_client_session \
  zhenbaoge_channel_config gateway_pay_order gateway_refund_order gateway_notify_record \
  > "$BACKUP_DIR/gateway-core.sql"

test -s "$BACKUP_DIR/payment_gateway-before.sql"
test -s "$BACKUP_DIR/gateway-core.sql"

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL "$TARGET_DB" <<'SQL'
SET FOREIGN_KEY_CHECKS = 0;
SET GROUP_CONCAT_MAX_LEN = 1000000;
SELECT GROUP_CONCAT(CONCAT('`', TABLE_NAME, '`'))
INTO @tables
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'payment_gateway';
SET @drop_sql = IF(@tables IS NULL, 'SELECT 1', CONCAT('DROP TABLE ', @tables));
PREPARE statement FROM @drop_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;
SET FOREIGN_KEY_CHECKS = 1;
SQL

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL "$TARGET_DB" < "$BACKUP_DIR/gateway-core.sql"

NEW_URL=$(printf '%s' "$GATEWAY_DATASOURCE_URL" | sed -E "s#(jdbc:mysql://[^/]+/)[^?]+#\1$TARGET_DB#")
TMP_ENV=$(mktemp)
grep -v '^GATEWAY_DATASOURCE_URL=' /etc/alipay/common.env > "$TMP_ENV" || true
printf "GATEWAY_DATASOURCE_URL='%s'\n" "$NEW_URL" >> "$TMP_ENV"
cat "$TMP_ENV" > /etc/alipay/common.env
rm -f "$TMP_ENV"
chmod 600 /etc/alipay/common.env

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" $MYSQL "$TARGET_DB" -N -B -e "
SELECT 'gateway_admin_user', COUNT(*) FROM gateway_admin_user
UNION ALL SELECT 'gateway_app', COUNT(*) FROM gateway_app
UNION ALL SELECT 'gateway_client', COUNT(*) FROM gateway_client
UNION ALL SELECT 'gateway_client_user', COUNT(*) FROM gateway_client_user
UNION ALL SELECT 'gateway_pay_order', COUNT(*) FROM gateway_pay_order
UNION ALL SELECT 'gateway_refund_order', COUNT(*) FROM gateway_refund_order
UNION ALL SELECT 'gateway_notify_record', COUNT(*) FROM gateway_notify_record
UNION ALL SELECT 'zhenbaoge_channel_config', COUNT(*) FROM zhenbaoge_channel_config;
"

echo "Gateway database migration completed. Backup: $BACKUP_DIR"
