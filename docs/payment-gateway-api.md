# 支付网关 OpenAPI 调用文档

本文档面向所有接入支付网关的业务系统。当前商城端也按第三方应用接入支付网关，不直接维护上游支付平台密钥。

## 一、接入边界

支付网关只负责接入应用、支付订单、退款、平台通知、业务通知、通道配置等支付相关能力。

支付网关不负责商城商品、用户、收货地址、商城订单等业务后台功能。

## 二、环境地址

本地支付网关：

```text
http://127.0.0.1:8090
```

生产支付网关：

```text
https://pay.zhenbaoge.com
```

上游直连支付平台：

```text
https://home.zhenbaoge.com
```

## 三、接入应用

每个业务系统需要在支付网关后台创建接入应用，获取：

```text
AppId
AppSecret
```

业务系统每次调用 OpenAPI 时使用 `AppSecret` 做 HMAC 签名。

## 四、OpenAPI 鉴权

请求 Header：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-Gateway-App-Id` | 是 | 接入应用 AppId |
| `X-Gateway-Timestamp` | 是 | 请求时间戳，建议 ISO 字符串 |
| `X-Gateway-Nonce` | 是 | 随机字符串，建议 UUID |
| `X-Gateway-Signature` | 是 | HMAC-SHA256 Base64 签名 |
| `Content-Type` | 是 | `application/json` |

签名原文：

```text
HTTP_METHOD + "\n" +
REQUEST_URI + "\n" +
X-Gateway-Timestamp + "\n" +
X-Gateway-Nonce + "\n" +
RAW_BODY
```

签名算法：

```text
Base64(HmacSHA256(AppSecret, 签名原文))
```

注意：`RAW_BODY` 必须和实际 HTTP 请求体完全一致。

## 五、创建支付订单

接口：

```http
POST /api/gateway/pay/orders
```

兼容旧接口：

```http
POST /api/gateway/pay/alipay/wap
```

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `merchantOrderNo` | string | 是 | 业务系统订单号，同一 AppId 下唯一 |
| `subject` | string | 是 | 订单标题 |
| `totalAmount` | number | 是 | 金额，最低 0.01，最多两位小数 |
| `typeIndex` | number | 否 | 支付类型，默认 1；1=手机网站，2=电脑网站，3=APP，4=小程序，5=公众号 |
| `goodsType` | number | 否 | 订单类型，默认 1 |
| `payMethodType` | string | 否 | 支付方式，默认 `ALIPAY_CN` |
| `clientIp` | string | 否 | 用户真实 IP；不传时网关从请求头获取，失败时使用兜底 IP |
| `returnUrl` | string | 否 | 本次支付完成后的第三方同步跳转地址，必须为 HTTP/HTTPS；网关通道不提供默认值 |
| `quitUrl` | string | 条件必填 | 用户中途退出地址，`typeIndex=1`（手机网站）时必填 |
| `businessNotifyUrl` | string | 否 | 业务系统支付结果通知地址；接入应用配置为空时兜底使用 |
| `attachInfo` | string | 否 | 附加信息，平台通知时原样返回 |
| `subExternalId` | string | 否 | 子商家编号 |
| `isShort` | number | 否 | 是否生成短地址：1=是，0=否 |
| `isQr` | number | 否 | 是否使用当面付二维码：1=是，0=否 |

请求示例：

```json
{
  "merchantOrderNo": "M202608230001",
  "subject": "测试商品",
  "totalAmount": 0.10,
  "typeIndex": 1,
  "goodsType": 1,
  "payMethodType": "ALIPAY_CN",
  "returnUrl": "https://mall.example.com/pay-result",
  "businessNotifyUrl": "https://mall.example.com/api/pay/notify",
  "attachInfo": "mall-order-10001"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "gatewayOrderNo": "GW202608231030001234ABCD",
    "merchantOrderNo": "M202608230001",
    "payUrl": "https://qr.alipay.com/...",
    "evokeMode": "0",
    "platformTradeNo": "TZ2026...",
    "payForm": "<!doctype html>..."
  }
}
```

支付载荷处理规则：

| 支付类型 | `evokeMode` | 调用方式 |
| --- | --- | --- |
| 手机网站、电脑网站 | `0` | `payUrl` 是网页支付地址，使用浏览器跳转 |
| 表单支付 | `1` | 使用 `payForm` 中的平台表单内容 |
| 二维码支付 | `2` | `payUrl` 是二维码内容，由接入方生成二维码展示 |
| APP 支付 | `3` | `payUrl` 实际是支付宝 APP SDK 的 `orderInfo`，必须交给原生支付宝 SDK，不能使用浏览器或 WebView 打开 |

APP 支付说明：第三方服务端创建订单后，把返回的 APP 支付参数交给自己的 iOS/Android 客户端；客户端调用支付宝 SDK。SDK 回调只代表客户端调用结果，最终支付状态仍以网关异步通知或订单查询结果为准。APP 支付不会依赖网页 `returnUrl` 完成客户端回跳。

## 六、查询支付订单

接口：

```http
POST /api/gateway/pay/orders/query
```

请求体：

```json
{
  "gatewayOrderNo": "GW202608231030001234ABCD"
}
```

或：

```json
{
  "merchantOrderNo": "M202608230001"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "order": {},
    "platformResponse": {}
  }
}
```

## 七、申请退款

接口：

```http
POST /api/gateway/pay/refunds
```

请求体：

```json
{
  "gatewayOrderNo": "GW202608231030001234ABCD",
  "refundAmount": 0.10,
  "refundReason": "用户退款"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "refundOrderNo": "RF202608231100001234ABCD",
    "status": "SUCCESS",
    "platformResponse": {}
  }
}
```

## 八、业务支付结果通知

支付网关收到上游平台支付通知并验签通过后，会通知业务系统。

业务通知地址优先级：

```text
接入应用 notifyUrl 优先
下单请求 businessNotifyUrl 兜底
```

通知方式：

```http
POST {业务通知地址}
Content-Type: application/json
```

通知 Header 与 OpenAPI 一致：

```text
X-Gateway-App-Id
X-Gateway-Timestamp
X-Gateway-Nonce
X-Gateway-Signature
```

通知 JSON：

```json
{
  "merchantOrderNo": "M202608230001",
  "gatewayOrderNo": "GW202608231030001234ABCD",
  "appId": "mall",
  "channel": "ZHENBAOGE",
  "status": "SUCCESS",
  "tradeStatus": "TRADE_SUCCESS",
  "platformTradeNo": "TZ2026...",
  "thirdTradeNo": "202608232200...",
  "totalAmount": 0.10,
  "paidAt": "2026-08-23 10:35:00",
  "notifyPayload": "{...上游平台原始通知...}"
}
```

业务系统处理成功后必须返回纯文本：

```text
success
```

## 九、状态枚举

网关订单状态：

| 值 | 说明 |
| --- | --- |
| `CREATED` | 已创建 |
| `PAYING` | 待支付 |
| `SUCCESS` | 交易成功 |
| `REFUNDED` | 已退款 |
| `FINISHED` | 交易结束，按失败类状态处理 |
| `CLOSED` | 交易关闭 |
| `FAILED` | 失败 |
| `UNKNOWN` | 未知 |

上游交易状态：

| 值 | 说明 |
| --- | --- |
| `WAIT_BUYER_PAY` | 待支付 |
| `TRADE_SUCCESS` | 支付成功 |
| `TRADE_FINISHED` | 交易结束 |
| `TRADE_CLOSED` | 交易关闭 |
| `TRADE_REFUND_SUCCESS` | 退款成功 |

## 十、上游平台配置

支付网关后台通道配置需要维护：

```text
host
externalId
md5Key
aesKey
notifyUrl
```

本地开发可复制：

```text
payment-gateway-api/.env.example -> payment-gateway-api/.env
```

再填写真实密钥。`.env` 已在 `.gitignore` 中，不应提交到仓库。
