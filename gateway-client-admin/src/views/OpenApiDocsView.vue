<template>
  <section class="docs-page">
    <div class="docs-intro">
      <div>
        <span class="docs-kicker">OPENAPI</span>
        <h2>支付网关接入文档</h2>
        <p>第三方前端只负责展示，订单创建、签名和 AppSecret 必须保留在第三方后端。</p>
      </div>
      <el-tag type="success" effect="plain">当前网关：{{ baseUrl }}</el-tag>
    </div>

    <el-alert
      title="AppSecret 不得写入网页、APP、小程序或其他客户端代码，也不要通过前端请求临时获取。"
      type="warning"
      :closable="false"
      show-icon
      class="security-alert"
    />

    <el-card shadow="never" class="docs-shell">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="快速接入" name="quick">
          <article class="doc-section">
            <h3>调用链路</h3>
            <div class="flow-line">
              <span>第三方前端</span><b>→</b><span>第三方后端</span><b>→</b><span>支付网关</span><b>→</b><span>上游支付平台</span>
            </div>

            <h3>基础信息</h3>
            <el-descriptions :column="descriptionColumns" border>
              <el-descriptions-item label="生产地址"><code>{{ baseUrl }}</code></el-descriptions-item>
              <el-descriptions-item label="请求格式"><code>application/json; charset=UTF-8</code></el-descriptions-item>
              <el-descriptions-item label="成功判断"><code>HTTP 2xx 且 code = 0</code></el-descriptions-item>
              <el-descriptions-item label="金额单位">人民币元，最多两位小数</el-descriptions-item>
            </el-descriptions>

            <h3>公共请求头</h3>
            <el-table :data="headers" border>
              <el-table-column prop="name" label="Header" min-width="190" />
              <el-table-column prop="required" label="必填" width="80" />
              <el-table-column prop="desc" label="说明" min-width="320" />
            </el-table>

            <div class="section-heading">
              <h3>签名规则</h3>
              <el-button text type="primary" :icon="CopyDocument" @click="copyText(signatureText)">复制签名原文</el-button>
            </div>
            <pre>{{ signatureText }}</pre>
            <p class="doc-note">PATH 只填写请求路径，例如 <code>/api/gateway/pay/orders</code>；RAW_BODY 必须与实际发送的 JSON 字节内容完全一致。</p>

            <div class="section-heading">
              <h3>Java 17 签名与请求示例</h3>
              <el-button text type="primary" :icon="CopyDocument" @click="copyText(javaExample)">复制 Java 示例</el-button>
            </div>
            <pre>{{ javaExample }}</pre>
          </article>
        </el-tab-pane>

        <el-tab-pane label="支付接口" name="apis">
          <article class="doc-section">
            <div class="endpoint-title"><el-tag>POST</el-tag><code>/api/gateway/pay/orders</code><strong>创建支付订单</strong></div>
            <el-table :data="createFields" border>
              <el-table-column prop="name" label="字段" min-width="170" />
              <el-table-column prop="type" label="类型" width="100" />
              <el-table-column prop="required" label="必填" width="90" />
              <el-table-column prop="desc" label="说明" min-width="360" />
            </el-table>
            <pre>{{ createOrderBody }}</pre>
            <pre>{{ createOrderResponse }}</pre>

            <h3>支付载荷处理</h3>
            <el-table :data="payPayloadRules" border>
              <el-table-column prop="evokeMode" label="evokeMode" width="120" />
              <el-table-column prop="scene" label="场景" width="150" />
              <el-table-column prop="handling" label="接入方处理方式" min-width="360" />
            </el-table>

            <div class="endpoint-title"><el-tag>POST</el-tag><code>/api/gateway/pay/orders/query</code><strong>查询支付订单</strong></div>
            <p class="doc-note">传入 <code>gatewayOrderNo</code> 或 <code>merchantOrderNo</code> 其中一个。两者同时传入时以网关订单号查询。</p>
            <pre>{{ queryOrderBody }}</pre>

            <div class="endpoint-title"><el-tag type="danger">POST</el-tag><code>/api/gateway/pay/refunds</code><strong>申请退款</strong></div>
            <p class="doc-note">退款必须由第三方后端发起。退款金额最低 0.01 元、最多两位小数，退款原因最长 128 个字符。</p>
            <pre>{{ refundBody }}</pre>
            <pre>{{ refundResponse }}</pre>
          </article>
        </el-tab-pane>

        <el-tab-pane label="通知回调" name="notify">
          <article class="doc-section">
            <h3>通知地址优先级</h3>
            <div class="priority-line"><b>1</b><span>接入配置中的业务通知地址</span><b>2</b><span>本次下单的 businessNotifyUrl</span><b>3</b><span>均为空时不发送</span></div>

            <h3>通知请求</h3>
            <p class="doc-note">网关使用 <code>POST application/json</code> 通知。请求头及签名算法与 OpenAPI 相同，签名密钥仍为当前应用的 AppSecret。</p>
            <pre>{{ notifyBody }}</pre>

            <h3>验签与应答</h3>
            <ol class="steps">
              <li>读取原始请求体，不要先反序列化再重新生成 JSON。</li>
              <li>按 <code>POST + PATH + TIMESTAMP + NONCE + RAW_BODY</code> 计算 HMAC-SHA256 Base64。</li>
              <li>校验 AppId 与签名，并使用 gatewayOrderNo 或 merchantOrderNo 做幂等处理。</li>
              <li>业务处理完成后返回 HTTP 2xx，响应体必须是纯文本 <code>success</code>。</li>
            </ol>
            <el-alert
              title="当前每次平台支付结果只立即通知一次；通知失败会记录为失败，不会自动重试。接入方应同时保留主动查询订单的补偿机制。"
              type="info"
              :closable="false"
              show-icon
            />

            <h3>同步跳转边界</h3>
            <p class="doc-note"><code>returnUrl</code> 只用于浏览器支付完成后的页面跳转，不能作为支付成功依据。最终状态必须以异步通知或订单查询结果为准。</p>
          </article>
        </el-tab-pane>

        <el-tab-pane label="状态与错误" name="status">
          <article class="doc-section">
            <h3>网关订单状态</h3>
            <el-table :data="orderStatuses" border>
              <el-table-column prop="value" label="状态值" width="150" />
              <el-table-column prop="label" label="中文含义" width="150" />
              <el-table-column prop="desc" label="处理建议" min-width="320" />
            </el-table>

            <h3>平台交易状态</h3>
            <el-table :data="tradeStatuses" border>
              <el-table-column prop="value" label="状态值" min-width="200" />
              <el-table-column prop="label" label="中文含义" min-width="180" />
            </el-table>

            <h3>错误响应</h3>
            <pre>{{ errorResponse }}</pre>
            <el-table :data="errorCodes" border>
              <el-table-column prop="code" label="HTTP 状态" width="130" />
              <el-table-column prop="meaning" label="含义" min-width="220" />
              <el-table-column prop="handling" label="处理建议" min-width="320" />
            </el-table>
          </article>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>

