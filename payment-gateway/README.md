# Payment Gateway

支付网关服务，负责隔离支付宝协议、签名验签、支付流水、回调记录和内部应用鉴权。

## 本地启动

```bash
cd payment-gateway
sh scripts/run-local.sh
```

默认端口：

```text
http://127.0.0.1:8090
```

## 关键环境变量

```bash
GATEWAY_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/payment_gateway?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
GATEWAY_DATASOURCE_USERNAME=alipay_user
GATEWAY_DATASOURCE_PASSWORD=数据库密码

GATEWAY_ADMIN_USERNAME=admin
GATEWAY_ADMIN_PASSWORD=请改成强密码
```

## 第一阶段接口

- `POST /api/gateway/admin/auth/login` 管理员登录
- `GET /api/gateway/admin/alipay/config` 查看支付宝渠道配置
- `PUT /api/gateway/admin/alipay/config` 保存支付宝渠道配置
- `GET /api/gateway/admin/apps` 查看内部应用
- `POST /api/gateway/admin/apps` 创建内部应用
- `POST /api/gateway/pay/alipay/wap` 内部系统发起支付宝手机网站支付
- `POST /api/gateway/notify/alipay` 支付宝异步通知

内部支付接口使用 `appId + appSecret` 做 HMAC 鉴权。
