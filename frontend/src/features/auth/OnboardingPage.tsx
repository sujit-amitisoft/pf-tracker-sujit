import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../../services/api";

export function OnboardingPage() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [type, setType] = useState("BANK_ACCOUNT");
  const [openingBalance, setOpeningBalance] = useState("0");
  const [error, setError] = useState<string | null>(null);

  const createAccount = async () => {
    try {
      await api.post("/api/accounts", {
        name,
        type,
        openingBalance: Number(openingBalance || 0),
        institutionName: name,
      });
      navigate("/", { replace: true });
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Failed to create account");
    }
  };

  return (
    <div className="auth-screen">
      <div className="auth-orb auth-orb-a" />
      <div className="auth-orb auth-orb-b" />
      <section className="auth-hero glass-panel onboarding-hero">
        <div className="auth-copy">
          <p className="eyebrow">Onboarding</p>
          <h1>Set up your first account</h1>
          <p className="auth-subcopy">
            Create the primary wallet that will receive income, expenses, and recurring payments. You can add more accounts later.
          </p>
        </div>
        <div className="auth-form-card">
          <div className="form-grid auth-form-grid">
            <input placeholder="Account name" value={name} onChange={(e) => setName(e.target.value)} />
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="BANK_ACCOUNT">Bank account</option>
              <option value="CASH_WALLET">Cash wallet</option>
              <option value="CREDIT_CARD">Credit card</option>
              <option value="SAVINGS_ACCOUNT">Savings account</option>
            </select>
            <input placeholder="Opening balance" value={openingBalance} onChange={(e) => setOpeningBalance(e.target.value)} />
            {error ? <p className="form-error">{error}</p> : null}
            <button className="button primary" onClick={createAccount}>Continue</button>
            <button className="button ghost" onClick={() => navigate("/", { replace: true })}>Skip for now</button>
          </div>
        </div>
      </section>
    </div>
  );
}