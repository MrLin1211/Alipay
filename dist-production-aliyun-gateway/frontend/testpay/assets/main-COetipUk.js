import"./modulepreload-polyfill-P2Xu9kJm.js";var e=`gateway-pay-test-config`,t=`gateway-pay-test-orders`,n=[{value:1,label:`手机网站`},{value:2,label:`电脑网站`},{value:3,label:`APP`},{value:4,label:`小程序`},{value:5,label:`公众号`}],r=`${window.location.origin}/pay-result.html`,i={subject:`支付网关测试商品`,totalAmount:`0.10`,typeIndex:`1`,payMethodType:`ALIPAY_CN`,attachInfo:`gateway-pay-test`,returnUrl:r,quitUrl:r,queryGatewayOrderNo:``,queryMerchantOrderNo:``},a=P(localStorage.getItem(e),{});[`gatewayBaseUrl`,`appId`,`appSecret`,`merchantOrderNo`,`businessNotifyUrl`].forEach(e=>delete a[e]),localStorage.setItem(e,JSON.stringify(a));var o={...i,...a},s=null,c=null;document.querySelector(`#app`).innerHTML=`
  <div class="shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">OpenAPI 调试</p>
        <h1>支付网关测试端</h1>
        <p class="subtitle">模拟第三方接入应用调用支付网关，验证下单、签名、跳转和订单查询。</p>
      </div>
      <span class="badge">正式联调环境</span>
    </header>

    <main class="layout">
      <section class="panel form-panel">
        <div class="panel-title">
          <h2>发起支付</h2>
        </div>
        <div class="warning">
          网关地址和接入密钥由测试端后端统一管理，业务订单号将在创建订单时自动生成。
        </div>
        <form id="payForm" class="grid">
          ${g(`subject`,`订单标题`,`text`)}
          ${g(`totalAmount`,`支付金额`,`number`,`0.01`)}
          ${_(`typeIndex`,`支付类型`,n)}
          ${g(`payMethodType`,`支付方式`,`text`)}
          ${g(`attachInfo`,`附加信息`,`text`)}
          ${g(`returnUrl`,`同步跳转地址`,`url`,``,`field-wide`)}
          ${g(`quitUrl`,`中途退出地址（手机网站必填）`,`url`,``,`field-wide`)}
          <div class="actions">
            <button type="submit" id="createPayBtn" class="primary">创建支付订单</button>
            <button type="button" id="openPayBtn" class="secondary" disabled>打开支付页</button>
          </div>
        </form>
      </section>

      <section class="panel">
        <div class="panel-title">
          <h2>查询订单</h2>
          <button id="useLastOrderBtn" class="ghost">填入最近订单</button>
        </div>
        <form id="queryForm" class="grid compact">
          ${g(`queryGatewayOrderNo`,`网关订单号`,`text`)}
          ${g(`queryMerchantOrderNo`,`业务订单号`,`text`)}
          <div class="actions">
            <button type="submit" class="primary">查询</button>
          </div>
        </form>
      </section>

      <section class="panel response-panel">
        <div class="panel-title">
          <h2>接口响应</h2>
          <button id="clearResponseBtn" class="ghost">清空</button>
        </div>
        <pre id="responseBox">等待请求...</pre>
      </section>

      <section class="panel">
        <div class="panel-title">
          <h2>最近订单</h2>
          <button id="clearRecentBtn" class="ghost">清空记录</button>
        </div>
        <div id="recentOrders" class="recent-list"></div>
      </section>
    </main>

    <dialog id="createResultDialog" class="result-dialog">
      <div class="dialog-content">
        <p id="dialogEyebrow" class="dialog-eyebrow"></p>
        <h2 id="dialogTitle"></h2>
        <p id="dialogMessage" class="dialog-message"></p>
        <dl id="dialogDetails" class="dialog-details"></dl>
        <div class="dialog-actions">
          <button type="button" id="dialogCancelBtn" class="ghost">取消</button>
          <button type="button" id="dialogConfirmBtn" class="primary">去支付</button>
        </div>
      </div>
    </dialog>
  </div>
`,v(),R(),document.querySelector(`#payForm`).addEventListener(`submit`,async e=>{e.preventDefault(),y();let t=x();if(t){A({success:!1,message:t}),d()&&p(t);return}O(),F(`createPayBtn`,!0);try{let e=await C(`/api/testpay/orders`,S());s=e,A(e);let t=e?.data;e?.code===0&&t?.gatewayOrderNo?(o.queryGatewayOrderNo=t.gatewayOrderNo,o.queryMerchantOrderNo=t.merchantOrderNo||``,b(`queryGatewayOrderNo`,o.queryGatewayOrderNo),b(`queryMerchantOrderNo`,o.queryMerchantOrderNo),I(t),u(t),d()&&f(t)):d()&&p(e?.message||e?.msg||`创建支付订单失败`)}catch(e){A(j(e)),d()&&p(M(e))}finally{F(`createPayBtn`,!1)}}),document.querySelector(`#openPayBtn`).addEventListener(`click`,async()=>{let e=s?.data;e&&await l(e)}),document.querySelector(`#dialogCancelBtn`).addEventListener(`click`,()=>{h()}),document.querySelector(`#dialogConfirmBtn`).addEventListener(`click`,async()=>{let e=c;if(!e){h();return}h(),await l(e)});async function l(e){if(e.evokeMode===`3`){try{await navigator.clipboard.writeText(e.payUrl||``),A({...s,testHint:`APP 支付参数已复制，请交给原生支付宝 SDK 调用。`})}catch{A({...s,testHint:`浏览器无法复制，请从 payUrl 字段取得 APP SDK 支付参数。`})}return}if(e.payUrl){window.open(e.payUrl,`_blank`,`noopener,noreferrer`);return}if(e.payForm){let t=new Blob([e.payForm],{type:`text/html;charset=utf-8`}),n=URL.createObjectURL(t);window.open(n,`_blank`,`noopener,noreferrer`),setTimeout(()=>URL.revokeObjectURL(n),3e4)}}function u(e){let t=document.querySelector(`#openPayBtn`),n=e.evokeMode===`3`;t.textContent=n?`复制 APP 支付参数`:`打开支付页`,t.disabled=n?!e.payUrl:!(e.payUrl||e.payForm)}function d(){return[1,2].includes(Number(o.typeIndex))}function f(e){c=e,m({eyebrow:`订单创建成功`,title:`是否跳转支付？`,message:`支付订单已创建，可以立即前往支付页面。`,details:[[`业务订单号`,e.merchantOrderNo||`-`],[`支付金额`,T(o.totalAmount)]]});let t=document.querySelector(`#dialogCancelBtn`),n=document.querySelector(`#dialogConfirmBtn`);t.hidden=!1,n.textContent=`去支付`,document.querySelector(`#createResultDialog`).showModal()}function p(e){c=null,m({eyebrow:`订单创建失败`,title:`未能创建支付订单`,message:e||`请检查参数后重试。`,details:[]});let t=document.querySelector(`#dialogCancelBtn`),n=document.querySelector(`#dialogConfirmBtn`);t.hidden=!0,n.textContent=`关闭`,document.querySelector(`#createResultDialog`).showModal()}function m({eyebrow:e,title:t,message:n,details:r}){document.querySelector(`#dialogEyebrow`).textContent=e,document.querySelector(`#dialogTitle`).textContent=t,document.querySelector(`#dialogMessage`).textContent=n,document.querySelector(`#dialogDetails`).innerHTML=r.map(([e,t])=>`
    <div><dt>${N(e)}</dt><dd>${N(t)}</dd></div>
  `).join(``)}function h(){let e=document.querySelector(`#createResultDialog`);e.open&&e.close(),c=null}document.querySelector(`#queryForm`).addEventListener(`submit`,async e=>{if(e.preventDefault(),y(),!o.queryGatewayOrderNo&&!o.queryMerchantOrderNo){A({success:!1,message:`网关订单号和业务订单号至少填写一个`});return}O();try{A(await C(`/api/testpay/orders/query`,{gatewayOrderNo:o.queryGatewayOrderNo||void 0,merchantOrderNo:o.queryMerchantOrderNo||void 0}))}catch(e){A(j(e))}}),document.querySelector(`#useLastOrderBtn`).addEventListener(`click`,()=>{let e=L()[0];if(!e){A({success:!1,message:`暂无最近订单`});return}o.queryGatewayOrderNo=e.gatewayOrderNo||``,o.queryMerchantOrderNo=e.merchantOrderNo||``,b(`queryGatewayOrderNo`,o.queryGatewayOrderNo),b(`queryMerchantOrderNo`,o.queryMerchantOrderNo)}),document.querySelector(`#clearResponseBtn`).addEventListener(`click`,()=>{A(`等待请求...`)}),document.querySelector(`#clearRecentBtn`).addEventListener(`click`,()=>{localStorage.removeItem(t),R()});function g(e,t,n,r=``,i=``){return`
    <label class="field ${i}">
      <span>${t}</span>
      <input id="${e}" name="${e}" type="${n}"${r?` step="${r}"`:``} autocomplete="off" />
    </label>
  `}function _(e,t,n){return`
    <label class="field">
      <span>${t}</span>
      <select id="${e}" name="${e}">
        ${n.map(e=>`<option value="${e.value}">${e.label}</option>`).join(``)}
      </select>
    </label>
  `}function v(){Object.keys(i).forEach(e=>b(e,o[e]??``)),document.querySelectorAll(`input, select`).forEach(e=>{e.addEventListener(`input`,()=>{o[e.name]=e.value.trim(),O()})})}function y(){document.querySelectorAll(`input, select`).forEach(e=>{o[e.name]=e.value.trim()})}function b(e,t){let n=document.querySelector(`#${e}`);n&&(n.value=t)}function x(){if(!o.subject)return`请填写订单标题`;let e=Number(o.totalAmount);return!Number.isFinite(e)||e<.01?`支付金额必须大于或等于 0.01`:/^\d+(\.\d{1,2})?$/.test(o.totalAmount)?o.returnUrl&&!k(o.returnUrl)?`同步跳转地址必须是有效的 HTTP 或 HTTPS 地址`:Number(o.typeIndex)===1&&!o.quitUrl?`手机网站支付必须填写中途退出地址`:o.quitUrl&&!k(o.quitUrl)?`中途退出地址必须是有效的 HTTP 或 HTTPS 地址`:``:`支付金额最多保留两位小数`}function S(){return D({subject:o.subject,totalAmount:Number(o.totalAmount),typeIndex:Number(o.typeIndex||1),goodsType:1,payMethodType:o.payMethodType||`ALIPAY_CN`,attachInfo:o.attachInfo,returnUrl:o.returnUrl,quitUrl:o.quitUrl})}async function C(e,t){let n=JSON.stringify(D(t)),r=await fetch(e,{method:`POST`,headers:{"Content-Type":`application/json`},body:n}),i=await r.text(),a=i;try{a=JSON.parse(i)}catch{}if(!r.ok){let e=typeof a==`object`?a.message||a.error||r.statusText:a;throw Error(`HTTP ${r.status}: ${e}`)}return a}function w(e){return`${e.getFullYear()}-${E(e.getMonth()+1)}-${E(e.getDate())} ${E(e.getHours())}:${E(e.getMinutes())}:${E(e.getSeconds())}`}function T(e){let t=Number(e);return Number.isFinite(t)?`¥${t.toFixed(2)}`:`-`}function E(e){return String(e).padStart(2,`0`)}function D(e){return Object.fromEntries(Object.entries(e).filter(([,e])=>e!=null&&e!==``))}function O(){let t={...o};delete t.gatewayBaseUrl,delete t.appId,delete t.appSecret,delete t.merchantOrderNo,delete t.businessNotifyUrl,delete t.queryGatewayOrderNo,delete t.queryMerchantOrderNo,localStorage.setItem(e,JSON.stringify(t))}function k(e){try{let t=new URL(e);return t.protocol===`http:`||t.protocol===`https:`}catch{return!1}}function A(e){document.querySelector(`#responseBox`).textContent=typeof e==`string`?e:JSON.stringify(e,null,2)}function j(e){return{success:!1,message:e?.message||`请求失败`}}function M(e){return String(e?.message||`创建支付订单失败`).replace(/^HTTP\s+\d+:\s*/i,``)||`创建支付订单失败`}function N(e){return String(e).replaceAll(`&`,`&amp;`).replaceAll(`<`,`&lt;`).replaceAll(`>`,`&gt;`).replaceAll(`"`,`&quot;`).replaceAll(`'`,`&#039;`)}function P(e,t){if(!e)return t;try{return JSON.parse(e)}catch{return t}}function F(e,t){let n=document.querySelector(`#${e}`);n&&(n.disabled=t,n.textContent=t?`请求中...`:`创建支付订单`)}function I(e){let n=L();n.unshift({gatewayOrderNo:e.gatewayOrderNo,merchantOrderNo:e.merchantOrderNo,payUrl:e.payUrl,platformTradeNo:e.platformTradeNo,createdAt:w(new Date)}),localStorage.setItem(t,JSON.stringify(n.slice(0,8))),R()}function L(){return P(localStorage.getItem(t),[])}function R(){let e=document.querySelector(`#recentOrders`),t=L();if(!t.length){e.innerHTML=`<p class="empty">暂无记录</p>`;return}e.innerHTML=t.map(e=>`
    <button class="recent-item" data-gateway-order-no="${e.gatewayOrderNo||``}" data-merchant-order-no="${e.merchantOrderNo||``}">
      <strong>${e.gatewayOrderNo||`-`}</strong>
      <span>${e.merchantOrderNo||`-`} · ${e.createdAt||`-`}</span>
    </button>
  `).join(``),e.querySelectorAll(`.recent-item`).forEach(e=>{e.addEventListener(`click`,()=>{o.queryGatewayOrderNo=e.dataset.gatewayOrderNo,o.queryMerchantOrderNo=e.dataset.merchantOrderNo,b(`queryGatewayOrderNo`,o.queryGatewayOrderNo),b(`queryMerchantOrderNo`,o.queryMerchantOrderNo)})})}