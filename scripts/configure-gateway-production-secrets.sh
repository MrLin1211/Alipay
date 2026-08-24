#!/usr/bin/env sh
set -eu

if [ -z "${TESTPAY_ENV_B64:-}" ] || [ -z "${ZHENBAOGE_ENV_B64:-}" ]; then
  echo "TESTPAY_ENV_B64 and ZHENBAOGE_ENV_B64 are required." >&2
  exit 1
fi

printf '%s' "$TESTPAY_ENV_B64" | base64 -d > /etc/alipay/testpay.env
chmod 600 /etc/alipay/testpay.env

CHANNEL_ENV=$(mktemp)
printf '%s' "$ZHENBAOGE_ENV_B64" | base64 -d > "$CHANNEL_ENV"
set -a
. "$CHANNEL_ENV"
set +a
rm -f "$CHANNEL_ENV"

set_env() {
  key="$1"
  value="$2"
  temp=$(mktemp)
  grep -v "^$key=" /etc/alipay/common.env > "$temp" || true
  printf "%s='%s'\n" "$key" "$value" >> "$temp"
  cat "$temp" > /etc/alipay/common.env
  rm -f "$temp"
}

set_env ZHENBAOGE_EXTERNAL_ID "$ZHENBAOGE_EXTERNAL_ID"
set_env ZHENBAOGE_MD5_KEY "$ZHENBAOGE_MD5_KEY"
set_env ZHENBAOGE_AES_KEY "$ZHENBAOGE_AES_KEY"
chmod 600 /etc/alipay/common.env

set -a
. /etc/alipay/common.env
. /etc/alipay/testpay.env
set +a

DB=$(printf '%s' "$GATEWAY_DATASOURCE_URL" | sed -E 's#^jdbc:mysql://[^/]+/([^?]+).*#\1#')
MD5_HEX=$(printf '%s' "$ZHENBAOGE_MD5_KEY" | od -An -tx1 | tr -d ' \n')
AES_HEX=$(printf '%s' "$ZHENBAOGE_AES_KEY" | od -An -tx1 | tr -d ' \n')
NOTIFY_HEX=$(printf '%s' "$TESTPAY_BUSINESS_NOTIFY_URL" | od -An -tx1 | tr -d ' \n')

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" mysql -h 127.0.0.1 -P 3306 -u "$GATEWAY_DATASOURCE_USERNAME" "$DB" -e "
UPDATE zhenbaoge_channel_config
SET external_id = '$ZHENBAOGE_EXTERNAL_ID', md5_key = UNHEX('$MD5_HEX'), aes_key = UNHEX('$AES_HEX'), updated_at = NOW()
WHERE enabled = 1;
UPDATE gateway_app
SET notify_url = UNHEX('$NOTIFY_HEX'), updated_at = NOW()
WHERE app_id = '$TESTPAY_GATEWAY_APP_ID';
"

MYSQL_PWD="$GATEWAY_DATASOURCE_PASSWORD" mysql -h 127.0.0.1 -P 3306 -u "$GATEWAY_DATASOURCE_USERNAME" "$DB" -N -B -e "
SELECT external_id, LENGTH(md5_key), LENGTH(aes_key), host, notify_url
FROM zhenbaoge_channel_config WHERE enabled = 1 LIMIT 1;
SELECT app_id, app_name, notify_url FROM gateway_app WHERE app_id = '$TESTPAY_GATEWAY_APP_ID';
"
