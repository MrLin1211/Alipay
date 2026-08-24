# 接入方支付后台

接入方支付后台给调用支付网关 OpenAPI 的第三方接入方使用，只能查看和维护自己 AppId 下的数据。

## 功能

- 支付订单列表、详情、发起退款
- 退款记录列表、详情
- 通知记录列表、详情
- 接入配置：查看 AppId，维护业务通知地址、IP 白名单，重置 AppSecret
- OpenAPI 文档：查看签名规则和接口示例

## 本地启动

```bash
cd gateway-client-admin
npm install
npm run dev
```

默认访问地址：

```text
http://127.0.0.1:5176
```

默认连接支付网关：

```text
http://127.0.0.1:8090
```

如需指定支付网关地址：

```bash
VITE_API_BASE_URL=http://127.0.0.1:8090 npm run dev
```

## 默认账号

本地默认接入方账号由 `payment-gateway-api` 初始化，默认值可通过环境变量覆盖：

```text
GATEWAY_CLIENT_USERNAME=client
GATEWAY_CLIENT_PASSWORD=client123456
GATEWAY_CLIENT_NAME=演示接入方
```

新增接入应用时，支付网关后台会同步生成对应的接入方后台账号和密码。
