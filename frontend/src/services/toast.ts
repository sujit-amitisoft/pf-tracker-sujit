export type AppToastDetail = {
  message: string;
};

const APP_TOAST_EVENT = "app-toast";

export function appToastEventName() {
  return APP_TOAST_EVENT;
}

export function showAppToast(message: string) {
  if (!message) return;
  window.dispatchEvent(new CustomEvent<AppToastDetail>(APP_TOAST_EVENT, { detail: { message } }));
}
