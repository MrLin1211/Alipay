import axios from "axios";

const TOKEN_KEY = "mallhome_admin_token";
const USER_KEY = "mallhome_admin_user";

export const backendBaseUrl = {
  value: "http://localhost:8080"
};

export function setBackendBaseUrl(value) {
  backendBaseUrl.value = value.replace(/\/$/, "");
}

export function apiClient() {
  const instance = axios.create({
    baseURL: backendBaseUrl.value,
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
        window.dispatchEvent(new CustomEvent("admin-session-expired"));
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
