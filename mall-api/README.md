# Mallhome Pay Backend

Spring Boot 后端项目，用于对接 `https://mallhome.jiaoyie.com/doc/` 文档中的支付接口，并给自己的前端暴露统一下单接口。

## 目录分层

- `config`: 平台地址、商户号、密钥等配置
- `util`: MD5、AES、签名、form body 工具
- `client`: 调用公共平台支付接口
- `service`: 自己的业务组装逻辑
- `controller`: 提供给前端调用的 REST API

## 启动前配置

推荐使用环境变量，不要把真实密钥提交到代码仓库。

```bash
export MALLHOME_PAY_HOST="https://实际平台地址"
export MALLHOME_MD5_KEY="平台提供的md5Key"
export MALLHOME_AES_KEY="平台提供的aesKey"
export MALLHOME_EXTERNAL_ID="平台提供的商户号"
export MALLHOME_NOTIFY_URL="https://你的后端域名/pay/notify"
export MALLHOME_RETURN_URL="https://你的前端域名/pay/success"
```

## 启动

需要 JDK 17+ 和 Maven。

```bash
mvn spring-boot:run
```

本地也可以使用项目根目录的 `.env` 启动：

```bash
sh scripts/run-local.sh
```

如果从仓库根目录启动：

```bash
sh mall-api/scripts/run-local.sh
```

## 前端调用接口

详细文档见：[docs/frontend-api.md](docs/frontend-api.md)

`POST http://localhost:8080/api/pay-orders`

```json
{
  "orderNo": "ORDER202606090001",
  "totalAmount": 0.10,
  "subject": "测试商品",
  "clientIp": "182.144.176.124",
  "typeIndex": 2,
  "goodsType": 1,
  "payMethodType": "ALIPAY_CN",
  "returnUrl": "https://www.example.com/pay/success",
  "attachInfo": "order_ext_001"
}
```

返回值中的 `rawResponse` 是平台原始响应，里面通常包含 `payUrl`、`evokeMode`、`platTradeNo`。

## 支付结果通知

平台支付结果回调地址：

```http
POST /api/pay/notify
Content-Type: application/x-www-form-urlencoded
```

该接口会校验 Header 中的 `timeStamp`、`visitAuth`，以及 Body 中的 `pltNotifySign`。校验通过返回纯文本 `success`，失败返回 `fail`。

## 管理接口

管理后台使用以下接口：

```http
GET /api/admin/pay-orders
GET /api/admin/pay-orders/{orderNo}
GET /api/admin/pay-config
GET /api/admin/pay-notifies
```

订单列表支持参数：

```text
orderNo: 按商户订单号模糊查询
status: 按本地支付状态筛选
page: 页码，从 0 开始
size: 每页条数
```

回调记录支持参数：

```text
orderNo: 按商户订单号模糊查询
verified: true/false，按验签结果筛选
page: 页码，从 0 开始
size: 每页条数
```

## 数据库

本地默认使用 MySQL：

```text
Host: 127.0.0.1
Port: 3306
Database: alipay_pay
Username: alipay_user
Password: alipay123456
```

创建支付订单时会写入 `payment_order` 表；收到支付结果通知并验签通过后，会按 `merchantTradeNo` 更新对应订单状态。

用 MySQL 客户端查看：

```bash
mysql -h 127.0.0.1 -P 3306 -u alipay_user -p alipay_pay
```

```sql
SELECT * FROM payment_order;
```
