# 多端订单与支付架构设计

更新时间：2026-07-21

## 1. 背景

商城后续将同时支持以下客户端：

- `mall.linsy.online`：现有 H5 商城。
- `app.linsy.online`：`mall-miniapp` 编译生成的 H5 商城。
- 微信小程序。
- 支付宝小程序。
- iOS 和 Android App。
- PC Web 或跨设备扫码支付场景。

用户可能在一个客户端创建商品订单，在另一个客户端继续支付。因此，支付返回地址不能只按商品订单来源固定，也不能简单地给每个前端配置一个全局 `returnUrl`。

## 2. 核心结论

商品订单属于用户和商城业务，允许跨客户端共享；支付单属于一次具体支付尝试和支付场景，不保证能够跨客户端复用。

`returnUrl` 只属于某一次支付尝试，不属于商品订单本身。

推荐的数据关系：

```text
商品订单 ProductOrder
  └─ 支付意图 PaymentIntent
       ├─ 支付尝试 PaymentAttempt：支付宝 H5
       ├─ 支付尝试 PaymentAttempt：微信小程序
       └─ 支付尝试 PaymentAttempt：App SDK
```

同一个商品订单可以存在多次支付尝试，但最终只能成功支付一次。

## 3. 需要区分的客户端信息

每次支付尝试至少记录以下三个维度：

- `orderOrigin`：商品订单最初在哪个客户端创建。
- `paymentInitiator`：用户在哪个客户端点击发起支付。
- `paymentScene`：最终使用的支付产品和唤起方式。

示例：

```json
{
  "orderOrigin": "WECHAT_MINIAPP",
  "paymentInitiator": "H5",
  "paymentScene": "ALIPAY_WAP"
}
```

订单来源是微信小程序，不妨碍用户后来在 H5 使用支付宝完成支付。

如果未来允许他人代付，还需要区分：

- 商品订单的 `buyerUserId`。
- 发起支付操作的商城账号。
- 支付渠道回传的实际付款人标识，例如微信 `openid` 或支付宝买家标识。

## 4. 不同终端的支付完成方式

| 支付环境 | 支付完成后的客户端处理 |
| --- | --- |
| 普通 H5 | 支付平台跳转 HTTPS `returnUrl` |
| PC Web | 返回结果页，或原页面持续轮询订单状态 |
| 微信小程序 | `wx.requestPayment` 返回后进入小程序订单页 |
| 支付宝小程序 | 小程序支付 API 返回后进入订单页 |
| iOS/Android App | 支付 SDK 回调、Universal Link 或应用深链 |
| 跨设备扫码 | 原设备轮询服务端状态，付款设备只展示付款结果 |

小程序和 App 不应依赖传统网页 `returnUrl` 完成业务闭环。

支付是否成功必须以服务端收到并验证通过的异步通知为准。前端跳转、SDK 成功回调或 `requestPayment` 返回结果只能用于改善交互，不能作为最终入账依据。

## 5. returnUrl 设计

网页支付可以为每次支付尝试保存不同的返回地址：

- `mall` 发起的支付宝 H5 支付：`https://mall.linsy.online/pay-result`
- `mall-miniapp` H5 发起的支付宝支付：`https://app.linsy.online/#/pages/orders/detail?orderNo=商品订单号`

商家支付配置中的返回地址只作为没有传入支付场景时的默认兜底值。

不建议客户端直接提交任意完整 URL。客户端应提交受控的 `returnTarget` 枚举，由后端根据受信任域名映射成实际 URL，避免开放重定向风险。

建议的受控目标示例：

- `MALL_PAY_RESULT`
- `APP_H5_ORDER_DETAIL`
- `PC_ORDER_RESULT`
- `APP_UNIVERSAL_LINK`

后端只允许跳转至经过配置的可信域名，例如：

- `mall.linsy.online`
- `app.linsy.online`

## 6. 建议的统一支付接口

客户端针对已有商品订单创建支付尝试：

```http
POST /api/mall/product-orders/{orderNo}/payment-attempts
```

请求示例：

```json
{
  "clientPlatform": "H5",
  "paymentScene": "ALIPAY_WAP",
  "returnTarget": "APP_H5_ORDER_DETAIL"
}
```

后端负责：

1. 校验商品订单归属、状态、金额和商家。
2. 判断是否已有与当前客户端兼容的有效支付尝试。
3. 兼容时返回已有支付动作。
4. 不兼容、失败或过期时创建新的支付尝试。
5. 根据 `returnTarget` 生成可信返回地址。
6. 返回统一的客户端执行动作。

建议响应结构：

```json
{
  "paymentAttemptNo": "PAY202607210001",
  "status": "CREATE_SUCCESS",
  "actionType": "REDIRECT_URL",
  "actionData": {
    "url": "https://api.linsy.online/api/mall/pay-orders/PAY202607210001/pay-page"
  }
}
```

`actionType` 可以包括：

