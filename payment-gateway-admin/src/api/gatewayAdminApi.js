import { apiClient, saveAdminSession } from "./client";

const UPLOAD_TIMEOUT_MS = 120000;

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

export async function fetchMerchants(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/merchants", { params }));
}

export async function updateMerchant(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/merchants/${id}`, payload));
}

export async function updateMerchantApp(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/merchants/${id}/app`, payload));
}

export async function fetchMallUsers(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/mall-users", { params }));
}

export async function updateMallUser(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/mall-users/${id}`, payload));
}

export async function fetchAddresses(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/addresses", { params }));
}

export async function updateAddress(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/addresses/${id}`, payload));
}

export async function setDefaultAddress(id) {
  return unwrap(await apiClient().put(`/api/gateway/admin/addresses/${id}/default`));
}

export async function deleteAddress(id) {
  return unwrap(await apiClient().delete(`/api/gateway/admin/addresses/${id}`));
}

export async function fetchCategories(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/categories", { params }));
}

export async function createCategory(payload) {
  return unwrap(await apiClient().post("/api/gateway/admin/categories", payload));
}

export async function updateCategory(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/categories/${id}`, payload));
}

export async function fetchProducts(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/products", { params }));
}

export async function createProduct(payload) {
  return unwrap(await apiClient().post("/api/gateway/admin/products", payload));
}

export async function updateProduct(id, payload) {
  return unwrap(await apiClient().put(`/api/gateway/admin/products/${id}`, payload));
}

export async function uploadProductImage(file) {
  const formData = new FormData();
  formData.append("file", file);
  return unwrap(await apiClient().post("/api/gateway/admin/uploads/product-image", formData, {
    timeout: UPLOAD_TIMEOUT_MS
  }));
}

export async function fetchOrders(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/orders", { params }));
}

export async function fetchNotifies(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/notifies", { params }));
}

export async function fetchProductOrders(params) {
  return unwrap(await apiClient().get("/api/gateway/admin/product-orders", { params }));
}

export async function fetchProductOrderDetail(orderNo) {
  return unwrap(await apiClient().get(`/api/gateway/admin/product-orders/${encodeURIComponent(orderNo)}`));
}

export async function updateProductOrderStatus(orderNo, status) {
  return unwrap(await apiClient().put(`/api/gateway/admin/product-orders/${encodeURIComponent(orderNo)}/status`, { status }));
}
