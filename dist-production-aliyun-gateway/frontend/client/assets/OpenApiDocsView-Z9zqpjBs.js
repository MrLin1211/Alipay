import{E as e,S as t,_ as n,j as r,m as i,w as a,y as o}from"./index-cnLav2MD.js";var s={class:`docs-page`},c=`METHOD + "\\n" +
PATH + "\\n" +
TIMESTAMP + "\\n" +
NONCE + "\\n" +
RAW_BODY`,l=`{
  "merchantOrderNo": "你的业务订单号",
  "subject": "订单标题",
  "totalAmount": 0.10,
  "returnUrl": "https://你的前端域名/pay-result",
  "notifyUrl": "https://你的后端域名/pay/notify"
}`,u=`{
  "gatewayOrderNo": "网关订单号"
}`,d=`{
  "gatewayOrderNo": "网关订单号",
  "refundAmount": 0.10,
  "refundReason": "用户申请退款"
}`,f=`{
  "gatewayOrderNo": "网关订单号",
  "merchantOrderNo": "你的业务订单号",
  "status": "SUCCESS",
  "tradeStatus": "TRADE_SUCCESS",
  "platformTradeNo": "平台单号",
  "thirdTradeNo": "第三方交易号",
  "paidAt": "2026-08-23 12:00:00"
}`,p={__name:`OpenApiDocsView`,setup(p){let m=[{name:`X-Gateway-App-Id`,desc:`接入方配置里的 AppId`},{name:`X-Gateway-Timestamp`,desc:`毫秒时间戳或秒级时间戳，业务后端自行生成`},{name:`X-Gateway-Nonce`,desc:`随机字符串，建议每次请求唯一`},{name:`X-Gateway-Signature`,desc:`使用 AppSecret 计算出的 HMAC-SHA256 Base64 签名`}];return(p,h)=>{let g=a(`el-table-column`),_=a(`el-table`),v=a(`el-card`);return t(),n(`section`,s,[o(v,{shadow:`never`},{default:e(()=>[h[0]||=i(`h2`,null,`调用链路`,-1),h[1]||=i(`pre`,null,`第三方前端 -> 第三方后端 -> 支付网关 OpenAPI -> 上游支付平台`,-1),h[2]||=i(`h2`,null,`公共请求头`,-1),o(_,{data:m,border:``},{default:e(()=>[o(g,{prop:`name`,label:`Header`,"min-width":`190`}),o(g,{prop:`desc`,label:`说明`,"min-width":`260`})]),_:1}),h[3]||=i(`h2`,null,`签名原文`,-1),i(`pre`,null,r(c)),h[4]||=i(`h2`,null,`创建支付订单`,-1),h[5]||=i(`pre`,null,`POST /api/gateway/pay/orders`,-1),i(`pre`,null,r(l)),h[6]||=i(`h2`,null,`查询支付订单`,-1),h[7]||=i(`pre`,null,`POST /api/gateway/pay/orders/query`,-1),i(`pre`,null,r(u)),h[8]||=i(`h2`,null,`申请退款`,-1),h[9]||=i(`pre`,null,`POST /api/gateway/pay/refunds`,-1),i(`pre`,null,r(d)),h[10]||=i(`h2`,null,`业务支付通知`,-1),i(`pre`,null,r(f))]),_:1})])}}};export{p as default};