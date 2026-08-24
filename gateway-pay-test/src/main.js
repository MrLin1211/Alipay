import './styles.css';

const storageKey = 'gateway-pay-test-config';
const recentOrdersKey = 'gateway-pay-test-orders';

const typeOptions = [
  { value: 1, label: '手机网站' },
  { value: 2, label: '电脑网站' },
  { value: 3, label: 'APP' },
  { value: 4, label: '小程序' },
  { value: 5, label: '公众号' }
];

const defaultPayResultUrl = `${window.location.origin}/pay-result.html`;

const defaultState = {
  subject: '支付网关测试商品',
  totalAmount: '0.10',
  typeIndex: '1',
  payMethodType: 'ALIPAY_CN',
  attachInfo: 'gateway-pay-test',
  returnUrl: defaultPayResultUrl,
  quitUrl: defaultPayResultUrl,
  queryGatewayOrderNo: '',
  queryMerchantOrderNo: ''
};

const persistedState = safeJson(localStorage.getItem(storageKey), {});
['gatewayBaseUrl', 'appId', 'appSecret', 'merchantOrderNo', 'businessNotifyUrl']
  .forEach((key) => delete persistedState[key]);
localStorage.setItem(storageKey, JSON.stringify(persistedState));

const state = {
  ...defaultState,
  ...persistedState
};

let lastCreateResponse = null;
let pendingDialogPayment = null;

document.querySelector('#app').innerHTML = `
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
          ${field('subject', '订单标题', 'text')}
          ${field('totalAmount', '支付金额', 'number', '0.01')}
          ${selectField('typeIndex', '支付类型', typeOptions)}
          ${field('payMethodType', '支付方式', 'text')}
          ${field('attachInfo', '附加信息', 'text')}
          ${field('returnUrl', '同步跳转地址', 'url', '', 'field-wide')}
          ${field('quitUrl', '中途退出地址（手机网站必填）', 'url', '', 'field-wide')}
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
          ${field('queryGatewayOrderNo', '网关订单号', 'text')}
          ${field('queryMerchantOrderNo', '业务订单号', 'text')}
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
`;

bindStateToInputs();
renderRecentOrders();

document.querySelector('#payForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  readInputs();
  const error = validatePayForm();
  if (error) {
    showResponse({ success: false, message: error });
    if (isWebPayment()) {
      showCreateFailure(error);
    }
    return;
  }
  saveConfig();
  setLoading('createPayBtn', true);
  try {
    const body = buildCreateBody();
    const result = await postJson('/api/testpay/orders', body);
    lastCreateResponse = result;
    showResponse(result);
    const data = result?.data;
    if (result?.code === 0 && data?.gatewayOrderNo) {
      state.queryGatewayOrderNo = data.gatewayOrderNo;
      state.queryMerchantOrderNo = data.merchantOrderNo || '';
      setInputValue('queryGatewayOrderNo', state.queryGatewayOrderNo);
      setInputValue('queryMerchantOrderNo', state.queryMerchantOrderNo);
      pushRecentOrder(data);
      configurePayAction(data);
      if (isWebPayment()) {
        showPaymentConfirm(data);
      }
    } else if (isWebPayment()) {
      showCreateFailure(result?.message || result?.msg || '创建支付订单失败');
    }
  } catch (error) {
    showResponse(normalizeError(error));
    if (isWebPayment()) {
      showCreateFailure(errorReason(error));
    }
  } finally {
    setLoading('createPayBtn', false);
  }
});

document.querySelector('#openPayBtn').addEventListener('click', async () => {
  const data = lastCreateResponse?.data;
  if (!data) {
    return;
  }
  await handlePayAction(data);
});

document.querySelector('#dialogCancelBtn').addEventListener('click', () => {
  closeCreateDialog();
});

document.querySelector('#dialogConfirmBtn').addEventListener('click', async () => {
  const data = pendingDialogPayment;
  if (!data) {
    closeCreateDialog();
    return;
  }
  closeCreateDialog();
  await handlePayAction(data);
});