<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { CopyDocument } from "@element-plus/icons-vue";
import { gatewayBaseUrl } from "../api/client";

const activeTab = ref("quick");
const descriptionColumns = computed(() => (window.innerWidth <= 760 ? 1 : 2));
const baseUrl = gatewayBaseUrl.value;

const headers = [
  { name: "X-Gateway-App-Id", required: "是", desc: "接入配置中的 AppId" },
  { name: "X-Gateway-Timestamp", required: "是", desc: "本次请求时间，建议使用 ISO-8601 字符串；签名与请求头必须完全一致" },
  { name: "X-Gateway-Nonce", required: "是", desc: "本次请求随机字符串，建议使用 UUID 且每次请求唯一" },
  { name: "X-Gateway-Signature", required: "是", desc: "使用 AppSecret 对签名原文计算 HMAC-SHA256 后进行 Base64 编码" },
  { name: "Content-Type", required: "是", desc: "application/json; charset=UTF-8" }
];

const createFields = [
  { name: "merchantOrderNo", type: "string", required: "是", desc: "第三方业务订单号，最长 64 个字符，同一 AppId 下必须唯一" },
  { name: "subject", type: "string", required: "是", desc: "订单标题，最长 128 个字符" },
  { name: "totalAmount", type: "number", required: "是", desc: "支付金额，最低 0.01 元，最多两位小数" },
  { name: "typeIndex", type: "integer", required: "否", desc: "1=手机网站，2=电脑网站，3=APP，4=小程序，5=公众号；默认 1" },
  { name: "goodsType", type: "integer", required: "否", desc: "商品类型，默认 1" },
  { name: "payMethodType", type: "string", required: "否", desc: "支付方式，默认 ALIPAY_CN" },
  { name: "returnUrl", type: "string", required: "否", desc: "第三方同步跳转地址，必须为 HTTP/HTTPS，最长 512 个字符" },
  { name: "quitUrl", type: "string", required: "条件", desc: "手机网站支付 typeIndex=1 时必填，最长 512 个字符" },
  { name: "businessNotifyUrl", type: "string", required: "否", desc: "本次订单业务通知地址；仅在接入配置未设置通知地址时使用" },
  { name: "clientIp", type: "string", required: "否", desc: "付款用户 IP；不传时由网关从请求头获取" },
  { name: "attachInfo", type: "string", required: "否", desc: "附加信息，最长 512 个字符" },
  { name: "subExternalId", type: "string", required: "否", desc: "子商家编号，最长 64 个字符" }
];

