export const orderStatusOptions = [
  { value: "CREATED", label: "已创建" },
  { value: "PAYING", label: "待支付" },
  { value: "SUCCESS", label: "交易成功" },
  { value: "REFUNDED", label: "已退款" },
  { value: "FINISHED", label: "交易结束" },
  { value: "CLOSED", label: "交易关闭" },
  { value: "FAILED", label: "失败" },
  { value: "UNKNOWN", label: "未知" }
];

export const tradeStatusOptions = [
  { value: "TRADE_SUCCESS", label: "支付成功" },
  { value: "WAIT_BUYER_PAY", label: "待支付" },
  { value: "TRADE_REFUND_SUCCESS", label: "退款成功" },
  { value: "TRADE_FINISHED", label: "交易结束" },
  { value: "TRADE_CLOSED", label: "交易关闭" }
];

export const notifyResultOptions = [
  { value: "SUCCESS", label: "处理成功" },
  { value: "FAIL", label: "处理失败" }
];

export const refundStatusOptions = [
  { value: "CREATED", label: "已创建" },
  { value: "PROCESSING", label: "处理中" },
  { value: "SUCCESS", label: "退款成功" },
  { value: "FAILED", label: "退款失败" }
];

export function labelOf(options, value) {
  return options.find((item) => item.value === value)?.label || value || "-";
}

export function orderTagType(status) {
  if (["SUCCESS", "REFUNDED"].includes(status)) return "success";
  if (["FINISHED", "CLOSED", "FAILED"].includes(status)) return "danger";
  if (status === "PAYING") return "warning";
  return "info";
}

export function resultTagType(result) {
  return result === "SUCCESS" ? "success" : "danger";
}

export function refundTagType(status) {
  if (status === "SUCCESS") return "success";
  if (status === "FAILED") return "danger";
  if (status === "PROCESSING") return "warning";
  return "info";
}
