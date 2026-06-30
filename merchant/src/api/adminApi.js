import { apiClient } from "./client";

const UPLOAD_TIMEOUT_MS = 120000;

export async function fetchOrders(params) {
  const { data } = await apiClient().get("/api/merchant/pay-orders", { params });
  return data;
}

export async function fetchOrderDetail(orderNo) {
  const { data } = await apiClient().get(`/api/merchant/pay-orders/${encodeURIComponent(orderNo)}`);
  return data;
}

export async function fetchOrderRefunds(orderNo) {
  const { data } = await apiClient().get(`/api/merchant/pay-orders/${encodeURIComponent(orderNo)}/refunds`);
  return data;
}

export async function createOrderRefund(orderNo, payload) {
  const { data } = await apiClient().post(
    `/api/merchant/pay-orders/${encodeURIComponent(orderNo)}/refunds`,
    payload
  );
  return data;
}

export async function fetchRefunds(params) {
  const { data } = await apiClient().get("/api/merchant/pay-refunds", { params });
  return data;
}

export async function fetchPayConfig() {
  const { data } = await apiClient().get("/api/merchant/pay-config");
  return data;
}

export async function savePayConfig(payload) {
  await apiClient().put("/api/merchant/pay-config", payload);
}

export async function fetchNotifies(params) {
  const { data } = await apiClient().get("/api/merchant/pay-notifies", { params });
  return data;
}

export async function loginAdmin(payload) {
  const { data } = await apiClient().post("/api/merchant/auth/login", payload);
  return data;
}

export async function logoutAdmin() {
  await apiClient().post("/api/merchant/auth/logout");
}

export async function fetchCurrentAdmin() {
  const { data } = await apiClient().get("/api/merchant/auth/me");
  return data;
}

export async function registerMerchant(payload) {
  const { data } = await apiClient().post("/api/merchant/auth/register", payload);
  return data;
}

export async function fetchMerchantProducts(params) {
  const { data } = await apiClient().get("/api/merchant/products", { params });
  return data;
}

export async function fetchMerchantCategories() {
  const { data } = await apiClient().get("/api/merchant/categories");
  return data;
}

export async function createMerchantProduct(payload) {
  const { data } = await apiClient().post("/api/merchant/products", payload);
  return data;
}

export async function updateMerchantProduct(id, payload) {
  await apiClient().put(`/api/merchant/products/${id}`, payload);
}

export async function uploadMerchantProductImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient().post("/api/merchant/uploads/product-image", formData, {
    timeout: UPLOAD_TIMEOUT_MS
  });
  return data;
}

export async function fetchAdminUsers(params) {
  const { data } = await apiClient().get("/api/admin/users", { params });
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

export function fetchProductOrders(params) {
  return apiClient()
    .get("/api/merchant/product-orders", { params })
    .then((res) => res.data);
}

export function fetchProductOrderDetail(orderNo) {
  return apiClient()
    .get(`/api/merchant/product-orders/${encodeURIComponent(orderNo)}`)
    .then((res) => res.data);
}

export function updateProductOrderStatus(orderNo, status) {
  return apiClient()
    .put(`/api/merchant/product-orders/${encodeURIComponent(orderNo)}/status`, { status })
    .then((res) => res.data);
}