const payPayloadRules = [
  { evokeMode: "0", scene: "网页地址", handling: "使用浏览器跳转 payUrl" },
  { evokeMode: "1", scene: "支付表单", handling: "使用网关返回的 payForm 页面内容发起支付" },
  { evokeMode: "2", scene: "二维码", handling: "将 payUrl 作为二维码内容生成并展示" },
  { evokeMode: "3", scene: "APP 支付", handling: "payUrl 实际为 orderInfo，交给原生支付宝 SDK；不能用浏览器或 WebView 打开" }
];

const orderStatuses = [
  { value: "CREATED", label: "已创建", desc: "订单已入库，尚未获得支付平台响应" },
  { value: "PAYING", label: "待支付", desc: "已经生成支付载荷，等待用户完成支付" },
  { value: "SUCCESS", label: "交易成功", desc: "可进入发货或业务履约流程" },
  { value: "REFUNDED", label: "已退款", desc: "退款已成功" },
  { value: "FINISHED", label: "交易结束", desc: "按失败类状态处理，不允许发起退款" },
  { value: "CLOSED", label: "交易关闭", desc: "支付未成功或交易已关闭" },
  { value: "FAILED", label: "失败", desc: "创建或渠道请求失败，可根据错误原因决定是否重新下单" },
  { value: "UNKNOWN", label: "未知", desc: "主动查询订单状态并人工核对" }
];

const tradeStatuses = [
  { value: "WAIT_BUYER_PAY", label: "待支付" },
  { value: "TRADE_SUCCESS", label: "支付成功" },
  { value: "TRADE_FINISHED", label: "交易结束" },
  { value: "TRADE_CLOSED", label: "交易关闭" },
  { value: "TRADE_REFUND_SUCCESS", label: "退款成功" }
];

const errorCodes = [
  { code: "400", meaning: "请求参数不合法", handling: "根据 message 修正字段、金额、URL 或必填项" },
  { code: "401", meaning: "应用不存在、禁用或签名错误", handling: "核对 AppId、AppSecret、请求路径和原始请求体" },
  { code: "404", meaning: "订单不存在", handling: "确认订单属于当前 AppId，并核对订单号" },
  { code: "502", meaning: "上游支付平台拒绝或异常", handling: "保留 message 和业务订单号，排查通道配置或平台参数" },
  { code: "500", meaning: "网关内部异常", handling: "不要盲目更换业务订单号重试，先查询原订单或联系网关管理员" }
];

const signatureText = `POST\n/api/gateway/pay/orders\n2026-08-23T12:00:00+08:00\n550e8400-e29b-41d4-a716-446655440000\n{"merchantOrderNo":"M202608230001","subject":"测试商品","totalAmount":0.10,"typeIndex":2}`;

const createOrderBody = `请求体\n{
  "merchantOrderNo": "M202608230001",
  "subject": "测试商品",
  "totalAmount": 0.10,
  "typeIndex": 2,
  "goodsType": 1,
  "payMethodType": "ALIPAY_CN",
  "returnUrl": "https://merchant.example.com/pay-result",
  "businessNotifyUrl": "https://merchant.example.com/api/pay/notify"
}`;

const createOrderResponse = `成功响应\n{
  "code": 0,
  "message": "success",
  "data": {
    "gatewayOrderNo": "GW202608231030001234ABCD",
    "merchantOrderNo": "M202608230001",
    "payUrl": "https://payment.example.com/...",
    "evokeMode": "0",
    "platformTradeNo": "XD202608230001",
    "payForm": "<!doctype html>..."
  }
}`;

const queryOrderBody = `按网关订单号查询\n{
  "gatewayOrderNo": "GW202608231030001234ABCD"
}

按业务订单号查询\n{
  "merchantOrderNo": "M202608230001"
}`;

