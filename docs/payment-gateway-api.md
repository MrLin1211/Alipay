# 自建支付网关接口调用文档

本文档说明业务系统如何调用自建支付网关创建支付宝 WAP 支付订单，以及如何接收支付网关回调通知。

## 一、接入准备

### 1. 网关地址

本地开发环境：

```text
http://127.0.0.1:8090
```

生产环境需要替换为实际支付网关域名：

```text
https://pay-gateway.example.com
```

### 2. 接入应用

在支付网关管理后台创建接入应用：

```text
支付网关管理后台 -> 接入应用 -> 新增应用
```

创建后会得到：

```text
AppId
AppSecret
```

业务系统调用支付网关接口时必须使用这两个值做接口鉴权。

### 3. 业务通知地址优先级

支付结果通知地址按以下规则确定：

```text
接入应用 notifyUrl 优先
请求参数 businessNotifyUrl 兜底
```

也就是说，如果接入应用里已经配置了业务通知地址，本次下单请求里的 `businessNotifyUrl` 不会覆盖它。

## 二、接口鉴权

业务系统调用支付网关接口时，需要在 HTTP Header 中带上以下参数：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-Gateway-App-Id` | 是 | 接入应用 AppId |
| `X-Gateway-Timestamp` | 是 | 请求时间戳，建议使用 ISO 时间字符串 |
| `X-Gateway-Nonce` | 是 | 随机字符串，建议 UUID |
| `X-Gateway-Signature` | 是 | HMAC-SHA256 签名结果 |
| `Content-Type` | 是 | `application/json` |

### 签名算法

签名算法：

```text
HmacSHA256
```

签名结果编码：

```text
Base64
```

签名密钥：

```text
AppSecret
```

签名原文格式：

```text
HTTP_METHOD + "\n" +
REQUEST_URI + "\n" +
X-Gateway-Timestamp + "\n" +
X-Gateway-Nonce + "\n" +
RAW_BODY
```

下单接口示例：

```text
POST
/api/gateway/pay/alipay/wap
2026-06-16T15:10:10
550e8400-e29b-41d4-a716-446655440000
{"merchantOrderNo":"ORDER202606161510106416727","subject":"测试商品","totalAmount":0.10}
```

注意：

```text
RAW_BODY 必须是最终发送到 HTTP 请求中的原始 JSON 字符串。
签名时用什么 JSON，发送时就必须用完全相同的 JSON。
字段顺序、空格、小数格式变化都会影响签名。
```

## 三、创建支付宝 WAP 支付订单

### 1. 接口地址

```http
POST /api/gateway/pay/alipay/wap
```

完整本地地址：

```text
http://127.0.0.1:8090/api/gateway/pay/alipay/wap
```

### 2. 请求参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `merchantOrderNo` | string | 是 | 业务系统订单号，最长 64 位 |
| `subject` | string | 是 | 订单标题，最长 128 位 |
| `totalAmount` | number | 是 | 支付金额，最低 0.01，最多两位小数 |
| `returnUrl` | string | 否 | 支付宝同步跳转地址 |
| `businessNotifyUrl` | string | 否 | 业务系统支付结果通知地址，接入应用未配置时使用 |

请求示例：

```json
{
  "merchantOrderNo": "ORDER202606161510106416727",
  "subject": "测试商品",
  "totalAmount": 0.10,
  "returnUrl": "http://127.0.0.1:5173/pay-result",
  "businessNotifyUrl": "http://127.0.0.1:8081/api/merchant/gateway/pay/notify"
}
```

### 3. 响应参数

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "gatewayOrderNo": "GW20260616151010955D819CB17",
    "merchantOrderNo": "ORDER202606161510106416727",
    "payForm": "<form id=\"alipay_submit\" ...></form><script>document.forms['alipay_submit'].submit();</script>"
  }
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `gatewayOrderNo` | 支付网关订单号 |
| `merchantOrderNo` | 业务系统订单号 |
| `payForm` | 支付宝自动提交 HTML 表单 |

### 4. 业务系统处理方式

业务系统收到 `payForm` 后，建议保存到订单表，并提供一个支付中转页。

示例流程：

```text
前端点击支付
  -> 业务后端创建本地订单
  -> 业务后端调用支付网关创建支付订单
  -> 业务后端保存 gatewayOrderNo 和 payForm
  -> 前端跳转到业务后端 pay-page
  -> pay-page 输出 payForm
  -> 浏览器自动 POST 到支付宝收银台
