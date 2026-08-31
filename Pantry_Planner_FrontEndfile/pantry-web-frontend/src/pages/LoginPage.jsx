import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { login } from "../api";
import Logo from "../components/Logo";

function loginError(error) {
  if (error.code === "ECONNABORTED") {
    return {
      message: "The free API took too long to wake up. Please try again.",
      invalidCredentials: false,
    };
  }
  if (!error.response) {
    return {
      message: "The free API is waking up or unavailable. Please try again in a minute.",
      invalidCredentials: false,
    };
  }
  if (error.response.status === 401) {
    return {
      message: "The username or password is incorrect.",
      invalidCredentials: true,
    };
  }
  if (error.response.status === 400) {
    return {
      message: "Enter a valid username and password.",
      invalidCredentials: true,
    };
  }
  if (error.response.status === 429) {
    return {
      message: "Too many login attempts. Please wait a moment and try again.",
      invalidCredentials: false,
    };
  }
  return {
    message: "Login failed. Please try again.",
    invalidCredentials: false,
  };
}

export default function LoginPage({ backendConfigured, initialError, onLogin }) {
  const location = useLocation();
  const navigate = useNavigate();
  const notice = location.state?.notice;
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(
    initialError && !notice
      ? { message: initialError, invalidCredentials: false }
      : null
  );
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setError(
      initialError && !notice
        ? { message: initialError, invalidCredentials: false }
        : null
    );
  }, [initialError, notice]);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      const user = await login(username.trim(), password);
      onLogin(user);
      navigate("/suggestions", { replace: true });
    } catch (requestError) {
      setError(loginError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-[calc(100dvh-4rem)] items-center justify-center bg-slate-50 px-4 py-10">
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

        {notice && (
          <p
            className="mt-5 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800"
            role="status"
          >
            {notice}
          </p>
        )}

        <form className="mt-7 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700" htmlFor="username">
              Username
            </label>
            <input
              id="username"
              autoComplete="username"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus-visible:border-emerald-700 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-1"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              aria-invalid={error?.invalidCredentials || undefined}
              aria-describedby={error ? "login-error" : undefined}
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
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus-visible:border-emerald-700 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-1"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={error?.invalidCredentials || undefined}
              aria-describedby={error ? "login-error" : undefined}
              required
              maxLength={72}
            />
          </div>

          {error && (
            <p
              id="login-error"
              className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700"
              role="alert"
            >
              {error.message}
            </p>
          )}

          <button
            className="w-full rounded-lg bg-emerald-700 px-4 py-2.5 font-semibold text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60"
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

        <p className="mt-6 text-center text-sm text-slate-600">
          New to Pantry Planner?{" "}
          <Link
            className="font-semibold text-emerald-700 hover:text-emerald-800 hover:underline"
            to="/signup"
          >
            Create an account
          </Link>
        </p>
      </section>
    </main>
  );
}
