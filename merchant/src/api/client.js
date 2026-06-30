import axios from "axios";

const TOKEN_KEY = "mallhome_merchant_token";
const USER_KEY = "mallhome_merchant_user";
const DEFAULT_BACKEND_BASE_URL = "http://localhost:8081";

function normalizeBackendBaseUrl(value) {
  const trimmed = String(value || "").trim().replace(/\/$/, "");
  if (!trimmed) {
    return DEFAULT_BACKEND_BASE_URL;
  }
  if (/^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  return `http://${trimmed}`;
}

export const backendBaseUrl = {
  value: normalizeBackendBaseUrl(import.meta.env.VITE_API_BASE_URL)
};

export function setBackendBaseUrl(value) {
  backendBaseUrl.value = normalizeBackendBaseUrl(value);
}

export function apiClient() {
  const instance = axios.create({
    baseURL: backendBaseUrl.value,
    timeout: 15000
  });

  instance.interceptors.request.use((config) => {
    const token = getMerchantToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        clearMerchantSession();
        window.dispatchEvent(new CustomEvent("merchant-session-expired"));
      }
      return Promise.reject(error);
    }
  );

  return instance;
}

export function getMerchantToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getMerchantUser() {
  const value = localStorage.getItem(USER_KEY);
  return value ? JSON.parse(value) : null;
}

export function saveMerchantSession({ token, user }) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearMerchantSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export const getAdminToken = getMerchantToken;
export const getAdminUser = getMerchantUser;
export const saveAdminSession = saveMerchantSession;
export const clearAdminSession = clearMerchantSession;
