import { apiClient } from "./client";

export async function fetchOrders(params) {
  const { data } = await apiClient().get("/api/admin/pay-orders", { params });
  return data;
}

export async function fetchOrderDetail(orderNo) {
  const { data } = await apiClient().get(`/api/admin/pay-orders/${encodeURIComponent(orderNo)}`);
  return data;
}

export async function fetchOrderRefunds(orderNo) {
  const { data } = await apiClient().get(`/api/admin/pay-orders/${encodeURIComponent(orderNo)}/refunds`);
  return data;
}

export async function createOrderRefund(orderNo, payload) {
  const { data } = await apiClient().post(
    `/api/admin/pay-orders/${encodeURIComponent(orderNo)}/refunds`,
    payload
  );
  return data;
}

export async function fetchRefunds(params) {
  const { data } = await apiClient().get("/api/admin/pay-refunds", { params });
  return data;
}

export async function fetchPayConfig() {
  const { data } = await apiClient().get("/api/admin/pay-config");
  return data;
}

export async function savePayConfig(payload) {
  await apiClient().put("/api/admin/pay-config", payload);
}

export async function fetchNotifies(params) {
  const { data } = await apiClient().get("/api/admin/pay-notifies", { params });
  return data;
}

export async function loginAdmin(payload) {
  const { data } = await apiClient().post("/api/admin/auth/login", payload);
  return data;
}

export async function logoutAdmin() {
  await apiClient().post("/api/admin/auth/logout");
}

export async function fetchCurrentAdmin() {
  const { data } = await apiClient().get("/api/admin/auth/me");
  return data;
}

export async function fetchAdminUsers() {
  const { data } = await apiClient().get("/api/admin/users");
  return data;
}

export async function createAdminUser(payload) {
  const { data } = await apiClient().post("/api/admin/users", payload);
  return data;
}

export async function updateAdminUser(id, payload) {
  const { data } = await apiClient().patch(`/api/admin/users/${id}`, payload);
  return data;
}

export async function deleteAdminUser(id) {
  await apiClient().delete(`/api/admin/users/${id}`);
}
