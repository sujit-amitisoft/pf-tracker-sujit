export type SessionUser = {
  userId: string;
  displayName: string;
  email: string;
  accessToken: string;
  refreshToken: string;
  expiresAt?: string;
};

const SESSION_KEY = "finance_session";

export function getSession(): SessionUser | null {
  const raw = window.localStorage.getItem(SESSION_KEY);
  return raw ? (JSON.parse(raw) as SessionUser) : null;
}

export function setSession(session: SessionUser) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_KEY);
}

export function isAuthenticated() {
  return !!getSession()?.accessToken;
}
