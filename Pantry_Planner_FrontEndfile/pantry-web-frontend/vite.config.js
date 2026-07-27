import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,

    // 👇 Forward all /api requests to the Spring Boot backend
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },

    // ⚙️  Vite handles SPA refresh automatically.
    // You don't need historyApiFallback; Vite serves index.html by default,
    // but you can enforce it with:
    fs: {
      strict: false,
    },
  },
});