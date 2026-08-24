# 生产环境打包和部署说明

本文档按当前线上规则维护：前端域名只提供静态页面，所有接口统一走 `https://api.linsy.online`。

## 1. 线上域名和服务

| 服务 | 线上地址 | 部署位置 |
| --- | --- | --- |
| 商城 | `https://mall.linsy.online` | `/var/www/alipay/mall` |
| uni-app H5 商城 | `https://app.linsy.online` | `/var/www/alipay/app` |
| 商家后台 | `https://merchant.linsy.online` | `/var/www/alipay/merchant` |
| 管理后台 | `https://admin.linsy.online` | `/var/www/alipay/admin` |
| 接入方支付后台 | `https://client.linsy.online` | `/var/www/alipay/client` |
| 支付测试端 | `https://testpay.linsy.online` | `/var/www/alipay/testpay` |
| 商城 API | `https://api.linsy.online/api/mall/**` | `127.0.0.1:8080` |
| 商家 API | `https://api.linsy.online/api/merchant/**` | `127.0.0.1:8081` |
| 管理后台 API | `https://api.linsy.online/api/admin/**` | `127.0.0.1:8081` |
| 支付网关 API | `https://api.linsy.online/api/gateway/**` | `127.0.0.1:8090` |
| 上传文件访问 | `https://api.linsy.online/uploads/**` | `/var/www/alipay/uploads` |

不要在 `mall.linsy.online`、`app.linsy.online`、`merchant.linsy.online`、`admin.linsy.online`、`client.linsy.online`、`testpay.linsy.online` 下再配置 API 代理。前端生产构建时应写入 `https://api.linsy.online`。

## 2. 日常上线流程

日常修改代码后，优先使用部署脚本：

```bash
cd /Users/mrlin/Documents/Alipay
sh scripts/deploy-production.sh
```

该命令会自动执行：

1. 运行 `scripts/build-production.sh`。
2. 生成 `/tmp/alipay-release.tar.gz`。
3. 上传到 `root@119.45.58.43:/tmp/alipay-release.tar.gz`。
4. 在服务器解压到 `/tmp/alipay-release`。
5. 备份旧前端和旧 jar。
6. 替换前端静态文件和三个后端 jar。
7. 重启 `payment-gateway-api`、`merchant-api`、`mall-api`。
8. 执行基础线上验证。

默认不会覆盖 Nginx 配置，避免证书路径和线上实际配置不一致时影响访问。

如果只改了页面、样式、图标、提示文案：

```bash
sh scripts/deploy-production.sh --frontend-only
```

如果只发布 `mall-miniapp` 的 H5 版本：

```bash
sh scripts/deploy-production.sh --app-only
```

首次发布或证书、Nginx 配置有变化时：

```bash
sh scripts/deploy-production.sh --app-with-config
```

如果只改了 Java 后端：

```bash
sh scripts/deploy-production.sh --backend-only
```

如果只改了 `deploy/` 下的 systemd 或 Nginx 配置：

```bash
sh scripts/deploy-production.sh --config-only
```

如果确实需要在一次上线中同时部署应用和配置：

```bash
sh scripts/deploy-production.sh --with-config
```

如果已经打包过，只想复用现有 `dist-production`：

```bash
sh scripts/deploy-production.sh --frontend-only --skip-build
```

## 3. 本地打包脚本

打包脚本位置：

```text
scripts/build-production.sh
```

手动打包：

```bash
cd /Users/mrlin/Documents/Alipay
sh scripts/build-production.sh
```

产物目录：

```text
dist-production/
├── backend/
│   ├── mall-api.jar
│   ├── merchant-api.jar
│   └── payment-gateway-api.jar
├── frontend/
│   ├── mall/
│   ├── app/
│   ├── merchant/
│   ├── admin/
│   ├── client/
│   └── testpay/
└── deploy/
```

前端打包时会注入：

