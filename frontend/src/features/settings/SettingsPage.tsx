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
    <PageSection title="Workspace preferences" subtitle="Control app appearance, accent styling, and alert behavior from one place.">
      <div className="settings-grid settings-grid-spaced settings-grid-refined">
        <section className="glass-panel settings-panel">
          <div className="panel-head">
            <div>
              <h2>Theme customization</h2>
              <p>Switch between light and dark themes, then optionally deepen dark mode with AMOLED surfaces while keeping your accent palette.</p>
            </div>
          </div>
          <div className="settings-stack">
            <div className="toggle-row settings-row-card">
              <div>
                <strong>Mode</strong>
                <p>Switch the full application between light and dark themes.</p>
              </div>
              <div className="segmented-control settings-mode-toggle">
                <button className={preferences.theme === "light" ? "segment active" : "segment"} onClick={() => setLocalPreferences({ ...preferences, theme: "light", amoledDark: false })}>Light</button>
                <button className={preferences.theme === "dark" ? "segment active" : "segment"} onClick={() => setLocalPreferences({ ...preferences, theme: "dark" })}>Dark</button>
              </div>
            </div>
            <label className="toggle-row checkbox-row settings-row-card">
              <div>
                <strong>AMOLED in dark mode</strong>
                <p>Use pure-black surfaces while staying in dark mode. Accent palette still applies.</p>
              </div>
              <input type="checkbox" checked={preferences.theme === "dark" && preferences.amoledDark} onChange={(event) => setLocalPreferences({ ...preferences, theme: "dark", amoledDark: event.target.checked })} />
            </label>
            <div className="settings-accent-section">
              <strong>Accent palette</strong>
              <p className="settings-hint">Choose one palette for buttons, highlights, progress bars, and search accents. These work in both dark and AMOLED dark mode.</p>
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
          <div className="settings-stack settings-stack-refined">
            <label className="toggle-row checkbox-row settings-row-card">
              <div>
                <strong>Email notifications</strong>
                <p>Reserved for future delivery channels. Preference is stored now.</p>
              </div>
              <input type="checkbox" checked={preferences.emailNotifications} onChange={(event) => setLocalPreferences({ ...preferences, emailNotifications: event.target.checked })} />
            </label>
            <label className="toggle-row checkbox-row settings-row-card">
              <div>
                <strong>Budget alerts</strong>
                <p>Show warnings for 80%, 100%, and 120% threshold crossings.</p>
              </div>
              <input type="checkbox" checked={preferences.budgetAlerts} onChange={(event) => setLocalPreferences({ ...preferences, budgetAlerts: event.target.checked })} />
            </label>
            <label className="toggle-row checkbox-row settings-row-card">
              <div>
                <strong>Recurring reminders</strong>
                <p>Show upcoming bills and recurring payment notices in the drawer.</p>
              </div>
              <input type="checkbox" checked={preferences.recurringAlerts} onChange={(event) => setLocalPreferences({ ...preferences, recurringAlerts: event.target.checked })} />
            </label>
            <label className="toggle-row checkbox-row settings-row-card">
              <div>
                <strong>Popup notifications</strong>
                <p>Show a brief top-right popup when new alerts arrive.</p>
              </div>
              <input type="checkbox" checked={preferences.popupNotifications} onChange={(event) => setLocalPreferences({ ...preferences, popupNotifications: event.target.checked })} />
            </label>
          </div>
        </section>
      </div>
    </PageSection>
  );
}

