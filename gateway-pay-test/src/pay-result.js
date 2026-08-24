import './pay-result.css';

const recentOrdersKey = 'gateway-pay-test-orders';
const terminalStatuses = new Set(['SUCCESS', 'FAILED', 'CLOSED', 'FINISHED', 'REFUNDED']);
const statusMeta = {
  SUCCESS: { tone: 'success', title: '支付成功', description: '订单已经支付成功。' },
  FAILED: { tone: 'failure', title: '支付失败', description: '订单支付失败，请返回后重新发起支付。' },
  CLOSED: { tone: 'failure', title: '支付失败', description: '订单已经关闭，请返回后重新发起支付。' },
  FINISHED: { tone: 'failure', title: '支付失败', description: '订单交易已结束，不能继续支付。' },
  REFUNDED: { tone: 'failure', title: '订单已退款', description: '该订单的支付款项已经退回。' },
  CREATED: { tone: 'processing', title: '支付处理中', description: '订单已创建，正在等待支付结果。' },
  PAYING: { tone: 'processing', title: '支付处理中', description: '支付结果尚未确认，页面会自动刷新。' },
  UNKNOWN: { tone: 'processing', title: '支付处理中', description: '暂未取得最终结果，页面会自动刷新。' }
};

const reference = resolveOrderReference();
let refreshTimer = null;
let refreshCount = 0;

document.querySelector('#app').innerHTML = `
  <div class="result-shell">
    <header class="result-header">
      <a href="/" class="brand-link">支付网关测试端</a>
      <span>支付结果</span>
    </header>
    <main class="result-main">
      <section class="result-panel" aria-live="polite">
        <div id="statusMark" class="status-mark processing">...</div>
        <p id="statusEyebrow" class="status-eyebrow">正在查询</p>
        <h1 id="statusTitle">正在确认支付结果</h1>
        <p id="statusDescription" class="status-description">请稍候，不要重复支付。</p>
        <dl id="orderDetails" class="order-details"></dl>
        <p id="updatedAt" class="updated-at"></p>
        <div class="result-actions">
          <button id="refreshButton" type="button" class="primary-button">刷新结果</button>
          <a href="/" class="secondary-button">返回测试端</a>
        </div>
      </section>
    </main>
  </div>
`;

document.querySelector('#refreshButton').addEventListener('click', () => refreshOrder(false));

if (reference.gatewayOrderNo || reference.merchantOrderNo) {
  refreshOrder(false);
} else {
  renderMessage('failure', '无法查询支付结果', '未找到本次支付的订单号，请返回测试端查询最近订单。');
}

async function refreshOrder(silent) {
  clearTimeout(refreshTimer);
  const button = document.querySelector('#refreshButton');
  if (!silent) {
    button.disabled = true;
    button.textContent = '查询中...';
  }

  try {
    const result = await postJson('/api/testpay/orders/query', reference);
    const order = result?.data?.order || result?.order || result?.data;
    if (!order?.status) {
      throw new Error('网关未返回订单状态');
    }
    renderOrder(order);
    if (!terminalStatuses.has(order.status) && refreshCount < 20) {
      refreshCount += 1;
      refreshTimer = setTimeout(() => refreshOrder(true), 3000);
    }
  } catch (error) {
    renderMessage('failure', '查询支付结果失败', error?.message || '请稍后重新查询。');
  } finally {
    button.disabled = false;
    button.textContent = '刷新结果';
  }
}

function renderOrder(order) {
  const meta = statusMeta[order.status] || statusMeta.UNKNOWN;
  const mark = document.querySelector('#statusMark');
  mark.className = `status-mark ${meta.tone}`;
  mark.textContent = meta.tone === 'success' ? 'OK' : meta.tone === 'failure' ? '!' : '...';
  document.querySelector('#statusEyebrow').textContent = meta.tone === 'processing' ? '状态确认中' : '支付结果';
  document.querySelector('#statusTitle').textContent = meta.title;
  document.querySelector('#statusDescription').textContent = meta.description;
  document.querySelector('#orderDetails').innerHTML = [
    detailItem('业务订单号', order.merchant_order_no || reference.merchantOrderNo),
    detailItem('网关订单号', order.gateway_order_no || reference.gatewayOrderNo),
    detailItem('支付金额', formatAmount(order.total_amount)),
    detailItem('订单标题', order.subject)
  ].join('');
  document.querySelector('#updatedAt').textContent = `查询时间：${formatDate(new Date())}`;
}

function renderMessage(tone, title, description) {
  const mark = document.querySelector('#statusMark');
  mark.className = `status-mark ${tone}`;
  mark.textContent = '!';
  document.querySelector('#statusEyebrow').textContent = '支付结果';
  document.querySelector('#statusTitle').textContent = title;
  document.querySelector('#statusDescription').textContent = description;
  document.querySelector('#orderDetails').innerHTML = '';
  document.querySelector('#updatedAt').textContent = '';
}

function resolveOrderReference() {
  const params = new URLSearchParams(window.location.search);
  const recentOrder = safeJson(localStorage.getItem(recentOrdersKey), [])[0] || {};
  return compactObject({
    gatewayOrderNo: params.get('gatewayOrderNo') || recentOrder.gatewayOrderNo,
    merchantOrderNo: params.get('merchantOrderNo') || recentOrder.merchantOrderNo
  });
}

async function postJson(path, body) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const text = await response.text();
  let data;
  try {
    data = JSON.parse(text);
  } catch {
    data = text;
  }
  if (!response.ok) {
    const message = typeof data === 'object' ? data.message || data.error || response.statusText : data;
    throw new Error(`HTTP ${response.status}: ${message}`);
  }
  return data;
}

function detailItem(label, value) {
  return `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value || '-')}</dd></div>`;
}

function formatAmount(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '-';
}

function formatDate(date) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  }).format(date);
}

function compactObject(object) {
  return Object.fromEntries(Object.entries(object).filter(([, value]) => value));
}

function safeJson(value, fallback) {
  if (!value) return fallback;
  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
