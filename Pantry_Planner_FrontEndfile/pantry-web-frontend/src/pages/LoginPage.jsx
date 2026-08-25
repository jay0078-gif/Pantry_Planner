import { useEffect, useState } from "react";
import { login } from "../api";
import Logo from "../components/Logo";

function loginError(error) {
  if (error.code === "ECONNABORTED") {
    return "The free API took too long to wake up. Please try again.";
  }
  if (!error.response) {
    return "The free API is waking up or unavailable. Please try again in a minute.";
  }
  if (error.response.status === 401) {
    return "The username or password is incorrect.";
  }
  if (error.response.status === 400) {
    return "Enter a valid username and password.";
  }
  return "Login failed. Please try again.";
}

export default function LoginPage({ backendConfigured, initialError, onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(initialError);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(initialError);
  }, [initialError]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      const user = await login(username.trim(), password);
      onLogin(user);
    } catch (requestError) {
      setError(loginError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-10">
      <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-lg sm:p-9">
        <div className="mb-8 flex justify-center">
          <Logo size={54} label="Pantry Planner" />
        </div>
        <h1 className="text-center text-2xl font-semibold text-slate-900">
          Welcome back
        </h1>
        <p className="mt-2 text-center text-sm text-slate-600">
          Sign in to plan recipes from what is already in your pantry.
        </p>

        <form className="mt-7 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="username">
              Username
            </label>
            <input
              id="username"
              autoComplete="username"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              required
              maxLength={50}
            />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              maxLength={72}
            />
          </div>

          {error && (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {error}
            </p>
          )}

          <button
            className="w-full rounded-lg bg-emerald-600 px-4 py-2.5 font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
            type="submit"
            disabled={!backendConfigured || submitting}
          >
            {submitting ? "Connecting..." : "Log in"}
          </button>
        </form>

        {!backendConfigured && (
          <p className="mt-5 text-center text-sm text-slate-600">
            Set VITE_API_BASE_URL to the HTTPS URL of the deployed backend.
          </p>
        )}
      </section>
    </main>
  );
}