const refundBody = `请求体\n{
  "gatewayOrderNo": "GW202608231030001234ABCD",
  "refundAmount": 0.10,
  "refundReason": "用户申请退款"
}`;

const refundResponse = `成功响应\n{
  "code": 0,
  "message": "success",
  "data": {
    "refundOrderNo": "RF202608231100001234ABCD",
    "status": "SUCCESS",
    "platformResponse": {}
  }
}`;

const notifyBody = `POST https://merchant.example.com/api/pay/notify
X-Gateway-App-Id: gwapp_xxx
X-Gateway-Timestamp: 2026-08-23T12:00:00
X-Gateway-Nonce: 550e8400-e29b-41d4-a716-446655440000
X-Gateway-Signature: Base64-HMAC-SHA256
Content-Type: application/json

{
  "merchantOrderNo": "M202608230001",
  "gatewayOrderNo": "GW202608231030001234ABCD",
  "appId": "gwapp_xxx",
  "channel": "ZHENBAOGE",
  "status": "SUCCESS",
  "tradeStatus": "TRADE_SUCCESS",
  "platformTradeNo": "XD202608230001",
  "thirdTradeNo": "202608232200001",
  "totalAmount": 0.10,
  "paidAt": "2026-08-23 12:00:00",
  "notifyPayload": "{...上游平台原始通知...}"
}`;

const errorResponse = `{
  "code": 401,
  "message": "内部应用签名错误",
  "data": null
}`;

const javaExample = `String path = "/api/gateway/pay/orders";
String body = "{\\\"merchantOrderNo\\\":\\\"M202608230001\\\",\\\"subject\\\":\\\"测试商品\\\",\\\"totalAmount\\\":0.10,\\\"typeIndex\\\":2}";
String timestamp = java.time.OffsetDateTime.now().toString();
String nonce = java.util.UUID.randomUUID().toString();
String signText = "POST\\n" + path + "\\n" + timestamp + "\\n" + nonce + "\\n" + body;

javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
mac.init(new javax.crypto.spec.SecretKeySpec(
    appSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
String signature = java.util.Base64.getEncoder().encodeToString(
    mac.doFinal(signText.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
    .uri(java.net.URI.create("${baseUrl}" + path))
    .header("Content-Type", "application/json; charset=UTF-8")
    .header("X-Gateway-App-Id", appId)
    .header("X-Gateway-Timestamp", timestamp)
    .header("X-Gateway-Nonce", nonce)
    .header("X-Gateway-Signature", signature)
    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
    .build();

java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
    .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());`;

async function copyText(value) {
  try {
    await navigator.clipboard.writeText(value);
    ElMessage.success("已复制");
  } catch {
    ElMessage.error("复制失败，请手动选择文本");
  }
}
</script>

<style scoped>
.docs-page {
  display: grid;
  gap: 16px;
}

.docs-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 2px;
}

.docs-intro h2 {
  margin: 4px 0 6px;
  font-size: 24px;
}

.docs-intro p,
.doc-note {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.docs-kicker {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.security-alert {
  margin: 0;
}

.docs-shell :deep(.el-card__body) {
  padding: 18px 20px 24px;
}

.doc-section {
  max-width: 1180px;
}

.doc-section h3 {
  margin: 26px 0 12px;
  font-size: 17px;
}

.doc-section h3:first-child {
  margin-top: 10px;
}

.flow-line,
.priority-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px;
  border: 1px solid #dfe5ec;
  border-radius: 6px;
  background: #f8fafc;
}

.flow-line span,
.priority-line span {
  font-weight: 600;
}

.priority-line b {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #0f766e;
  color: #fff;
}

.section-heading,
.endpoint-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.section-heading {
  justify-content: space-between;
  margin-top: 24px;
}

.section-heading h3 {
  margin: 0;
}

.endpoint-title {
  margin: 14px 0;
}

.endpoint-title code {
  font-size: 15px;
}

code {
  font-family: "SFMono-Regular", Consolas, monospace;
  overflow-wrap: anywhere;
}

.steps {
  margin: 0 0 18px;
  padding-left: 24px;
  color: #344054;
  line-height: 1.9;
}

pre {
  max-height: 520px;
}

@media (max-width: 760px) {
  .docs-intro {
    align-items: flex-start;
    flex-direction: column;
  }

  .docs-intro h2 {
    font-size: 21px;
  }

  .docs-shell :deep(.el-card__body) {
    padding: 12px;
  }

  .docs-shell :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }

  .flow-line,
  .priority-line {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