- `REDIRECT_URL`：支付宝 H5 等网页跳转。
- `HTML_FORM`：需要浏览器提交的支付表单。
- `WECHAT_MINIAPP_PARAMS`：微信小程序支付参数。
- `ALIPAY_MINIAPP_PARAMS`：支付宝小程序支付参数。
- `APP_SDK_PARAMS`：原生 App 支付 SDK 参数。
- `QR_CODE`：PC 或跨设备扫码支付。
- `NONE`：订单已支付或当前不需要客户端动作。

前端只负责执行后端返回的支付动作，不自行拼装渠道参数。

## 7. 跨客户端复用规则

客户端发起支付时按以下顺序处理：

1. 查询商品订单是否已经支付。
2. 查询已有支付尝试是否仍在有效期内。
3. 判断已有支付尝试的支付场景与当前客户端是否兼容。
4. 判断支付动作和返回目标是否适用于当前客户端。
5. 找到兼容尝试时继续使用；没有时创建新的支付尝试。

不能再简单地取商品订单的最新一张支付单直接使用。

兼容性示例：

- `ALIPAY_WAP` 可以在支持网页跳转的手机浏览器中使用。
- `WECHAT_MINIAPP` 参数只能在对应微信小程序中使用。
- `APP_SDK` 参数不能拿到 H5 中使用。
- PC 二维码允许手机扫码付款，但 PC 页面必须自行轮询服务端状态。
- 返回地址属于 `mall.linsy.online` 的历史支付单，不应在 `app.linsy.online` 中直接复用。

## 8. 支付尝试建议字段

支付尝试建议至少包含：

- `paymentAttemptNo`
- `productOrderId`
- `merchantNo`
- `buyerUserId`
- `totalAmount`
- `orderOrigin`
- `paymentInitiator`
- `clientPlatform`
- `paymentScene`
- `paymentChannel`
- `returnTarget`
- `resolvedReturnUrl`
- `actionType`
- `status`
- `expiresAt`
- `createdAt`
- `paidAt`
- `supersededBy`
- 支付平台交易号和付款人渠道标识

支付尝试状态可包括：

- `CREATED`
- `CREATE_SUCCESS`
- `CREATE_FAILED`
- `PAYING`
- `SUCCESS`
- `CLOSED`
- `EXPIRED`
- `SUPERSEDED`

## 9. 并发、幂等和重复支付

一个商品订单可以有多张支付尝试，但必须保证只能成功一次：

1. 创建支付尝试前重新检查商品订单状态。
2. 支付回调按渠道通知编号或平台交易号实现幂等。
3. 商品订单从 `PENDING` 更新到 `PAID` 时使用数据库锁或条件更新。
4. 第一个合法成功回调完成订单支付状态更新、库存确认和记账。
5. 重复通知不得重复扣库存、重复记账或重复发放权益。
6. 其他未完成支付尝试进入 `CLOSED`、`EXPIRED` 或 `SUPERSEDED`。
7. 如果出现多渠道同时成功的极端情况，需要自动进入异常退款和人工审计流程。
8. 退款必须绑定实际成功的支付尝试，不能只绑定商品订单。

## 10. 当前系统的演进方向

当前系统已经将商品订单和支付订单分开保存，但客户端仍存在“取最新支付单并直接复用”的逻辑。该方式不能覆盖完整的多端支付场景。

后续改造原则：

```text
查询当前客户端兼容的有效支付尝试
    ├─ 找到：继续执行该支付尝试
    └─ 未找到：为同一商品订单创建新的支付尝试
```

建议分阶段实施：

### 第一阶段：统一 H5 支付

- 为 `mall` 和 `mall-miniapp H5` 增加客户端来源和 `returnTarget`。
- 不再按“最新支付单”盲目复用。
- 后端增加可信返回目标映射。
- 支付成功后统一查询商品订单状态。

### 第二阶段：微信小程序支付

- 增加 `WECHAT_MINIAPP` 支付场景。
- 后端返回 `WECHAT_MINIAPP_PARAMS`。
- 小程序调用 `wx.requestPayment`。
- 微信回调幂等更新支付尝试和商品订单。

### 第三阶段：支付宝小程序和 App

- 增加支付宝小程序支付动作。
- 增加微信和支付宝 App SDK 动作。
- 增加 Universal Link、应用深链和支付恢复机制。

### 第四阶段：跨设备和异常治理

- 增加 PC 二维码支付和轮询。
- 增加支付尝试过期、关闭和替换机制。
- 增加重复支付自动退款、对账和人工审计。

## 11. 已确定原则

- 商品订单跨端共享。
- 支付尝试按客户端能力和支付场景隔离。
- `returnUrl` 属于支付尝试，不属于商品订单。
- 服务端异步通知是支付成功的最终依据。
- 客户端不能提交任意跳转地址，只提交受控目标。
- 不兼容的历史支付尝试不能跨端直接复用。
- 同一商品订单允许多次尝试，但只允许一次业务支付成功。

## 12. 待后续实现

- 数据库支付尝试模型及迁移方案。
- 统一支付尝试接口。
- 支付场景兼容性判断服务。
- 可信 `returnTarget` 映射配置。
- `mall` 和 `mall-miniapp H5` 对统一接口的迁移。
- 微信小程序支付参数、回调和订单同步。
- App SDK、深链恢复和跨设备支付。
- 重复支付自动退款与对账机制。