async function handlePayAction(data) {
  if (data.evokeMode === '3') {
    try {
      await navigator.clipboard.writeText(data.payUrl || '');
      showResponse({
        ...lastCreateResponse,
        testHint: 'APP 支付参数已复制，请交给原生支付宝 SDK 调用。'
      });
    } catch {
      showResponse({
        ...lastCreateResponse,
        testHint: '浏览器无法复制，请从 payUrl 字段取得 APP SDK 支付参数。'
      });
    }
    return;
  }
  if (data.payUrl) {
    window.open(data.payUrl, '_blank', 'noopener,noreferrer');
    return;
  }
  if (data.payForm) {
    const blob = new Blob([data.payForm], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    window.open(url, '_blank', 'noopener,noreferrer');
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  }
}

function configurePayAction(data) {
  const button = document.querySelector('#openPayBtn');
  const isAppPay = data.evokeMode === '3';
  button.textContent = isAppPay ? '复制 APP 支付参数' : '打开支付页';
  button.disabled = isAppPay ? !data.payUrl : !(data.payUrl || data.payForm);
}

function isWebPayment() {
  return [1, 2].includes(Number(state.typeIndex));
}

function showPaymentConfirm(data) {
  pendingDialogPayment = data;
  setDialogText({
    eyebrow: '订单创建成功',
    title: '是否跳转支付？',
    message: '支付订单已创建，可以立即前往支付页面。',
    details: [
      ['业务订单号', data.merchantOrderNo || '-'],
      ['支付金额', formatAmount(state.totalAmount)]
    ]
  });
  const cancelButton = document.querySelector('#dialogCancelBtn');
  const confirmButton = document.querySelector('#dialogConfirmBtn');
  cancelButton.hidden = false;
  confirmButton.textContent = '去支付';
  document.querySelector('#createResultDialog').showModal();
}

function showCreateFailure(message) {
  pendingDialogPayment = null;
  setDialogText({
    eyebrow: '订单创建失败',
    title: '未能创建支付订单',
    message: message || '请检查参数后重试。',
    details: []
  });
  const cancelButton = document.querySelector('#dialogCancelBtn');
  const confirmButton = document.querySelector('#dialogConfirmBtn');
  cancelButton.hidden = true;
  confirmButton.textContent = '关闭';
  document.querySelector('#createResultDialog').showModal();
}

function setDialogText({ eyebrow, title, message, details }) {
  document.querySelector('#dialogEyebrow').textContent = eyebrow;
  document.querySelector('#dialogTitle').textContent = title;
  document.querySelector('#dialogMessage').textContent = message;
  document.querySelector('#dialogDetails').innerHTML = details.map(([label, value]) => `
    <div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>
  `).join('');
}

function closeCreateDialog() {
  const dialog = document.querySelector('#createResultDialog');
  if (dialog.open) {
    dialog.close();
  }
  pendingDialogPayment = null;
}

document.querySelector('#queryForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  readInputs();
  if (!state.queryGatewayOrderNo && !state.queryMerchantOrderNo) {
    showResponse({ success: false, message: '网关订单号和业务订单号至少填写一个' });
    return;
  }
  saveConfig();
  try {
    const body = {
      gatewayOrderNo: state.queryGatewayOrderNo || undefined,
      merchantOrderNo: state.queryMerchantOrderNo || undefined
    };
    showResponse(await postJson('/api/testpay/orders/query', body));
  } catch (error) {
    showResponse(normalizeError(error));
  }
});

document.querySelector('#useLastOrderBtn').addEventListener('click', () => {
  const orders = getRecentOrders();
  const latest = orders[0];
  if (!latest) {
    showResponse({ success: false, message: '暂无最近订单' });
    return;
  }
  state.queryGatewayOrderNo = latest.gatewayOrderNo || '';
  state.queryMerchantOrderNo = latest.merchantOrderNo || '';
  setInputValue('queryGatewayOrderNo', state.queryGatewayOrderNo);
  setInputValue('queryMerchantOrderNo', state.queryMerchantOrderNo);
});

document.querySelector('#clearResponseBtn').addEventListener('click', () => {
  showResponse('等待请求...');
});

document.querySelector('#clearRecentBtn').addEventListener('click', () => {
  localStorage.removeItem(recentOrdersKey);
  renderRecentOrders();
});

function field(id, label, type, step = '', className = '') {
  const stepAttr = step ? ` step="${step}"` : '';
  return `
    <label class="field ${className}">
      <span>${label}</span>
      <input id="${id}" name="${id}" type="${type}"${stepAttr} autocomplete="off" />
    </label>
  `;
}

function selectField(id, label, options) {
  return `
    <label class="field">
      <span>${label}</span>
      <select id="${id}" name="${id}">
        ${options.map((item) => `<option value="${item.value}">${item.label}</option>`).join('')}
      </select>
    </label>
  `;
}

function bindStateToInputs() {
  Object.keys(defaultState).forEach((key) => setInputValue(key, state[key] ?? ''));
  document.querySelectorAll('input, select').forEach((input) => {
    input.addEventListener('input', () => {
      state[input.name] = input.value.trim();
      saveConfig();
    });
  });
}

function readInputs() {
  document.querySelectorAll('input, select').forEach((input) => {
    state[input.name] = input.value.trim();
  });
}

function setInputValue(id, value) {
  const input = document.querySelector(`#${id}`);
  if (input) {
    input.value = value;
  }
}

