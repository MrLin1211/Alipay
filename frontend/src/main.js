const form = document.querySelector("#payForm");
const orderPage = document.querySelector("#orderPage");
const payResultPage = document.querySelector("#payResultPage");
const backendBaseUrlInput = document.querySelector("#backendBaseUrl");
const backendStatus = document.querySelector("#backendStatus");
const submitButton = document.querySelector("#submitButton");
const requestState = document.querySelector("#requestState");
const emptyState = document.querySelector("#emptyState");
const resultContent = document.querySelector("#resultContent");
const payUrlOutput = document.querySelector("#payUrl");
const openPayUrl = document.querySelector("#openPayUrl");
const copyPayUrl = document.querySelector("#copyPayUrl");
const rawResponse = document.querySelector("#rawResponse");
const orderNoResult = document.querySelector("#orderNoResult");
const platTradeNo = document.querySelector("#platTradeNo");
const evokeMode = document.querySelector("#evokeMode");
const localStatus = document.querySelector("#localStatus");
const resultBackendStatus = document.querySelector("#resultBackendStatus");
const payResultState = document.querySelector("#payResultState");
const payResultIcon = document.querySelector("#payResultIcon");
const payResultTitle = document.querySelector("#payResultTitle");
const payResultDesc = document.querySelector("#payResultDesc");
const resultOrderNo = document.querySelector("#resultOrderNo");
const resultStatus = document.querySelector("#resultStatus");
const resultPlatTradeNo = document.querySelector("#resultPlatTradeNo");
const resultAmount = document.querySelector("#resultAmount");
const resultUpdatedAt = document.querySelector("#resultUpdatedAt");
const resultRawResponse = document.querySelector("#resultRawResponse");
const refreshResult = document.querySelector("#refreshResult");

const DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";
const RESULT_PATH = "/pay-result";
const LAST_ORDER_NO_KEY = "frontend_last_order_no";

const orderStatusLabels = {
  CREATED: "订单已创建",
  CREATE_SUCCESS: "下单成功",
  CREATE_FAILED: "下单失败",
  SUCCESS: "交易成功",
  FINISHED: "交易结束",
  CLOSED: "交易关闭",
  UNKNOWN_NOTIFY: "未知通知状态"
};

function orderStatusText(status) {
  return orderStatusLabels[status] || status || "-";
}

function resultStateOf(status) {
  if (status === "SUCCESS") {
    return "success";
  }
  if (["CREATE_FAILED", "FINISHED", "CLOSED", "UNKNOWN_NOTIFY"].includes(status)) {
    return "failed";
  }
  return "processing";
}

function resultStateText(state) {
  if (state === "success") {
    return {
      icon: "OK",
      title: "支付成功",
      desc: "订单已确认交易成功。"
    };
  }
  if (state === "failed") {
    return {
      icon: "!",
      title: "支付失败",
      desc: "订单未完成支付，请返回重新下单或联系商户处理。"
    };
  }
  return {
    icon: "...",
    title: "支付处理中",
    desc: "如果你已经完成支付，系统会自动刷新订单状态。"
  };
}

function currentFrontendResultUrl(orderNo) {
  const url = new URL(RESULT_PATH, window.location.origin);
  if (orderNo) {
    url.searchParams.set("orderNo", orderNo);
  }
  url.searchParams.set("backend", readBackendBaseUrl());
  return url.toString();
}

function readBackendBaseUrl() {
  return (backendBaseUrlInput?.value || localStorage.getItem("frontend_backend_base_url") || DEFAULT_BACKEND_BASE_URL)
    .trim()
    .replace(/\/$/, "");
}

function setState(text, tone = "idle") {
  requestState.textContent = text;
  requestState.dataset.tone = tone;
}

function setLoading(loading) {
  submitButton.disabled = loading;
  submitButton.textContent = loading ? "创建中..." : "创建支付订单";
}

function readForm() {
  const data = new FormData(form);
  const backendBaseUrl = data.get("backendBaseUrl").trim().replace(/\/$/, "");
  localStorage.setItem("frontend_backend_base_url", backendBaseUrl);
  return {
    backendBaseUrl,
    payload: {
      totalAmount: Number(data.get("totalAmount")),
      subject: data.get("subject").trim(),
      typeIndex: Number(data.get("typeIndex")),
      goodsType: 1,
      payMethodType: data.get("payMethodType"),
      returnUrl: data.get("returnUrl").trim() || currentFrontendResultUrl("")
    }
  };
}

function renderResult(backendResult, platformResult) {
  const payData = platformResult.data?.data ?? {};
  const payUrl = backendResult.payUrl || payData.payUrl || "";

  emptyState.classList.add("hidden");
  resultContent.classList.remove("hidden");
  payUrlOutput.value = payUrl || "平台未返回 payUrl";
  openPayUrl.href = payUrl || "#";
  openPayUrl.classList.toggle("disabled", !payUrl);
  orderNoResult.textContent = backendResult.orderNo || "-";
  localStatus.textContent = orderStatusText(backendResult.status);
  platTradeNo.textContent = backendResult.platTradeNo || payData.platTradeNo || "-";
  evokeMode.textContent = payData.evokeMode ?? "-";
  rawResponse.textContent = JSON.stringify({ backendResult, platformResult }, null, 2);
}

