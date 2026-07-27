import axios from "axios";


const base =
  (import.meta.env.VITE_API_BASE_URL?.replace(/\/+$/, "") ||
    "http://localhost:8080") + "/api";

const api = axios.create({
  baseURL: base,           // ⇒ http://localhost:8080/api
  withCredentials: true,   // ⬅️ required: send/receive JSESSIONID cookie
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
});

// ---------------------------------------------------------------------------
// 🧩 Environment‑based request logging (development only)
// ---------------------------------------------------------------------------
if (import.meta.env.DEV) {
  api.interceptors.request.use((config) => {
    console.log(
      `➡️  ${config.method?.toUpperCase()} ${config.baseURL}${config.url}`,
      { params: config.params || null, data: config.data || null }
    );
    return config;
  });

  api.interceptors.response.use(
    (res) => {
      console.log("⬅️  Response:", res.status, res.data);
      return res;
    },
    (err) => {
      if (err.response)
        console.error("❌  API Error:", err.response.status, err.response.data);
      else console.error("❌  API Error:", err.message);
      return Promise.reject(err);
    }
  );
}


export async function login(username, password) {
  const form = new URLSearchParams();
  form.append("username", username);
  form.append("password", password);

  // Important: override header for form data
  return api.post("/auth/login", form, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
  });
}

/**
 * Logout current user — invalidates JSESSIONID on the server
 */
export async function logout() {
  return api.post("/auth/logout");
}

/**
 * Fetch currently authenticated user info.
 */
export async function currentUser() {
  return api.get("/auth/me");
}

export default api;