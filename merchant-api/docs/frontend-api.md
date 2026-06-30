# 前端调用接口文档

本文档描述前端如何调用本后端的支付下单接口。后端会负责对接公共平台支付接口，前端不需要处理 `md5Key`、`aesKey`、`visitAuth`、`sign` 等敏感加密逻辑。

## 基础信息

- 本地后端地址：`http://localhost:8080`
- 请求格式：`application/json`
- 返回格式：`application/json`

## 获取当前用户 IP

该接口只用于调试查看后端识别到的 IP。正式创建支付订单时，前端不需要传用户 IP，后端会直接从当前请求中解析。

```http
GET /api/client-ip
```

返回示例：

```json
{
  "ip": "182.144.176.124"
}
```

后端会优先读取代理头：

- `X-Forwarded-For`
- `X-Real-IP`
- `Proxy-Client-IP`
- `WL-Proxy-Client-IP`

本地开发或代理环境无法识别真实公网 IP 时，后端会使用固定兜底值 `182.144.176.124`，避免向平台提交 `127.0.0.1` 或 `::1`。

## 创建支付订单

### 接口地址

```http
POST /api/pay-orders
```

完整本地地址：

```http
POST http://localhost:8080/api/pay-orders
```

### 请求参数

| 字段 | 类型 | 必填 | 说明 | 示例 |
| --- | --- | --- | --- | --- |
| `totalAmount` | number | 是 | 支付金额，单位元，最小 `0.01`，最多 2 位小数，整数部分最多 10 位 | `0.10` |
| `subject` | string | 是 | 订单标题，最多 128 个字符 | `测试商品` |
| `typeIndex` | number | 否 | 支付类型，只允许 `1`-`5`：`1` 手机网站，`2` 电脑网站，`3` APP，`4` 小程序，`5` 公众号。默认 `2` | `2` |
| `goodsType` | number | 否 | 订单类型，只允许 `1`-`9`，默认 `1` | `1` |
| `payMethodType` | string | 否 | 支付方式，只允许 `ALIPAY_CN`、`ALIPAY`、`WECHATPAY`、`CARD`。默认取后端配置 | `ALIPAY_CN` |
| `attachInfo` | string | 否 | 附加信息，最多 256 个字符，支付回调时原样返回 | `order_ext_001` |
| `quitUrl` | string | 否 | 合法 URL，最多 512 个字符。用户付款中途退出返回地址，`typeIndex=1` 时建议传 | `https://example.com/pay/cancel` |
| `returnUrl` | string | 否 | 合法 URL，最多 512 个字符。支付成功后的前端跳转地址，不传则使用后端配置 | `https://example.com/pay/success` |
| `subExternalId` | string | 否 | 子商家编号，最多 64 个字符，商户进件后才需要传 | `800000002` |

### 请求示例

```bash
curl -X POST 'http://localhost:8080/api/pay-orders' \
  -H 'Content-Type: application/json' \
  -d '{
    "totalAmount": 0.10,
    "subject": "测试商品",
    "typeIndex": 2,
    "goodsType": 1,
    "payMethodType": "ALIPAY_CN",
    "attachInfo": "order_ext_001",
    "returnUrl": "https://example.com/pay/success"
  }'
```

### JavaScript 调用示例

```js
async function createPayOrder() {
  const response = await fetch("http://localhost:8080/api/pay-orders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      totalAmount: 0.10,
      subject: "测试商品",
      typeIndex: 2,
      goodsType: 1,
      payMethodType: "ALIPAY_CN",
      attachInfo: "order_ext_001",
      returnUrl: "https://example.com/pay/success"
    })
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
}
```

### 成功返回

后端会先保存本地订单，再调用平台支付接口。成功后返回本地订单信息和平台原始响应。
其中 `orderNo` 是后端生成的商户订单号，后续查询订单或排查回调时使用这个值。

```json
{
  "orderId": 1,
  "orderNo": "ORDER202606090001",
  "status": "CREATE_SUCCESS",
  "payUrl": "https://qr.alipay.com/...",
  "platTradeNo": "2406068116172875052217",
  "rawResponse": "{\"code\":0,\"msg\":\"success\",\"data\":{\"data\":{\"payUrl\":\"https://qr.alipay.com/...\",\"evokeMode\":\"0\",\"platTradeNo\":\"2406068116172875052217\"}}}"
}
```

平台原始响应中常用字段：

| 字段 | 说明 |
| --- | --- |
| `code` | `0` 表示成功，非 `0` 表示失败 |
| `msg` | 平台返回信息 |
| `data.data.payUrl` | 支付跳转链接或二维码内容 |
| `data.data.evokeMode` | 唤起方式：`0` 跳转链接，`1` 表单提交，`2` 二维码 |
| `data.data.platTradeNo` | 平台订单号 |

### 前端处理建议

收到返回后，前端可先解析 `rawResponse`：

```js
const result = await createPayOrder();
const platformResult = JSON.parse(result.rawResponse);

if (platformResult.code !== 0) {
  throw new Error(platformResult.msg || "创建支付订单失败");
}

const payData = platformResult.data?.data;
const payUrl = result.payUrl || payData?.payUrl;

if (payData?.evokeMode === "0" && payUrl) {
  window.location.href = payUrl;
}
```

## 查询本地订单

创建支付订单后，可通过商户订单号查询本地库中的支付状态。

```http
GET /api/pay-orders/{orderNo}
```

示例：

```http
GET http://localhost:8080/api/pay-orders/ORDER202606090001
```

返回示例：

```json
{
  "id": 1,
  "orderNo": "ORDER202606090001",
  "externalId": "888888",
  "totalAmount": 0.10,
  "subject": "测试商品",
  "clientIp": "182.144.176.124",
  "status": "SUCCESS",
  "tradeStatus": "TRADE_SUCCESS",
  "platTradeNo": "2406068116172875052217",
  "thirdOutTradeNo": "202606092200...",
  "payUrl": "https://qr.alipay.com/...",
  "evokeMode": "0",
  "paidAt": "2026-06-09T18:10:00",
  "createdAt": "2026-06-09T18:08:00",
  "updatedAt": "2026-06-09T18:10:01"
}
```

## 本地订单状态

| 状态 | 说明 |
| --- | --- |
| `CREATED` | 后端已保存订单，准备调用平台 |
| `CREATE_SUCCESS` | 平台支付订单创建成功 |
| `CREATE_FAILED` | 平台支付订单创建失败 |
| `SUCCESS` | 交易成功，平台状态为 `TRADE_SUCCESS` |
| `FINISHED` | 交易结束，属于失败类状态，平台状态为 `TRADE_FINISHED` |
| `CLOSED` | 收到交易关闭通知，平台状态为 `TRADE_CLOSED` |
| `UNKNOWN_NOTIFY` | 收到其他未知交易状态 |

## 错误返回

### 参数校验失败

```json
{
  "code": 400,
  "message": "subject 不能为空"
}
```

### 后端或平台调用异常

```json
{
  "code": 500,
  "message": "AES key bytes length must be 16, 24 or 32"
}
```

常见原因：

- `aesKey` 长度不正确，应为 16、24 或 32 字节
- `md5Key`、`aesKey`、`externalId` 未配置
- 平台接口地址 `MALLHOME_PAY_HOST` 配置错误
- 平台返回权限不匹配，通常是签名或密钥不正确
- 平台返回权限过期，通常是服务器时间戳异常

## 本地启动

后端启动：

```bash
sh scripts/run-local.sh
```

启动后前端即可调用：

```http
http://localhost:8080/api/pay-orders
```