async function createPayOrder(event) {
  event.preventDefault();

    const { backendBaseUrl, payload } = readForm();
    backendStatus.textContent = `后端：${backendBaseUrl.replace(/^https?:\/\//, "")}`;

  setLoading(true);
  setState("请求中", "loading");

  try {
    const response = await fetch(`${backendBaseUrl}/api/pay-orders`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });

    const responseText = await response.text();
    if (!response.ok) {
      throw new Error(responseText || `HTTP ${response.status}`);
    }

    const backendResult = JSON.parse(responseText);
    const platformResult = JSON.parse(backendResult.rawResponse);
    if (backendResult.orderNo) {
      localStorage.setItem(LAST_ORDER_NO_KEY, backendResult.orderNo);
    }

    if (platformResult.code !== 0) {
      throw new Error(platformResult.msg || "平台创建支付订单失败");
    }

    renderResult(backendResult, platformResult);
    setState("创建成功", "success");
  } catch (error) {
    emptyState.classList.add("hidden");
    resultContent.classList.remove("hidden");
    payUrlOutput.value = "";
    openPayUrl.href = "#";
    openPayUrl.classList.add("disabled");
    orderNoResult.textContent = "-";
    localStatus.textContent = "-";
    platTradeNo.textContent = "-";
    evokeMode.textContent = "-";
    rawResponse.textContent = error instanceof Error ? error.message : String(error);
    setState("创建失败", "error");
  } finally {
    setLoading(false);
  }
}

async function loadPayResult() {
  const params = new URLSearchParams(window.location.search);
  const orderNo = params.get("orderNo")
    || params.get("out_trade_no")
    || params.get("merchantOrderNo")
    || localStorage.getItem(LAST_ORDER_NO_KEY);
  const backendBaseUrl = (params.get("backend") || localStorage.getItem("frontend_backend_base_url") || DEFAULT_BACKEND_BASE_URL)
    .trim()
    .replace(/\/$/, "");

  resultBackendStatus.textContent = `后端：${backendBaseUrl.replace(/^https?:\/\//, "")}`;
  resultOrderNo.textContent = orderNo || "-";

  if (!orderNo) {
    renderPayResultState("failed", null, "缺少商户订单号，无法查询支付结果。");
    return;
  }

  try {
    const response = await fetch(`${backendBaseUrl}/api/pay-orders/${encodeURIComponent(orderNo)}`);
    const responseText = await response.text();
    if (!response.ok) {
      throw new Error(responseText || `HTTP ${response.status}`);
    }
    const order = JSON.parse(responseText);
    const state = resultStateOf(order.status);
    renderPayResultState(state, order);
  } catch (error) {
    renderPayResultState("processing", null, "暂时无法获取订单状态，请稍后刷新。");
    resultRawResponse.textContent = error instanceof Error ? error.message : String(error);
  }
}

function renderPayResultState(state, order, customDesc) {
  const text = resultStateText(state);
  payResultState.dataset.state = state;
  payResultIcon.textContent = text.icon;
  payResultTitle.textContent = text.title;
  payResultDesc.textContent = customDesc || text.desc;

  resultStatus.textContent = orderStatusText(order?.status);
  resultPlatTradeNo.textContent = order?.platTradeNo || "-";
  resultAmount.textContent = order?.totalAmount ? Number(order.totalAmount).toFixed(2) : "-";
  resultUpdatedAt.textContent = order?.updatedAt || "-";
  resultRawResponse.textContent = order ? JSON.stringify(order, null, 2) : resultRawResponse.textContent || "-";
}

function initOrderPage() {
  const savedBackend = localStorage.getItem("frontend_backend_base_url");
  if (savedBackend) {
    backendBaseUrlInput.value = savedBackend;
  }
  const defaultReturnUrl = currentFrontendResultUrl("");
  const returnUrlInput = document.querySelector("#returnUrl");
  if (returnUrlInput && (!returnUrlInput.value || returnUrlInput.value.includes("example.com"))) {
    returnUrlInput.value = defaultReturnUrl;
  }

  backendBaseUrlInput.addEventListener("input", () => {
    backendStatus.textContent = `后端：${backendBaseUrlInput.value.replace(/^https?:\/\//, "") || "-"}`;
  });

  copyPayUrl.addEventListener("click", async () => {
    if (!payUrlOutput.value || payUrlOutput.value === "平台未返回 payUrl") {
      return;
    }
    await navigator.clipboard.writeText(payUrlOutput.value);
    copyPayUrl.textContent = "已复制";
    setTimeout(() => {
      copyPayUrl.textContent = "复制链接";
    }, 1200);
  });

  form.addEventListener("submit", createPayOrder);
}

function initPayResultPage() {
  orderPage.classList.add("hidden");
  payResultPage.classList.remove("hidden");
  refreshResult.addEventListener("click", loadPayResult);
  loadPayResult();
  setInterval(loadPayResult, 5000);
}

if (window.location.pathname === RESULT_PATH) {
  initPayResultPage();
} else {
  initOrderPage();
}
