# 支付网关测试端后端

该服务保存测试应用的网关地址、`AppId` 和 `AppSecret`，负责：

- 自动生成业务订单号；
- 接收测试端填写的同步跳转地址，并作为第三方订单参数提交给网关；
- 对支付网关 OpenAPI 请求进行 HMAC-SHA256 签名；
- 代理创建订单和订单查询；
- 接收并验签支付网关的业务通知。

## 环境变量

```bash
TESTPAY_GATEWAY_BASE_URL=http://127.0.0.1:8090
TESTPAY_GATEWAY_APP_ID=your-app-id
TESTPAY_GATEWAY_APP_SECRET=your-app-secret
TESTPAY_RETURN_URL=http://127.0.0.1:5177/pay-result.html
TESTPAY_BUSINESS_NOTIFY_URL=http://127.0.0.1:8092/api/testpay/notify
```

## 启动

```bash
cd gateway-pay-test-api
mvn spring-boot:run
```

默认监听 `127.0.0.1:8092`。
