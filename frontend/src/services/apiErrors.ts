import { showAppToast } from "./toast";

const DEFAULT_PERMISSION_MESSAGE = "You do not have permission to modify this account";

export function resolveApiError(err: any, fallbackMessage: string) {
  const details = err?.response?.data?.details;
  if (Array.isArray(details) && details.length) {
    return details[0];
  }
  return err?.response?.data?.message ?? fallbackMessage;
}

export function handlePermissionDenied(err: any, setError: (message: string) => void, fallbackMessage = DEFAULT_PERMISSION_MESSAGE) {
  const message = resolveApiError(err, fallbackMessage);
  setError(message);
  showAppToast(message);
}

export function isPermissionDeniedMessage(message: string | null | undefined) {
  if (!message) return false;
  return message.toLowerCase().includes("do not have permission");
}
