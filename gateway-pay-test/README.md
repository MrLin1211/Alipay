# 支付网关测试端

该项目用于联调支付网关 OpenAPI，模拟第三方接入应用发起支付和查询订单。

## 重要边界

浏览器只调用 `gateway-pay-test-api`。网关地址、`AppId` 和 `AppSecret` 保存在测试端后端环境变量中，不会打包进前端 JavaScript。

## 本地启动

```bash
cd gateway-pay-test
npm install
npm run dev
```

默认访问：

```text
http://127.0.0.1:5177
```

本地开发时还需要启动 `gateway-pay-test-api`；生产环境由 Nginx 将 `/api/testpay/**` 转发到该服务。

支付完成或中途退出后会进入独立的 `/pay-result.html`。测试端后端会把自动生成的业务订单号追加到本次跳转地址，结果页据此查询订单并展示支付成功、处理中或失败。

选择 APP 支付时，珍宝阁返回的是支付宝原生 SDK 支付参数，不是网页地址。测试端只提供复制功能，真实 iOS/Android 应用必须把该参数交给支付宝 SDK 调用。

## 测试流程

1. 确认支付网关服务已启动。
2. 在支付网关后台创建或确认一个启用状态的接入应用。
3. 在测试端填写订单标题、金额、同步跳转地址；手机网站支付还需填写中途退出地址，业务订单号由测试端后端自动生成。
4. 点击“创建支付订单”。
5. 网页支付点击“打开支付页”；APP 支付点击“复制 APP 支付参数”并交给原生支付宝 SDK。
6. 支付完成后进入独立结果页；也可以返回测试端用“查询订单”检查网关订单状态。

## 域名建议

不要挂到 `mall.linsy.online`。该域名应保留给商城前台。

如果后续要部署测试端，建议使用单独域名：

```text
testpay.linsy.online
```

或：

```text
sandbox-pay.linsy.online
```

OpenAPI 接口仍然使用：

```text
https://api.linsy.online
```
