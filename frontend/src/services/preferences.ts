export type Preferences = {
  theme: "dark" | "light" | "amoled";
  accent: "teal" | "amber" | "rose" | "sky" | "violet" | "emerald";
  emailNotifications: boolean;
  budgetAlerts: boolean;
  recurringAlerts: boolean;
};

const KEY = "finance_preferences";
const EVENT = "finance-preferences-changed";

const defaults: Preferences = {
  theme: "light",
  accent: "sky",
  emailNotifications: false,
  budgetAlerts: true,
  recurringAlerts: true,
};

export function getPreferences(): Preferences {
  const raw = window.localStorage.getItem(KEY);
  return raw ? { ...defaults, ...(JSON.parse(raw) as Partial<Preferences>) } : defaults;
}

export function setPreferences(next: Preferences) {
  window.localStorage.setItem(KEY, JSON.stringify(next));
  window.dispatchEvent(new Event(EVENT));
}

export function preferenceEventName() {
  return EVENT;
}
