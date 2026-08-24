import { apiClient, saveAdminSession } from "./client";

function unwrap(response) {
  const body = response.data;
  if (body?.code !== 0) {
    throw new Error(body?.message || "请求失败");
  }
  return body.data;
}

export async function loginClient(payload) {
  const data = unwrap(await apiClient().post("/api/gateway/client/auth/login", payload));
  saveAdminSession(data);
  return data.user;
}

export async function fetchProfile() {
  return unwrap(await apiClient().get("/api/gateway/client/profile"));
}

export async function fetchAppConfig() {
  return unwrap(await apiClient().get("/api/gateway/client/app"));
}

export async function saveAppConfig(payload) {
  return unwrap(await apiClient().put("/api/gateway/client/app", payload));
}

export async function resetAppSecret() {
  return unwrap(await apiClient().post("/api/gateway/client/app/secret/reset"));
}

export async function fetchOrders(params) {
  return unwrap(await apiClient().get("/api/gateway/client/orders", { params }));
}

export async function createRefund(payload) {
  return unwrap(await apiClient().post("/api/gateway/client/refunds", payload));
}

export async function fetchRefunds(params) {
  return unwrap(await apiClient().get("/api/gateway/client/refunds", { params }));
}

export async function fetchNotifies(params) {
  return unwrap(await apiClient().get("/api/gateway/client/notifies", { params }));
}
