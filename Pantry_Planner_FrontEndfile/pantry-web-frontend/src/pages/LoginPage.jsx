import React, { useState } from "react";
import { login, getCurrentUser } from "../services/authService";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [user, setUser] = useState(null);
  const [error, setError] = useState("");

  async function handleLogin(e) {
    e.preventDefault();
    try {
      await login(username, password);
      const u = await getCurrentUser();
      setUser(u);
      setError("");
    } catch (err) {
      console.error(err);
      setError("Login failed");
    }
  }

  return (
    <div style={{ padding: 30 }}>
      <h2>Login</h2>
      <form onSubmit={handleLogin}>
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Username"
        />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
        />
        <button type="submit">Login</button>
      </form>

      {error && <p style={{ color: "red" }}>{error}</p>}
      {user && (
        <p>
          Logged in as <strong>{user.username}</strong> ({user.role})
        </p>
      )}
    </div>
  );
}