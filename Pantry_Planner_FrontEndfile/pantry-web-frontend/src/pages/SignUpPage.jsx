import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register } from "../api";
import Logo from "../components/Logo";
import { USERNAME_PATTERN } from "../lib/authValidation";

function signUpError(error) {
  if (error.code === "ECONNABORTED") {
    return {
      message: "The request timed out. Your account may already be ready, so try logging in with these details before signing up again.",
      field: null,
    };
  }
  if (!error.response) {
    return {
      message: "The free API is waking up or unavailable. Please try again in a minute.",
      field: null,
    };
  }
  if (error.response.status === 409) {
    return {
      message: "That username is already taken. Try another one.",
      field: "username",
    };
  }
  if (error.response.status === 400) {
    return {
      message: "Use 3 to 50 letters, numbers, dots, dashes, or underscores for your username and at least 8 characters for your password.",
      field: null,
    };
  }
  if (error.response.status === 401 || error.response.status === 403) {
    return {
      message: "Sign up is not available right now. Please try again later.",
      field: null,
    };
  }
  if (error.response.status === 429) {
    return {
      message: "Too many sign-up attempts. Please wait and try again later.",
      field: null,
    };
  }
  return { message: "Account creation failed. Please try again.", field: null };
}

export default function SignUpPage({ backendConfigured }) {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError(null);

    if (password !== confirmation) {
      setError({ message: "The passwords do not match.", field: "password" });
      return;
    }

    setSubmitting(true);
    try {
      await register(username.trim(), password);
      navigate("/login", {
        replace: true,
        state: { notice: "Your account is ready. Log in to start planning." },
      });
    } catch (requestError) {
      setError(signUpError(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-[calc(100dvh-4rem)] items-center justify-center bg-slate-50 px-4 py-10">
      <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-lg sm:p-9">
        <div className="mb-7 flex justify-center">
          <Logo size={54} label="Pantry Planner" />
        </div>
        <h1 className="text-center text-2xl font-semibold text-slate-900">
          Create your account
        </h1>
        <p className="mt-2 text-center text-sm text-slate-600">
          Start finding meals that fit what is already in your kitchen.
        </p>

        <form className="mt-7 space-y-5" onSubmit={handleSubmit}>
          <div>
            <label
              className="mb-1.5 block text-sm font-medium text-slate-700"
              htmlFor="signup-username"
            >
              Username
            </label>
            <input
              id="signup-username"
              autoComplete="username"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus-visible:border-emerald-700 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-1"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              aria-invalid={error?.field === "username" || undefined}
              aria-describedby={error ? "signup-error" : undefined}
              minLength={3}
              maxLength={50}
              pattern={USERNAME_PATTERN}
              title="Use letters, numbers, dots, dashes, or underscores."
              required
            />
          </div>

          <div>
            <label
              className="mb-1.5 block text-sm font-medium text-slate-700"
              htmlFor="signup-password"
            >
              Password
            </label>
            <input
              id="signup-password"
              type="password"
              autoComplete="new-password"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus-visible:border-emerald-700 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-1"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              aria-invalid={error?.field === "password" || undefined}
              aria-describedby={`signup-password-hint${
                error ? " signup-error" : ""
              }`}
              minLength={8}
              maxLength={72}
              required
            />
            <p id="signup-password-hint" className="mt-1.5 text-xs text-slate-500">
              Use at least 8 characters.
            </p>
          </div>

          <div>
            <label
              className="mb-1.5 block text-sm font-medium text-slate-700"
              htmlFor="signup-confirmation"
            >
              Confirm password
            </label>
            <input
              id="signup-confirmation"
              type="password"
              autoComplete="new-password"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-slate-900 outline-none transition focus-visible:border-emerald-700 focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-1"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              aria-invalid={error?.field === "password" || undefined}
              aria-describedby={error ? "signup-error" : undefined}
              minLength={8}
              maxLength={72}
              required
            />
          </div>

          {error && (
            <p
              id="signup-error"
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
            {submitting ? "Creating account..." : "Create account"}
          </button>
        </form>

        {!backendConfigured && (
          <p className="mt-5 text-center text-sm text-slate-600">
            Set VITE_API_BASE_URL to the HTTPS URL of the deployed backend.
          </p>
        )}

        <p className="mt-6 text-center text-sm text-slate-600">
          Already have an account?{" "}
          <Link
            className="font-semibold text-emerald-700 hover:text-emerald-800 hover:underline"
            to="/login"
          >
            Log in
          </Link>
        </p>
      </section>
    </main>
  );
}