```

不要把 `AppSecret` 或支付宝应用私钥暴露给前端、App 或小程序。

## 四、支付结果通知

支付宝通知支付网关后，支付网关会把处理后的支付结果通知给业务系统。

通知地址来源：

```text
gateway_app.notify_url
或
下单参数 businessNotifyUrl
```

### 1. 通知方式

```http
POST {业务通知地址}
Content-Type: application/json
```

支付网关通知业务系统时，也会带同样的鉴权 Header：

| Header | 说明 |
| --- | --- |
| `X-Gateway-App-Id` | 接入应用 AppId |
| `X-Gateway-Timestamp` | 通知时间戳 |
| `X-Gateway-Nonce` | 随机字符串 |
| `X-Gateway-Signature` | HMAC-SHA256 签名 |

通知签名原文：

```text
POST + "\n" +
通知 URL 的 path + "\n" +
X-Gateway-Timestamp + "\n" +
X-Gateway-Nonce + "\n" +
RAW_BODY
```

示例：如果业务通知地址是：

```text
http://127.0.0.1:8081/api/merchant/gateway/pay/notify
```

签名 path 是：

```text
/api/merchant/gateway/pay/notify
```

### 2. 通知参数

支付网关通知业务系统的 JSON 示例：

```json
{
  "merchantOrderNo": "ORDER202606161510106416727",
  "gatewayOrderNo": "GW20260616151010955D819CB17",
  "appId": "biz-demo",
  "channel": "ALIPAY",
  "status": "SUCCESS",
  "tradeStatus": "TRADE_SUCCESS",
  "alipayTradeNo": "2026061622001400000000000000",
  "totalAmount": 0.10,
  "paidAt": "2026-06-16 15:20:00",
  "notifyPayload": "{...支付宝原始通知内容...}"
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `merchantOrderNo` | 业务系统订单号 |
| `gatewayOrderNo` | 支付网关订单号 |
| `appId` | 接入应用 AppId |
| `channel` | 支付渠道，目前为 `ALIPAY` |
| `status` | 网关订单状态 |
| `tradeStatus` | 支付宝交易状态 |
| `alipayTradeNo` | 支付宝交易号 |
| `totalAmount` | 订单金额 |
| `paidAt` | 支付成功时间 |
| `notifyPayload` | 支付宝原始通知内容 |

### 3. 状态枚举

网关订单状态：

| 值 | 中文说明 | 业务含义 |
| --- | --- | --- |
| `CREATED` | 已创建 | 网关已收到下单请求 |
| `PAYING` | 待支付 | 支付宝支付表单已生成 |
| `SUCCESS` | 交易成功 | 支付成功 |
| `FINISHED` | 交易结束 | 交易结束，当前按失败类状态处理 |
| `CLOSED` | 交易关闭 | 支付关闭 |
| `FAILED` | 失败 | 创建或处理失败 |
| `UNKNOWN` | 未知 | 未识别状态 |

支付宝交易状态：

| 值 | 中文说明 |
| --- | --- |
| `TRADE_SUCCESS` | 支付成功 |
| `TRADE_FINISHED` | 交易结束 |
| `TRADE_CLOSED` | 交易关闭 |

### 4. 业务系统响应

业务系统处理成功后必须返回纯文本：

```text
success
```

如果返回其他内容或接口异常，支付网关会记录业务通知失败。

业务系统需要自己保证幂等：

```text
同一笔订单重复收到 SUCCESS 通知时，不要重复发货、重复加余额或重复改账。
```

## 五、Java 调用示例

下面是一个完整的 Java 11+ 调用示例，使用 `java.net.http.HttpClient`。

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

public class PaymentGatewayDemo {

    private static final String GATEWAY_HOST = "http://127.0.0.1:8090";
    private static final String WAP_PAY_PATH = "/api/gateway/pay/alipay/wap";
    private static final String APP_ID = "biz-demo";
    private static final String APP_SECRET = "demo-secret-change-me";

    public static void main(String[] args) throws Exception {
        String body = """
                {"merchantOrderNo":"ORDER202606161510106416727","subject":"测试商品","totalAmount":0.10,"returnUrl":"http://127.0.0.1:5173/pay-result","businessNotifyUrl":"http://127.0.0.1:8081/api/merchant/gateway/pay/notify"}
                """.trim();

        String timestamp = LocalDateTime.now().toString();
        String nonce = UUID.randomUUID().toString();
        String signText = "POST\n" + WAP_PAY_PATH + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = hmacSha256Base64(APP_SECRET, signText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GATEWAY_HOST + WAP_PAY_PATH))
                .header("Content-Type", "application/json")
                .header("X-Gateway-App-Id", APP_ID)
                .header("X-Gateway-Timestamp", timestamp)
                .header("X-Gateway-Nonce", nonce)
                .header("X-Gateway-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    private static String hmacSha256Base64(String secret, String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }
}
```

## 六、常见错误

### 1. 401 内部应用鉴权参数缺失

原因：

```text
缺少 X-Gateway-App-Id、X-Gateway-Timestamp、X-Gateway-Nonce 或 X-Gateway-Signature
```

### 2. 401 内部应用不存在或已禁用

原因：

```text
AppId 不存在
接入应用被禁用
```

### 3. 401 内部应用签名错误

常见原因：

```text
AppSecret 不正确
签名 path 不正确
签名 body 和实际请求 body 不一致
JSON 序列化后字段顺序或空格变化
```

### 4. 支付宝RSA2签名失败

常见原因：

```text
支付网关后台的支付宝应用私钥配置错误
把支付宝公钥误填到了应用私钥
私钥不是 PKCS8 格式
私钥内容被换行、空格或复制截断破坏
```

### 5. 支付宝页面 invalid-signature

常见原因：

```text
支付宝后台配置的应用公钥和支付网关里的应用私钥不是同一对
AppId 对应错了应用
正式环境和沙箱环境混用
```

### 6. 支付成功后业务订单没有更新

常见原因：

```text
支付宝异步通知地址不是公网可访问地址
支付网关没有收到支付宝通知
业务通知地址为空
业务系统通知接口验签失败
业务系统没有返回 success
```
