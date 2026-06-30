import axios from "axios";

const TOKEN_KEY = "payment_gateway_admin_token";
const USER_KEY = "payment_gateway_admin_user";
const DEFAULT_GATEWAY_BASE_URL = "http://127.0.0.1:8090";

function normalizeGatewayBaseUrl(value) {
  const trimmed = String(value || "").trim().replace(/\/$/, "");
  if (!trimmed) {
    return DEFAULT_GATEWAY_BASE_URL;
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  return `http://${trimmed}`;
}

export const gatewayBaseUrl = {
  value: normalizeGatewayBaseUrl(import.meta.env.VITE_API_BASE_URL)
};

export function setGatewayBaseUrl(value) {
  gatewayBaseUrl.value = normalizeGatewayBaseUrl(value);
}

export function apiClient() {
  const instance = axios.create({
    baseURL: gatewayBaseUrl.value,
    timeout: 15000
  });

  instance.interceptors.request.use((config) => {
    const token = getAdminToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        clearAdminSession();
        window.dispatchEvent(new CustomEvent("gateway-admin-session-expired"));
      }
      if (!error.response) {
        error.message = `网络连接失败，请检查网关地址：${gatewayBaseUrl.value}`;
      }
      return Promise.reject(error);
    }
  );

  return instance;
}

export function getAdminToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getAdminUser() {
  const value = localStorage.getItem(USER_KEY);
  return value ? JSON.parse(value) : null;
}

export function saveAdminSession({ token, user }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAdminSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
