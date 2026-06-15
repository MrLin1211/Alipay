# Mallhome Pay Frontend

无依赖前端项目，用于调用后端支付下单接口。

## 启动

```bash
cd frontend
npm run dev
```

默认访问：

```text
http://127.0.0.1:5173
```

## 页面功能

- 输入支付金额
- 输入订单标题
- 选择支付类型和支付方式
- 配置后端接口地址，默认 `http://localhost:8080`
- 调用后端 `POST /api/pay-orders`
- 商户订单号由后端生成
- 用户 IP 由后端从当前请求中获取；获取失败时后端使用固定 IP 兜底
- 解析后端返回的 `rawResponse`
- 展示商户订单号、支付链接、平台订单号、唤起方式和原始响应

## 联调顺序

先启动后端：

```bash
sh backend/scripts/run-local.sh
```

再启动前端：

```bash
cd frontend
npm run dev
```

然后打开：

```text
http://127.0.0.1:5173
```