```text
VITE_API_BASE_URL=https://api.linsy.online
VITE_GATEWAY_BASE_URL=https://api.linsy.online
```

## 4. 手动上传和部署

如果不用 `scripts/deploy-production.sh`，可以手动执行。

本地压缩：

```bash
tar -czf /tmp/alipay-release.tar.gz -C dist-production .
```

上传：

```bash
scp /tmp/alipay-release.tar.gz root@119.45.58.43:/tmp/alipay-release.tar.gz
```

登录服务器：

```bash
ssh root@119.45.58.43
```

服务器解压：

```bash
rm -rf /tmp/alipay-release
mkdir -p /tmp/alipay-release
tar -xzf /tmp/alipay-release.tar.gz -C /tmp/alipay-release
```

### 4.1 只部署前端

```bash
TS=$(date +%Y%m%d-%H%M%S)
mkdir -p /var/www/alipay/backups

tar -czf /var/www/alipay/backups/mall-$TS.tar.gz -C /var/www/alipay/mall .
tar -czf /var/www/alipay/backups/merchant-$TS.tar.gz -C /var/www/alipay/merchant .
tar -czf /var/www/alipay/backups/admin-$TS.tar.gz -C /var/www/alipay/admin .
tar -czf /var/www/alipay/backups/client-$TS.tar.gz -C /var/www/alipay/client .
tar -czf /var/www/alipay/backups/testpay-$TS.tar.gz -C /var/www/alipay/testpay .

rm -rf /var/www/alipay/mall/*
rm -rf /var/www/alipay/merchant/*
rm -rf /var/www/alipay/admin/*
rm -rf /var/www/alipay/client/*
rm -rf /var/www/alipay/testpay/*

cp -a /tmp/alipay-release/frontend/mall/. /var/www/alipay/mall/
cp -a /tmp/alipay-release/frontend/merchant/. /var/www/alipay/merchant/
cp -a /tmp/alipay-release/frontend/admin/. /var/www/alipay/admin/
cp -a /tmp/alipay-release/frontend/client/. /var/www/alipay/client/
cp -a /tmp/alipay-release/frontend/testpay/. /var/www/alipay/testpay/

chown -R nginx:nginx /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin /var/www/alipay/client /var/www/alipay/testpay
```

### 4.2 只部署后端

```bash
TS=$(date +%Y%m%d-%H%M%S)
mkdir -p /opt/alipay/backups

cp /opt/alipay/mall-api.jar /opt/alipay/backups/mall-api-$TS.jar
cp /opt/alipay/merchant-api.jar /opt/alipay/backups/merchant-api-$TS.jar
cp /opt/alipay/payment-gateway-api.jar /opt/alipay/backups/payment-gateway-api-$TS.jar

cp /tmp/alipay-release/backend/mall-api.jar /opt/alipay/mall-api.jar
cp /tmp/alipay-release/backend/merchant-api.jar /opt/alipay/merchant-api.jar
cp /tmp/alipay-release/backend/payment-gateway-api.jar /opt/alipay/payment-gateway-api.jar

chown alipay:alipay /opt/alipay/*.jar

systemctl restart payment-gateway-api
systemctl restart merchant-api
systemctl restart mall-api
```

### 4.3 部署 Nginx 和 systemd 配置

首次部署或配置文件变更时执行：

```bash
cp /tmp/alipay-release/deploy/systemd/*.service /etc/systemd/system/
systemctl daemon-reload

cp /etc/nginx/conf.d/alipay.conf /var/www/alipay/backups/nginx-alipay-$(date +%Y%m%d-%H%M%S).conf
cp /tmp/alipay-release/deploy/nginx/alipay.conf /etc/nginx/conf.d/alipay.conf
nginx -t
systemctl reload nginx
```

## 5. 首次部署准备

### 5.1 服务器软件

服务器需要：

```bash
java -version
mvn -version
node -v
npm -v
nginx -v
mysql --version
```

Java 需要 17。

### 5.2 数据库

当前生产环境使用统一库：

