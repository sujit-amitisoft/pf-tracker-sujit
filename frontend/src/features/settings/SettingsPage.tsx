import { useEffect, useState } from "react";
import { PageSection } from "../../components/PageBits";
import { getPreferences, setPreferences, type Preferences } from "../../services/preferences";

const accents: Array<{ value: Preferences["accent"]; label: string; hint: string }> = [
  { value: "teal", label: "Teal Aurora", hint: "Fresh cyan and lime" },
  { value: "amber", label: "Amber Glow", hint: "Warm gold and coral" },
  { value: "rose", label: "Rose Quartz", hint: "Soft pink and violet" },
  { value: "sky", label: "Sky Pulse", hint: "Blue and mint" },
  { value: "violet", label: "Violet Edge", hint: "Indigo and lavender" },
  { value: "emerald", label: "Emerald Ink", hint: "Blue-green finance contrast" },
];

export function SettingsPage() {
  const [preferences, setLocalPreferences] = useState<Preferences>(() => getPreferences());

  useEffect(() => {
    setPreferences(preferences);
  }, [preferences]);

  return (
    <PageSection title="Settings" subtitle="Theme, accent colors, and notification behavior for the app shell.">
      <div className="settings-grid">
        <section className="glass-panel settings-panel">
          <div className="panel-head">
            <div>
              <h2>Theme customization</h2>
              <p>Light, dark, and AMOLED modes support multiple accent palettes.</p>
            </div>
          </div>
          <div className="settings-stack">
            <div className="toggle-row">
              <div>
                <strong>Mode</strong>
                <p>Switch the full application between light, dark, and AMOLED themes.</p>
              </div>
              <div className="segmented-control three-up">
                <button className={preferences.theme === "light" ? "segment active" : "segment"} onClick={() => setLocalPreferences({ ...preferences, theme: "light" })}>Light</button>
                <button className={preferences.theme === "dark" ? "segment active" : "segment"} onClick={() => setLocalPreferences({ ...preferences, theme: "dark" })}>Dark</button>
                <button className={preferences.theme === "amoled" ? "segment active" : "segment"} onClick={() => setLocalPreferences({ ...preferences, theme: "amoled" })}>AMOLED</button>
              </div>
            </div>
            <div>
              <strong>Accent palette</strong>
              <p className="settings-hint">Choose one palette for buttons, highlights, progress bars, and search accents.</p>
              <div className="palette-grid palette-grid-extended">
                {accents.map((accent) => (
                  <button key={accent.value} className={preferences.accent === accent.value ? "palette-card active" : "palette-card"} onClick={() => setLocalPreferences({ ...preferences, accent: accent.value })}>
                    <span className={`palette-preview ${accent.value}`} />
                    <strong>{accent.label}</strong>
                    <span>{accent.hint}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="glass-panel settings-panel">
          <div className="panel-head">
            <div>
              <h2>Notification preferences</h2>
              <p>Control which in-app alerts matter most while keeping the drawer useful.</p>
            </div>
          </div>
          <div className="settings-stack">
            <label className="toggle-row checkbox-row">
              <div>
                <strong>Email notifications</strong>
                <p>Reserved for future delivery channels. Preference is stored now.</p>
              </div>
              <input type="checkbox" checked={preferences.emailNotifications} onChange={(event) => setLocalPreferences({ ...preferences, emailNotifications: event.target.checked })} />
            </label>
            <label className="toggle-row checkbox-row">
              <div>
                <strong>Budget alerts</strong>
                <p>Show warnings for 80%, 100%, and 120% threshold crossings.</p>
              </div>
              <input type="checkbox" checked={preferences.budgetAlerts} onChange={(event) => setLocalPreferences({ ...preferences, budgetAlerts: event.target.checked })} />
            </label>
            <label className="toggle-row checkbox-row">
              <div>
                <strong>Recurring reminders</strong>
                <p>Show upcoming bills and recurring payment notices in the drawer.</p>
              </div>
              <input type="checkbox" checked={preferences.recurringAlerts} onChange={(event) => setLocalPreferences({ ...preferences, recurringAlerts: event.target.checked })} />
            </label>
          </div>
        </section>
      </div>
    </PageSection>
  );
}
