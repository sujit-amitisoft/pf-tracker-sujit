import type { FormEvent } from "react";
import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { api } from "../../services/api";
import { resolveApiError } from "../../services/apiErrors";
import { getPendingInviteToken, setPendingInviteToken, setSession } from "../../services/session";
import { getPreferences, setPreferences, type Preferences } from "../../services/preferences";

type AuthResponse = {
  userId: string;
  displayName: string;
  email: string;
  accessToken: string;
  refreshToken: string;
};

type AuthMode = "login" | "register" | "forgot" | "reset";

type PasswordChecks = {
  minLength: boolean;
  uppercase: boolean;
  lowercase: boolean;
  number: boolean;
};

function validatePassword(password: string): PasswordChecks {
  return {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    number: /\d/.test(password),
  };
}

export function AuthPage() {
  const [searchParams] = useSearchParams();
  const [mode, setMode] = useState<AuthMode>("login");
  const [theme, setTheme] = useState<Preferences["theme"]>(() => getPreferences().theme);
  const [showPassword, setShowPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [form, setForm] = useState({ displayName: "", email: "", password: "", resetToken: "", newPassword: "" });
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [forgotEmailSent, setForgotEmailSent] = useState(false);
  const resetTokenFromUrl = (searchParams.get("token") ?? "").trim();
  const modeFromUrl = (searchParams.get("mode") ?? "").trim();
  const inviteTokenFromUrl = modeFromUrl === "invite" ? resetTokenFromUrl : "";

  useEffect(() => {
    if (modeFromUrl === "reset" && resetTokenFromUrl) {
      setMode("reset");
      setForm((current) => ({ ...current, resetToken: resetTokenFromUrl }));
      setForgotEmailSent(false);
      setError(null);
      setMessage(null);
      return;
    }
    if (modeFromUrl === "invite" && resetTokenFromUrl) {
      setPendingInviteToken(resetTokenFromUrl);
      setMode("login");
      setForgotEmailSent(false);
      setError(null);
      setMessage("Log in or create an account with the invited email address to accept the shared account invitation.");
    }
  }, [modeFromUrl, resetTokenFromUrl]);

  const updateField = (field: keyof typeof form, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    if (error) setError(null);
  };

  const switchMode = (nextMode: AuthMode) => {
    setMode(nextMode);
    setError(null);
    setMessage(null);
    setForgotEmailSent(false);
    setShowPassword(false);
    setShowNewPassword(false);
    if (nextMode !== "reset") {
      setForm((current) => ({ ...current, resetToken: resetTokenFromUrl || current.resetToken, newPassword: nextMode === "forgot" ? "" : current.newPassword }));
    }
  };

  const afterAuthPath = inviteTokenFromUrl || getPendingInviteToken();
  const passwordValue = mode === "reset" ? form.newPassword : form.password;
  const passwordChecks = useMemo(() => validatePassword(passwordValue), [passwordValue]);
  const shouldShowPasswordHelper = (mode === "register" || mode === "reset") && passwordValue.length > 0;
  const allPasswordChecksPassed = Object.values(passwordChecks).every(Boolean);
  const passwordHelperClass = allPasswordChecksPassed ? "signup-hint success" : "signup-hint error";
  const passwordHelperText = allPasswordChecksPassed
    ? "Password looks good."
    : "Password must be at least 8 characters with uppercase, lowercase, and number.";

  const submit = async () => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      if (mode === "forgot") {
        const { data } = await api.post<{ message: string }>("/api/auth/forgot-password", { email: form.email });
        setMessage(data.message);
        setForgotEmailSent(true);
      } else if (mode === "reset") {
        const { data } = await api.post<{ message: string }>("/api/auth/reset-password", { token: form.resetToken, newPassword: form.newPassword });
        setMessage(data.message);
        setForgotEmailSent(false);
        switchMode("login");
      } else {
        const credentials = { email: form.email, password: form.password };
        const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
        const payload = mode === "login"
          ? credentials
          : { ...credentials, displayName: form.displayName };
        const { data } = await api.post<Partial<AuthResponse>>(endpoint, payload);
        const hasSession = Boolean(data?.accessToken && data?.refreshToken);
        const session = hasSession ? (data as AuthResponse) : (await api.post<AuthResponse>("/api/auth/login", credentials)).data;
        setSession(session);
        const destination = afterAuthPath
          ? `/shared-accounts?invite=${encodeURIComponent(afterAuthPath)}`
          : mode === "register"
            ? "/onboarding"
            : "/";
        window.location.replace(destination);
      }
    } catch (err: any) {
      setError(resolveApiError(err, "Authentication failed"));
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
        : "Set a new password using the secure reset link from your email.";

  return (
    <div className="auth-screen-clean" data-theme={theme === "dark" && getPreferences().amoledDark ? "amoled" : theme}>
      <div className="auth-clean-shell auth-box-shell">
        <section className={mode === "register" ? "auth-clean-hero auth-box-panel auth-box-panel-signup" : "auth-clean-hero auth-box-panel"}>
          <div key={mode === "register" ? "signup-title" : mode === "login" ? "login-title" : "auth-title"} className="auth-clean-badge auth-clean-badge-plain auth-type-title">Personal Finance Tracker</div>
          <div className="auth-clean-hero-box">
            <h1>{mode === "login" ? "Track money clearly" : "Start Your Journey"}</h1>
            <p>{mode === "login" ? "Don't have an account?" : "Already have an account?"}</p>
            <button type="button" className="button auth-hero-button" onClick={() => switchMode(mode === "login" ? "register" : "login")}>
              {mode === "login" ? "Register" : "Login"}
            </button>
            <div className="auth-theme-row">
              <button type="button" className="button ghost" onClick={() => { setTheme("light"); setPreferences({ ...getPreferences(), theme: "light" }); }}>Light</button>
              <button type="button" className="button ghost" onClick={() => { setTheme("dark"); setPreferences({ ...getPreferences(), theme: "dark" }); }}>Dark</button>
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
            {mode === "login" || mode === "register" ? (
              <div className="auth-password-field">
                <input className={mode === "register" ? "signup-input" : undefined} placeholder="Password" type={showPassword ? "text" : "password"} value={form.password} onChange={(e) => updateField("password", e.target.value)} />
                <button className="auth-password-toggle" type="button" onClick={() => setShowPassword((current) => !current)}>{showPassword ? "Hide" : "Show"}</button>
              </div>
            ) : null}
            {mode === "reset" ? (
              <div className="auth-reset-stack">
                <div className="auth-confirmation-card auth-confirmation-card-reset">
                  <strong>Password reset</strong>
                  <p>Enter your new password below. This screen is available only through the secure link sent to your email.</p>
                </div>
                <div className="auth-password-field">
                  <input placeholder="New password" type={showNewPassword ? "text" : "password"} value={form.newPassword} onChange={(e) => updateField("newPassword", e.target.value)} />
                  <button className="auth-password-toggle" type="button" onClick={() => setShowNewPassword((current) => !current)}>{showNewPassword ? "Hide" : "Show"}</button>
                </div>
              </div>
            ) : null}
            {mode === "forgot" && forgotEmailSent ? (
              <div className="auth-confirmation-card">
                <strong>Check your email</strong>
                <p>{message ?? "If an account exists for that email, we sent a password reset link."}</p>
                <div className="auth-link-grid auth-confirmation-actions">
                  <button type="button" className="auth-switch-link" onClick={() => setForgotEmailSent(false)}>Send again</button>
                  <button type="button" className="auth-switch-link" onClick={() => switchMode("login")}>Back to Log In</button>
                </div>
              </div>
            ) : null}
            {shouldShowPasswordHelper ? <p className={passwordHelperClass}>{passwordHelperText}</p> : null}
            {message && !(mode === "forgot" && forgotEmailSent) ? <p className="form-message auth-feedback-success">{message}</p> : null}
            {error ? <p className="form-error auth-feedback-error">{error}</p> : null}
            {!(mode === "forgot" && forgotEmailSent) ? (
              <button type="submit" className={mode === "register" ? "button primary auth-submit auth-submit-strong auth-submit-signup" : "button primary auth-submit auth-submit-strong"} disabled={loading}>
                {loading ? "Working..." : mode === "login" ? "Log In" : mode === "register" ? "Create Account" : mode === "forgot" ? "Send Reset Link" : "Reset Password"}
              </button>
            ) : null}
            <div className="auth-link-grid">
              {mode === "login" ? <button type="button" className="auth-switch-link" onClick={() => switchMode("forgot")}>Forgot password?</button> : null}
              {mode === "login" ? <button type="button" className="auth-switch-link" onClick={() => switchMode("register")}>Don't have an account? Sign up</button> : null}
              {mode !== "login" && !(mode === "forgot" && forgotEmailSent) ? <button type="button" className="auth-switch-link" onClick={() => switchMode("login")}>Back to Log In</button> : null}
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}
