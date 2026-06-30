# 支付结果通知接口

本文档描述公共平台支付成功后，如何回调本后端。

## 接口地址

```http
POST /api/pay/notify
```

完整示例：

```http
POST https://你的后端域名/api/pay/notify
```

对应环境变量建议配置为：

```bash
MALLHOME_NOTIFY_URL=https://你的后端域名/api/pay/notify
```

## 请求头

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `timeStamp` | 是 | 平台发送通知时的秒级时间戳 |
| `visitAuth` | 是 | 平台按文档规则生成的认证字符串 |
| `Content-Type` | 是 | `application/x-www-form-urlencoded` |

## 请求体

平台会以表单格式发送通知参数。

| 字段 | 说明 |
| --- | --- |
| `externalId` | 商户号 |
| `merchantTradeNo` | 商户订单号 |
| `notifyType` | 通知类型，例如 `TRADE_NOTIFY` |
| `platformOutTradeNo` | 平台订单号 |
| `thirdOutTradeNo` | 第三方支付交易号 |
| `totalAmount` | 实际支付金额 |
| `tradeStatus` | 交易状态，通常支付成功为 `TRADE_SUCCESS` |
| `buyerUserId` | 买家 ID，可能为空 |
| `buyerInfo` | 买家信息，可能为空 |
| `attachInfo` | 下单时传入的附加信息 |
| `createTime` | 订单创建时间 |
| `sucTime` | 支付成功时间 |
| `pltNotifySign` | 平台通知签名 |

## 处理逻辑

后端接收到通知后会依次校验：

1. Header 是否包含 `timeStamp` 和 `visitAuth`
2. `visitAuth` 是否正确
3. Body 中的 `pltNotifySign` 是否正确
4. `externalId` 是否等于当前配置的商户号

全部通过后返回：

```text
success
```

失败时返回：

```text
fail
```

注意：平台文档说明，只有返回纯文本 `success` 才认为通知处理成功。返回其他内容时，平台会按重试策略继续通知。

## 订单状态更新位置

通知校验通过后，后端会根据 `merchantTradeNo` 查询本地订单，并更新：

- `status`
- `tradeStatus`
- `platformOutTradeNo`
- `thirdOutTradeNo`
- `sucTime`
- `notifyPayload`

状态映射：

| 平台 `tradeStatus` | 本地 `status` |
| --- | --- |
| `TRADE_SUCCESS` | `SUCCESS` |
| `TRADE_FINISHED` | `FINISHED`（交易结束，失败类状态） |
| `TRADE_CLOSED` | `CLOSED` |
| 其他 | `UNKNOWN_NOTIFY` |