function validatePayForm() {
  if (!state.subject) return '请填写订单标题';
  const amount = Number(state.totalAmount);
  if (!Number.isFinite(amount) || amount < 0.01) return '支付金额必须大于或等于 0.01';
  if (!/^\d+(\.\d{1,2})?$/.test(state.totalAmount)) return '支付金额最多保留两位小数';
  if (state.returnUrl && !isHttpUrl(state.returnUrl)) return '同步跳转地址必须是有效的 HTTP 或 HTTPS 地址';
  if (Number(state.typeIndex) === 1 && !state.quitUrl) return '手机网站支付必须填写中途退出地址';
  if (state.quitUrl && !isHttpUrl(state.quitUrl)) return '中途退出地址必须是有效的 HTTP 或 HTTPS 地址';
  return '';
}

function buildCreateBody() {
  return compactObject({
    subject: state.subject,
    totalAmount: Number(state.totalAmount),
    typeIndex: Number(state.typeIndex || 1),
    goodsType: 1,
    payMethodType: state.payMethodType || 'ALIPAY_CN',
    attachInfo: state.attachInfo,
    returnUrl: state.returnUrl,
    quitUrl: state.quitUrl
  });
}

async function postJson(path, body) {
  const rawBody = JSON.stringify(compactObject(body));
  const response = await fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: rawBody
  });
  const text = await response.text();
  let data = text;
  try {
    data = JSON.parse(text);
  } catch {
    // 非 JSON 响应直接展示原文，便于排查网关或上游错误。
  }
  if (!response.ok) {
    const message = typeof data === 'object' ? data.message || data.error || response.statusText : data;
    throw new Error(`HTTP ${response.status}: ${message}`);
  }
  return data;
}

function formatTimestamp(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatAmount(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '-';
}

function pad(value) {
  return String(value).padStart(2, '0');
}

function compactObject(obj) {
  return Object.fromEntries(
    Object.entries(obj).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
}

function saveConfig() {
  const saved = { ...state };
  delete saved.gatewayBaseUrl;
  delete saved.appId;
  delete saved.appSecret;
  delete saved.merchantOrderNo;
  delete saved.businessNotifyUrl;
  delete saved.queryGatewayOrderNo;
  delete saved.queryMerchantOrderNo;
  localStorage.setItem(storageKey, JSON.stringify(saved));
}

function isHttpUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

function showResponse(value) {
  document.querySelector('#responseBox').textContent =
    typeof value === 'string' ? value : JSON.stringify(value, null, 2);
}

function normalizeError(error) {
  return {
    success: false,
    message: error?.message || '请求失败'
  };
}

function errorReason(error) {
  return String(error?.message || '创建支付订单失败').replace(/^HTTP\s+\d+:\s*/i, '') || '创建支付订单失败';
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function safeJson(value, fallback) {
  if (!value) return fallback;
  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function setLoading(id, loading) {
  const button = document.querySelector(`#${id}`);
  if (!button) return;
  button.disabled = loading;
  button.textContent = loading ? '请求中...' : '创建支付订单';
}

function pushRecentOrder(data) {
  const orders = getRecentOrders();
  orders.unshift({
    gatewayOrderNo: data.gatewayOrderNo,
    merchantOrderNo: data.merchantOrderNo,
    payUrl: data.payUrl,
    platformTradeNo: data.platformTradeNo,
    createdAt: formatTimestamp(new Date())
  });
  localStorage.setItem(recentOrdersKey, JSON.stringify(orders.slice(0, 8)));
  renderRecentOrders();
}

function getRecentOrders() {
  return safeJson(localStorage.getItem(recentOrdersKey), []);
}

function renderRecentOrders() {
  const box = document.querySelector('#recentOrders');
  const orders = getRecentOrders();
  if (!orders.length) {
    box.innerHTML = '<p class="empty">暂无记录</p>';
    return;
  }
  box.innerHTML = orders.map((order) => `
    <button class="recent-item" data-gateway-order-no="${order.gatewayOrderNo || ''}" data-merchant-order-no="${order.merchantOrderNo || ''}">
      <strong>${order.gatewayOrderNo || '-'}</strong>
      <span>${order.merchantOrderNo || '-'} · ${order.createdAt || '-'}</span>
    </button>
  `).join('');
  box.querySelectorAll('.recent-item').forEach((item) => {
    item.addEventListener('click', () => {
      state.queryGatewayOrderNo = item.dataset.gatewayOrderNo;
      state.queryMerchantOrderNo = item.dataset.merchantOrderNo;
      setInputValue('queryGatewayOrderNo', state.queryGatewayOrderNo);
      setInputValue('queryMerchantOrderNo', state.queryMerchantOrderNo);
    });
  });
}
