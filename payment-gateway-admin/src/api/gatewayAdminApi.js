import { apiClient, saveAdminSession } from "./client";

function unwrap(response) {
  const body = response.data;
  if (body?.code !== 0) {
    throw new Error(body?.message || "请求失败");
  }
  return body.data;
}

export async function loginAdmin(payload) {
  const data = unwrap(await apiClient().post("/api/gateway/admin/auth/login", payload));
  saveAdminSession(data);
  return data.user;
}

export async function fetchAlipayConfig() {
  return unwrap(await apiClient().get("/api/gateway/admin/alipay/config"));
}

export async function saveAlipayConfig(payload) {
  return unwrap(await apiClient().put("/api/gateway/admin/alipay/config", payload));
}

export async function fetchApps(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/apps", { params }));
}

export async function createApp(payload) {
  return unwrap(await apiClient().post("/api/gateway/admin/apps", payload));
}

export async function updateApp(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/apps/${id}`, payload));
}

export async function fetchOrders(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/orders", { params }));
}

export async function fetchNotifies(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/notifies", { params }));
}