```sql
CREATE DATABASE alipay_pay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'alipay_user'@'127.0.0.1' IDENTIFIED BY 'CHANGE_ME_DB_PASSWORD';
GRANT ALL PRIVILEGES ON alipay_pay.* TO 'alipay_user'@'127.0.0.1';
FLUSH PRIVILEGES;
```

### 5.3 目录

```bash
useradd -r -s /usr/sbin/nologin alipay || true
mkdir -p /opt/alipay /etc/alipay /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin /var/www/alipay/uploads /var/www/alipay/backups
chown -R alipay:alipay /opt/alipay
chown -R nginx:nginx /var/www/alipay
```

### 5.4 环境变量

```bash
mkdir -p /etc/alipay
cp /tmp/alipay-release/deploy/production.env.example /etc/alipay/alipay.env
chmod 600 /etc/alipay/alipay.env
vim /etc/alipay/alipay.env
```

必须替换所有 `CHANGE_ME_*`。

### 5.5 SSL 证书

证书目录按 `deploy/nginx/alipay.conf` 中的路径准备：

```text
/etc/nginx/ssl/app.linsy.online/
/etc/nginx/ssl/mall.linsy.online_nginx/
/etc/nginx/ssl/merchant.linsy.online_nginx/
/etc/nginx/ssl/admin.linsy.online_nginx/
/etc/nginx/ssl/client.linsy.online_nginx/
/etc/nginx/ssl/testpay.linsy.online_nginx/
/etc/nginx/ssl/api.linsy.online_nginx/
```

每个目录需要：

```text
<domain>_bundle.crt
<domain>.key
```

如果服务器实际证书路径不同，需要同步修改 `deploy/nginx/alipay.conf`。

### 5.6 启动 systemd 服务

```bash
cp /tmp/alipay-release/deploy/systemd/*.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable payment-gateway-api merchant-api mall-api
systemctl start payment-gateway-api
systemctl start merchant-api
systemctl start mall-api
```

## 6. 验证

前端：

```bash
curl -I https://mall.linsy.online
curl -I https://merchant.linsy.online
curl -I https://admin.linsy.online
curl -I https://client.linsy.online
curl -I https://testpay.linsy.online
```

API：

```bash
curl -I https://api.linsy.online/api/mall/catalog/categories
curl -I https://api.linsy.online/api/merchant/products
curl -I https://api.linsy.online/api/gateway/alipay/config
```

服务状态：

```bash
systemctl status payment-gateway-api merchant-api mall-api --no-pager
journalctl -u mall-api -n 100 --no-pager
journalctl -u merchant-api -n 100 --no-pager
journalctl -u payment-gateway-api -n 100 --no-pager
```

未登录接口返回 `401` 是正常现象；需要看响应是否为 JSON，而不是 HTML。

## 7. 支付相关地址

支付宝异步通知地址：

```text
https://api.linsy.online/api/gateway/alipay/notify
```

商城支付结果页：

```text
https://mall.linsy.online/pay-result
```

商家后台自建支付网关配置建议：

```text
gatewayHost=http://127.0.0.1:8090
gatewayReturnUrl=https://mall.linsy.online/pay-result
gatewayBusinessNotifyUrl=https://api.linsy.online/api/merchant/gateway/pay/notify
```

如果页面中还有 `127.0.0.1`、局域网 IP、旧接口路径，需要上线前修正。

## 8. 回滚

前端回滚示例：

```bash
rm -rf /var/www/alipay/mall/*
tar -xzf /var/www/alipay/backups/mall-YYYYMMDD-HHMMSS.tar.gz -C /var/www/alipay/mall
chown -R nginx:nginx /var/www/alipay/mall
```

后端回滚示例：

```bash
cp /opt/alipay/backups/mall-api-YYYYMMDD-HHMMSS.jar /opt/alipay/mall-api.jar
chown alipay:alipay /opt/alipay/mall-api.jar
systemctl restart mall-api
```
