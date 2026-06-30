export const orderStatusOptions = [
  { label: "订单已创建", value: "CREATED" },
  { label: "下单成功", value: "CREATE_SUCCESS" },
  { label: "下单失败", value: "CREATE_FAILED" },
  { label: "交易成功", value: "SUCCESS" },
  { label: "交易结束", value: "FINISHED" },
  { label: "交易关闭", value: "CLOSED" },
  { label: "未知通知状态", value: "UNKNOWN_NOTIFY" }
];

export const tradeStatusOptions = [
  { label: "支付成功", value: "TRADE_SUCCESS" },
  { label: "交易结束", value: "TRADE_FINISHED" },
  { label: "交易关闭", value: "TRADE_CLOSED" }
];

export const refundStatusOptions = [
  { label: "处理中", value: "PROCESSING" },
  { label: "退款成功", value: "SUCCESS" },
  { label: "退款失败", value: "FAILED" }
];

const orderStatusLabels = {
  CREATED: "订单已创建",
  CREATE_SUCCESS: "下单成功",
  CREATE_FAILED: "下单失败",
  SUCCESS: "交易成功",
  FINISHED: "交易结束",
  CLOSED: "交易关闭",
  UNKNOWN_NOTIFY: "未知通知状态"
};

const tradeStatusLabels = {
  TRADE_SUCCESS: "支付成功",
  TRADE_FINISHED: "交易结束",
  TRADE_CLOSED: "交易关闭"
};

const refundStatusLabels = {
  PROCESSING: "处理中",
  SUCCESS: "退款成功",
  FAILED: "退款失败"
};

const notifyResultLabels = {
  SUCCESS: "处理成功",
  FAIL: "处理失败",
  FAILED: "处理失败"
};

function enumLabel(labels, value) {
  if (!value) return "-";
  return labels[value] || value;
}

export function orderStatusText(value) {
  return enumLabel(orderStatusLabels, value);
}

export function tradeStatusText(value) {
  return enumLabel(tradeStatusLabels, value);
}

export function refundStatusText(value) {
  return enumLabel(refundStatusLabels, value);
}

export function notifyResultText(value) {
  return enumLabel(notifyResultLabels, value);
}
