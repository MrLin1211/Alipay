import"./modulepreload-polyfill-P2Xu9kJm.js";var e=`gateway-pay-test-config`,t=`gateway-pay-test-orders`,n=[{value:1,label:`手机网站`},{value:2,label:`电脑网站`},{value:3,label:`APP`},{value:4,label:`小程序`},{value:5,label:`公众号`}],r={subject:`支付网关测试商品`,totalAmount:`0.10`,typeIndex:`1`,payMethodType:`ALIPAY_CN`,attachInfo:`gateway-pay-test`,returnUrl:`https://testpay.linsy.online/pay-result.html`,quitUrl:`https://testpay.linsy.online/pay-result.html`,queryGatewayOrderNo:``,queryMerchantOrderNo:``},i=N(localStorage.getItem(e),{});[`gatewayBaseUrl`,`appId`,`appSecret`,`merchantOrderNo`,`businessNotifyUrl`].forEach(e=>delete i[e]),localStorage.setItem(e,JSON.stringify(i));var a={...r,...i},o=null,s=null;document.querySelector(`#app`).innerHTML=`
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
          ${h(`subject`,`订单标题`,`text`)}
          ${h(`totalAmount`,`支付金额`,`number`,`0.01`)}
          ${g(`typeIndex`,`支付类型`,n)}
          ${h(`payMethodType`,`支付方式`,`text`)}
          ${h(`attachInfo`,`附加信息`,`text`)}
          ${h(`returnUrl`,`同步跳转地址`,`url`,``,`field-wide`)}
          ${h(`quitUrl`,`中途退出地址（手机网站必填）`,`url`,``,`field-wide`)}
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
          ${h(`queryGatewayOrderNo`,`网关订单号`,`text`)}
          ${h(`queryMerchantOrderNo`,`业务订单号`,`text`)}
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
`,_(),L(),document.querySelector(`#payForm`).addEventListener(`submit`,async e=>{e.preventDefault(),v();let t=b();if(t){k({success:!1,message:t}),u()&&f(t);return}D(),P(`createPayBtn`,!0);try{let e=await S(`/api/testpay/orders`,x());o=e,k(e);let t=e?.data;e?.code===0&&t?.gatewayOrderNo?(a.queryGatewayOrderNo=t.gatewayOrderNo,a.queryMerchantOrderNo=t.merchantOrderNo||``,y(`queryGatewayOrderNo`,a.queryGatewayOrderNo),y(`queryMerchantOrderNo`,a.queryMerchantOrderNo),F(t),l(t),u()&&d(t)):u()&&f(e?.message||e?.msg||`创建支付订单失败`)}catch(e){k(A(e)),u()&&f(j(e))}finally{P(`createPayBtn`,!1)}}),document.querySelector(`#openPayBtn`).addEventListener(`click`,async()=>{let e=o?.data;e&&await c(e)}),document.querySelector(`#dialogCancelBtn`).addEventListener(`click`,()=>{m()}),document.querySelector(`#dialogConfirmBtn`).addEventListener(`click`,async()=>{let e=s;if(!e){m();return}m(),await c(e)});async function c(e){if(e.evokeMode===`3`){try{await navigator.clipboard.writeText(e.payUrl||``),k({...o,testHint:`APP 支付参数已复制，请交给原生支付宝 SDK 调用。`})}catch{k({...o,testHint:`浏览器无法复制，请从 payUrl 字段取得 APP SDK 支付参数。`})}return}if(e.payUrl){window.open(e.payUrl,`_blank`,`noopener,noreferrer`);return}if(e.payForm){let t=new Blob([e.payForm],{type:`text/html;charset=utf-8`}),n=URL.createObjectURL(t);window.open(n,`_blank`,`noopener,noreferrer`),setTimeout(()=>URL.revokeObjectURL(n),3e4)}}function l(e){let t=document.querySelector(`#openPayBtn`),n=e.evokeMode===`3`;t.textContent=n?`复制 APP 支付参数`:`打开支付页`,t.disabled=n?!e.payUrl:!(e.payUrl||e.payForm)}function u(){return[1,2].includes(Number(a.typeIndex))}function d(e){s=e,p({eyebrow:`订单创建成功`,title:`是否跳转支付？`,message:`支付订单已创建，可以立即前往支付页面。`,details:[[`业务订单号`,e.merchantOrderNo||`-`],[`支付金额`,w(a.totalAmount)]]});let t=document.querySelector(`#dialogCancelBtn`),n=document.querySelector(`#dialogConfirmBtn`);t.hidden=!1,n.textContent=`去支付`,document.querySelector(`#createResultDialog`).showModal()}function f(e){s=null,p({eyebrow:`订单创建失败`,title:`未能创建支付订单`,message:e||`请检查参数后重试。`,details:[]});let t=document.querySelector(`#dialogCancelBtn`),n=document.querySelector(`#dialogConfirmBtn`);t.hidden=!0,n.textContent=`关闭`,document.querySelector(`#createResultDialog`).showModal()}function p({eyebrow:e,title:t,message:n,details:r}){document.querySelector(`#dialogEyebrow`).textContent=e,document.querySelector(`#dialogTitle`).textContent=t,document.querySelector(`#dialogMessage`).textContent=n,document.querySelector(`#dialogDetails`).innerHTML=r.map(([e,t])=>`
    <div><dt>${M(e)}</dt><dd>${M(t)}</dd></div>
  `).join(``)}function m(){let e=document.querySelector(`#createResultDialog`);e.open&&e.close(),s=null}document.querySelector(`#queryForm`).addEventListener(`submit`,async e=>{if(e.preventDefault(),v(),!a.queryGatewayOrderNo&&!a.queryMerchantOrderNo){k({success:!1,message:`网关订单号和业务订单号至少填写一个`});return}D();try{k(await S(`/api/testpay/orders/query`,{gatewayOrderNo:a.queryGatewayOrderNo||void 0,merchantOrderNo:a.queryMerchantOrderNo||void 0}))}catch(e){k(A(e))}}),document.querySelector(`#useLastOrderBtn`).addEventListener(`click`,()=>{let e=I()[0];if(!e){k({success:!1,message:`暂无最近订单`});return}a.queryGatewayOrderNo=e.gatewayOrderNo||``,a.queryMerchantOrderNo=e.merchantOrderNo||``,y(`queryGatewayOrderNo`,a.queryGatewayOrderNo),y(`queryMerchantOrderNo`,a.queryMerchantOrderNo)}),document.querySelector(`#clearResponseBtn`).addEventListener(`click`,()=>{k(`等待请求...`)}),document.querySelector(`#clearRecentBtn`).addEventListener(`click`,()=>{localStorage.removeItem(t),L()});function h(e,t,n,r=``,i=``){return`
    <label class="field ${i}">
      <span>${t}</span>
      <input id="${e}" name="${e}" type="${n}"${r?` step="${r}"`:``} autocomplete="off" />
    </label>
  `}function g(e,t,n){return`
    <label class="field">
      <span>${t}</span>
      <select id="${e}" name="${e}">
        ${n.map(e=>`<option value="${e.value}">${e.label}</option>`).join(``)}
      </select>
    </label>
  `}function _(){Object.keys(r).forEach(e=>y(e,a[e]??``)),document.querySelectorAll(`input, select`).forEach(e=>{e.addEventListener(`input`,()=>{a[e.name]=e.value.trim(),D()})})}function v(){document.querySelectorAll(`input, select`).forEach(e=>{a[e.name]=e.value.trim()})}function y(e,t){let n=document.querySelector(`#${e}`);n&&(n.value=t)}function b(){if(!a.subject)return`请填写订单标题`;let e=Number(a.totalAmount);return!Number.isFinite(e)||e<.01?`支付金额必须大于或等于 0.01`:/^\d+(\.\d{1,2})?$/.test(a.totalAmount)?a.returnUrl&&!O(a.returnUrl)?`同步跳转地址必须是有效的 HTTP 或 HTTPS 地址`:Number(a.typeIndex)===1&&!a.quitUrl?`手机网站支付必须填写中途退出地址`:a.quitUrl&&!O(a.quitUrl)?`中途退出地址必须是有效的 HTTP 或 HTTPS 地址`:``:`支付金额最多保留两位小数`}function x(){return E({subject:a.subject,totalAmount:Number(a.totalAmount),typeIndex:Number(a.typeIndex||1),goodsType:1,payMethodType:a.payMethodType||`ALIPAY_CN`,attachInfo:a.attachInfo,returnUrl:a.returnUrl,quitUrl:a.quitUrl})}async function S(e,t){let n=JSON.stringify(E(t)),r=await fetch(e,{method:`POST`,headers:{"Content-Type":`application/json`},body:n}),i=await r.text(),a=i;try{a=JSON.parse(i)}catch{}if(!r.ok){let e=typeof a==`object`?a.message||a.error||r.statusText:a;throw Error(`HTTP ${r.status}: ${e}`)}return a}function C(e){return`${e.getFullYear()}-${T(e.getMonth()+1)}-${T(e.getDate())} ${T(e.getHours())}:${T(e.getMinutes())}:${T(e.getSeconds())}`}function w(e){let t=Number(e);return Number.isFinite(t)?`¥${t.toFixed(2)}`:`-`}function T(e){return String(e).padStart(2,`0`)}function E(e){return Object.fromEntries(Object.entries(e).filter(([,e])=>e!=null&&e!==``))}function D(){let t={...a};delete t.gatewayBaseUrl,delete t.appId,delete t.appSecret,delete t.merchantOrderNo,delete t.businessNotifyUrl,delete t.queryGatewayOrderNo,delete t.queryMerchantOrderNo,localStorage.setItem(e,JSON.stringify(t))}function O(e){try{let t=new URL(e);return t.protocol===`http:`||t.protocol===`https:`}catch{return!1}}function k(e){document.querySelector(`#responseBox`).textContent=typeof e==`string`?e:JSON.stringify(e,null,2)}function A(e){return{success:!1,message:e?.message||`请求失败`}}function j(e){return String(e?.message||`创建支付订单失败`).replace(/^HTTP\s+\d+:\s*/i,``)||`创建支付订单失败`}function M(e){return String(e).replaceAll(`&`,`&amp;`).replaceAll(`<`,`&lt;`).replaceAll(`>`,`&gt;`).replaceAll(`"`,`&quot;`).replaceAll(`'`,`&#039;`)}function N(e,t){if(!e)return t;try{return JSON.parse(e)}catch{return t}}function P(e,t){let n=document.querySelector(`#${e}`);n&&(n.disabled=t,n.textContent=t?`请求中...`:`创建支付订单`)}function F(e){let n=I();n.unshift({gatewayOrderNo:e.gatewayOrderNo,merchantOrderNo:e.merchantOrderNo,payUrl:e.payUrl,platformTradeNo:e.platformTradeNo,createdAt:C(new Date)}),localStorage.setItem(t,JSON.stringify(n.slice(0,8))),L()}function I(){return N(localStorage.getItem(t),[])}function L(){let e=document.querySelector(`#recentOrders`),t=I();if(!t.length){e.innerHTML=`<p class="empty">暂无记录</p>`;return}e.innerHTML=t.map(e=>`
    <button class="recent-item" data-gateway-order-no="${e.gatewayOrderNo||``}" data-merchant-order-no="${e.merchantOrderNo||``}">
      <strong>${e.gatewayOrderNo||`-`}</strong>
      <span>${e.merchantOrderNo||`-`} · ${e.createdAt||`-`}</span>
    </button>
  `).join(``),e.querySelectorAll(`.recent-item`).forEach(e=>{e.addEventListener(`click`,()=>{a.queryGatewayOrderNo=e.dataset.gatewayOrderNo,a.queryMerchantOrderNo=e.dataset.merchantOrderNo,y(`queryGatewayOrderNo`,a.queryGatewayOrderNo),y(`queryMerchantOrderNo`,a.queryMerchantOrderNo)})})}