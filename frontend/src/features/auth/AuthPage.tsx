import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../../services/api";
import { setSession } from "../../services/session";
import { getPreferences, setPreferences, type Preferences } from "../../services/preferences";

type AuthResponse = {
  userId: string;
  displayName: string;
  email: string;
  accessToken: string;
  refreshToken: string;
};

type AuthMode = "login" | "register" | "forgot" | "reset";

export function AuthPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [mode, setMode] = useState<AuthMode>("login");
  const [theme, setTheme] = useState<Preferences["theme"]>(() => getPreferences().theme);
  const [form, setForm] = useState({ displayName: "", email: "", password: "", resetToken: "", newPassword: "" });
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const resetTokenFromUrl = (searchParams.get("token") ?? "").trim();
  const modeFromUrl = (searchParams.get("mode") ?? "").trim();

  useEffect(() => {
    if (modeFromUrl !== "reset" || !resetTokenFromUrl) return;
    setMode("reset");
    setForm((current) => ({ ...current, resetToken: resetTokenFromUrl }));
    setError(null);
    setMessage(null);
  }, [modeFromUrl, resetTokenFromUrl]);
  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    if (error) {
      setError(null);
    }
  };

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setError(null);
    setMessage(null);
  };

  const submit = async () => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      if (mode === "forgot") {
        const { data } = await api.post<{ message: string }>("/api/auth/forgot-password", { email: form.email });
        setMessage(data.message);
        switchMode("reset");
      } else if (mode === "reset") {
        const { data } = await api.post<{ message: string }>("/api/auth/reset-password", { token: form.resetToken, newPassword: form.newPassword });
        setMessage(data.message);
        switchMode("login");
      } else {
        const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
        const payload = mode === "login"
          ? { email: form.email, password: form.password }
          : { email: form.email, password: form.password, displayName: form.displayName };
        const { data } = await api.post<AuthResponse>(endpoint, payload);
        setSession(data);
        navigate(mode === "register" ? "/onboarding" : "/", { replace: true });
      }
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    await submit();
  };

  const subtitle = mode === "login"
    ? "Log in to access your dashboard, transactions, budgets, goals, and reports."
    : mode === "register"
      ? "Create your personal finance account to start tracking income, expenses, and savings goals."
      : mode === "forgot"
        ? "Enter your email and we will send a password reset link if the account exists."
        : "Set a new password using the secure reset link token.";

  return (
    <div className="auth-screen-clean" data-theme={theme}>
      <div className="auth-clean-shell auth-box-shell">
        <section className={mode === "register" ? "auth-clean-hero auth-box-panel auth-box-panel-signup" : "auth-clean-hero auth-box-panel"}>
          <div key={mode === "register" ? "signup-title" : mode === "login" ? "login-title" : "auth-title"} className="auth-clean-badge auth-clean-badge-plain auth-type-title">Personal Finance Tracker</div>
          <div className="auth-clean-hero-box">
            <h1>{mode === "login" ? "Hello, Welcome!" : "Start Your Journey"}</h1>
            <p>{mode === "login" ? "Don't have an account?" : "Already have an account?"}</p>
            <button type="button" className="button auth-hero-button" onClick={() => switchMode(mode === "login" ? "register" : "login")}>
              {mode === "login" ? "Register" : "Login"}
            </button>
            <div className="auth-theme-row">
              <button type="button" className="button ghost" onClick={() => { setTheme("light"); setPreferences({ ...getPreferences(), theme: "light" }); }}>Light</button>
              <button type="button" className="button ghost" onClick={() => { setTheme("dark"); setPreferences({ ...getPreferences(), theme: "dark" }); }}>Dark</button>
              <button type="button" className="button ghost" onClick={() => { setTheme("amoled"); setPreferences({ ...getPreferences(), theme: "amoled" }); }}>AMOLED</button>
            </div>
          </div>
        </section>

        <section className={mode === "register" ? "auth-clean-form auth-box-form signup-form-surface" : "auth-clean-form auth-box-form"}>
          <div className="auth-form-header">
            <h2>{mode === "login" ? "Login" : mode === "register" ? "Sign Up" : mode === "forgot" ? "Forgot Password" : "Reset Password"}</h2>
            <p>{subtitle}</p>
          </div>

          <form className="form-grid auth-form-grid clean-form-grid" onSubmit={handleSubmit}>
            {mode === "register" ? <input className="signup-input" placeholder="Display name" value={form.displayName} onChange={(e) => updateField("displayName", e.target.value)} /> : null}
            {mode !== "reset" ? <input className={mode === "register" ? "signup-input" : undefined} placeholder="Email" value={form.email} onChange={(e) => updateField("email", e.target.value)} /> : null}
            {mode === "login" || mode === "register" ? <input className={mode === "register" ? "signup-input" : undefined} placeholder="Password" type="password" value={form.password} onChange={(e) => updateField("password", e.target.value)} /> : null}
            {mode === "reset" ? (
              <>
                {resetTokenFromUrl ? null : <input placeholder="Reset token" value={form.resetToken} onChange={(e) => updateField("resetToken", e.target.value)} />}
                <input placeholder="New password" type="password" value={form.newPassword} onChange={(e) => updateField("newPassword", e.target.value)} />
              </>
            ) : null}
            {(mode === "register" || mode === "reset") ? <p className="settings-hint signup-hint">Password must be at least 8 characters with uppercase, lowercase, and number.</p> : null}
            {message ? <p className="form-message">{message}</p> : null}
            {error ? <p className="form-error">{error}</p> : null}
            <button type="submit" className={mode === "register" ? "button primary auth-submit auth-submit-strong auth-submit-signup" : "button primary auth-submit auth-submit-strong"} disabled={loading}>
              {loading ? "Working..." : mode === "login" ? "Log In" : mode === "register" ? "Create Account" : mode === "forgot" ? "Send Reset Link" : "Reset Password"}
            </button>
            <div className="auth-link-grid">
              {mode === "login" ? <button type="button" className="auth-switch-link" onClick={() => switchMode("forgot")}>Forgot password?</button> : null}
              {mode === "login" ? <button type="button" className="auth-switch-link" onClick={() => switchMode("register")}>Don't have an account? Sign up</button> : null}
              {mode !== "login" ? <button type="button" className="auth-switch-link" onClick={() => switchMode("login")}>Back to Log In</button> : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}






