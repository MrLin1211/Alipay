const form = document.querySelector("#payForm");
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
  return {
    backendBaseUrl: data.get("backendBaseUrl").trim().replace(/\/$/, ""),
    payload: {
      totalAmount: Number(data.get("totalAmount")),
      subject: data.get("subject").trim(),
      typeIndex: Number(data.get("typeIndex")),
      goodsType: 1,
      payMethodType: data.get("payMethodType"),
      returnUrl: data.get("returnUrl").trim()
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
