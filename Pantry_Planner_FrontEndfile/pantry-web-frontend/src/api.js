import axios from "axios";

const configuredBackendUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const authTokenKey = "pantryPlanner.authToken";

export const authExpiredEvent = "pantryPlanner:authExpired";

export const backendBaseUrl = (
  configuredBackendUrl || (import.meta.env.DEV ? "http://localhost:8080" : "")
).replace(/\/+$/, "");

export const isBackendConfigured = Boolean(backendBaseUrl);

export function backendUrl(path = "") {
  if (!isBackendConfigured) return "";
  const normalizedPath = path.replace(/^\/+/, "");
  return normalizedPath ? `${backendBaseUrl}/${normalizedPath}` : backendBaseUrl;
}

export function hasAuthToken() {
  return Boolean(sessionStorage.getItem(authTokenKey));
}

export function clearAuthToken() {
  sessionStorage.removeItem(authTokenKey);
}

const api = axios.create({
  baseURL: isBackendConfigured ? backendUrl("api") : "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 90000,
});

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(authTokenKey);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && error.config?.url !== "/auth/login") {
      clearAuthToken();
      window.dispatchEvent(new Event(authExpiredEvent));
    }
    return Promise.reject(error);
  }
);

if (import.meta.env.DEV) {
  api.interceptors.request.use((config) => {
    console.log(
      `${config.method?.toUpperCase()} ${config.baseURL}${config.url}`,
      { params: config.params || null }
    );
    return config;
  });

  api.interceptors.response.use(
    (response) => {
      console.log("Response:", response.status);
      return response;
    },
    (error) => {
      if (error.response) {
        console.error("API error:", error.response.status, error.response.data);
      } else {
        console.error("API error:", error.message);
      }
      return Promise.reject(error);
    }
  );
}

export async function login(username, password) {
  const response = await api.post("/auth/login", { username, password });
  sessionStorage.setItem(authTokenKey, response.data.token);
  return response.data;
}

export function logout() {
  clearAuthToken();
}

export async function currentUser() {
  const response = await api.get("/auth/current");
  return response.data;
}

export default api;
