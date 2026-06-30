# 生产环境部署说明

本文档按以下域名规划部署：

| 服务 | 域名 | 本机端口 |
| --- | --- | --- |
| 商城 | `https://mall.linsy.online` | 静态文件 |
| 商家后台 | `https://merchant.linsy.online` | 静态文件 |
| 管理后台 | `https://admin.linsy.online` | 静态文件 |
| 商城 API | `https://api.linsy.online/api/mall/**` | `127.0.0.1:8080` |
| 商家 API | `https://api.linsy.online/api/merchant/**` | `127.0.0.1:8081` |
| 支付网关 API | `https://api.linsy.online/api/gateway/**` | `127.0.0.1:8090` |

同时为了减少跨域配置，Nginx 也会在各自前端域名下反向代理对应 API：

- `mall.linsy.online/api/mall/**` -> `mall-api`
- `merchant.linsy.online/api/merchant/**` -> `merchant-api`
- `admin.linsy.online/api/gateway/**` -> `payment-gateway-api`

## 1. 服务器准备

推荐系统：Ubuntu 22.04 / Debian 12 / CentOS 8+。

需要安装：

```bash
java -version
mvn -version
node -v
npm -v
nginx -v
mysql --version
```

Java 需要 17。

## 2. 数据库

创建统一数据库和账号：

```sql
CREATE DATABASE alipay_pay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'alipay_user'@'127.0.0.1' IDENTIFIED BY 'CHANGE_ME_DB_PASSWORD';
GRANT ALL PRIVILEGES ON alipay_pay.* TO 'alipay_user'@'127.0.0.1';
FLUSH PRIVILEGES;
```

当前三个后端启动时会执行现有初始化/迁移逻辑。第一次上线建议先启动 `payment-gateway-api`，再启动 `mall-api` 和 `merchant-api`。

## 3. 本地构建产物

在项目根目录执行：

```bash
sh scripts/build-production.sh
```

产物会生成在：

```text
dist-production/
├── backend/
│   ├── mall-api.jar
│   ├── merchant-api.jar
│   └── payment-gateway-api.jar
├── frontend/
│   ├── mall/
│   ├── merchant/
│   └── admin/
└── deploy/
```

## 4. 上传到服务器

示例：

```bash
scp -r dist-production/* root@119.45.58.43:/tmp/alipay-release/
```

服务器上执行：

```bash
useradd -r -s /usr/sbin/nologin alipay || true
mkdir -p /opt/alipay /etc/alipay /var/www/alipay/mall /var/www/alipay/merchant /var/www/alipay/admin /var/www/alipay/uploads

cp /tmp/alipay-release/backend/*.jar /opt/alipay/
cp -R /tmp/alipay-release/frontend/mall/. /var/www/alipay/mall/
cp -R /tmp/alipay-release/frontend/merchant/. /var/www/alipay/merchant/
cp -R /tmp/alipay-release/frontend/admin/. /var/www/alipay/admin/

chown -R alipay:alipay /opt/alipay /var/www/alipay
```

## 5. 环境变量

复制模板：

```bash
mkdir -p /etc/alipay
cp /tmp/alipay-release/deploy/production.env.example /etc/alipay/alipay.env
chmod 600 /etc/alipay/alipay.env
```

编辑：

```bash
vim /etc/alipay/alipay.env
```

必须替换所有 `CHANGE_ME_*`。

## 6. systemd

```bash
cp /tmp/alipay-release/deploy/systemd/*.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable payment-gateway-api merchant-api mall-api
systemctl start payment-gateway-api
systemctl start mall-api merchant-api
```

查看状态：

```bash
systemctl status payment-gateway-api mall-api merchant-api --no-pager
journalctl -u payment-gateway-api -f
```

## 7. Nginx 和证书

把证书放到这些目录：

```text
/etc/nginx/ssl/mall.linsy.online/
/etc/nginx/ssl/merchant.linsy.online/
/etc/nginx/ssl/admin.linsy.online/
/etc/nginx/ssl/api.linsy.online/
```

每个目录需要：

```text
<domain>_bundle.crt
<domain>.key
```

安装配置：

```bash
cp /tmp/alipay-release/deploy/nginx/alipay.conf /etc/nginx/conf.d/alipay.conf
nginx -t
systemctl reload nginx
```

## 8. 防火墙

公网只开放：

```bash
ufw allow 80/tcp
ufw allow 443/tcp
```

后端端口 `8080/8081/8090` 只监听 `127.0.0.1`，不需要对公网开放。

## 9. 验证

```bash
curl -I https://mall.linsy.online
curl -I https://merchant.linsy.online
curl -I https://admin.linsy.online
curl -I https://api.linsy.online/api/mall/catalog/categories
```

登录管理后台后检查：

- 管理后台商家列表
- 商品分类列表
- 商家后台商品列表
- 商城商品列表
- 商城下单和支付创建

## 10. 支付宝回调

支付宝异步通知地址建议配置为：

```text
https://api.linsy.online/api/gateway/alipay/notify
```

业务支付成功回跳地址：

```text
https://mall.linsy.online/pay-result
```

如果当前支付宝配置页面里有旧的 `127.0.0.1` 或局域网地址，上线前需要改成线上域名。
