import axios from "axios";

const TOKEN_KEY = "payment_gateway_admin_token";
const USER_KEY = "payment_gateway_admin_user";
const BASE_URL_KEY = "payment_gateway_admin_base_url";

export const gatewayBaseUrl = {
  value: localStorage.getItem(BASE_URL_KEY) || "http://127.0.0.1:8090"
};

export function setGatewayBaseUrl(value) {
  const nextValue = String(value || "").trim().replace(/\/$/, "");
  gatewayBaseUrl.value = nextValue || "http://127.0.0.1:8090";
  localStorage.setItem(BASE_URL_KEY, gatewayBaseUrl.value);
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
