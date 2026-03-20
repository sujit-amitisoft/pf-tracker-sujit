import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { clearSession, getSession, setSession, type SessionUser } from "./session";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const api = axios.create({
  baseURL,
});

const refreshClient = axios.create({
  baseURL,
});

api.interceptors.request.use((config) => {
  const session = getSession();
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry || originalRequest.url?.includes("/api/auth/refresh")) {
      return Promise.reject(error);
    }

    const session = getSession();
    if (!session?.refreshToken) {
      clearSession();
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      const { data } = await refreshClient.post<SessionUser>("/api/auth/refresh", { refreshToken: session.refreshToken });
      setSession(data);
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      clearSession();
      if (window.location.pathname !== "/auth") {
        window.location.href = "/auth";
      }
      return Promise.reject(refreshError);
    }
  },
);
