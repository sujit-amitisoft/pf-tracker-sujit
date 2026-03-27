export type SessionUser = {
  userId: string;
  displayName: string;
  email: string;
  accessToken: string;
  refreshToken: string;
  expiresAt?: string;
};

const SESSION_KEY = "finance_session";
const PENDING_INVITE_KEY = "pending_shared_invite";

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

export function setPendingInviteToken(token: string | null) {
  if (!token) {
    window.sessionStorage.removeItem(PENDING_INVITE_KEY);
    return;
  }
  window.sessionStorage.setItem(PENDING_INVITE_KEY, token);
}

export function getPendingInviteToken() {
  return window.sessionStorage.getItem(PENDING_INVITE_KEY);
}