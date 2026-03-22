import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppSelect } from "../../components/FormControls";

type Budget = { id: string; categoryId: string; category: string; month: number; year: number; amount: string; actualSpend: string; percentUsed: number };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };

export function BudgetsPage() {
  const now = new Date();
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ categoryId: "", amount: "", alertThresholdPercent: "80" });
  const [error, setError] = useState<string | null>(null);
  const budgets = useQuery({ queryKey: ["budgets", now.getMonth() + 1, now.getFullYear()], queryFn: async () => (await api.get<Budget[]>(`/api/budgets?month=${now.getMonth() + 1}&year=${now.getFullYear()}`)).data });
  const categories = useQuery({ queryKey: ["categories"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });

  const saveBudget = async () => {
    if (!form.categoryId || !form.amount) {
      setError("Select a category and enter a budget amount.");
      return;
    }
    setError(null);
    await api.post("/api/budgets", {
      categoryId: form.categoryId,
      month: now.getMonth() + 1,
      year: now.getFullYear(),
      amount: Number(form.amount),
      alertThresholdPercent: Number(form.alertThresholdPercent),
    });
    setForm({ categoryId: "", amount: "", alertThresholdPercent: "80" });
    await queryClient.invalidateQueries({ queryKey: ["budgets"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  const categoryOptions = [
    { value: "", label: "Select expense category" },
    ...((categories.data ?? []).filter((item) => item.type === "EXPENSE").map((item) => ({ value: item.id, label: item.name }))),
  ];

  const thresholdOptions = [
    { value: "80", label: "Alert at 80%" },
    { value: "100", label: "Alert at 100%" },
    { value: "120", label: "Alert at 120%" },
  ];

  const handleAmountChange = (value: string) => {
    const numericValue = value.replace(/[^\d.]/g, "");
    const normalizedValue = numericValue
      .replace(/(\..*)\./g, "$1")
      .replace(/^(\d*\.\d{0,2}).*$/, "$1");

    setForm({ ...form, amount: normalizedValue });
  };

  return (
    <section className="glass-panel budgets-page-shell budgets-sticky-layout">
      <div className="sticky-section-head budgets-sticky-head">
        <div className="panel-head budgets-head">
          <div>
            <h2>Spending Limits</h2>
            <p>Set budget and compare actual spending against category limits.</p>
          </div>
        </div>
        <div className="form-grid compact-form-grid panel-enter app-form-strip budgets-form-row">
          <AppSelect value={form.categoryId} onChange={(value) => setForm({ ...form, categoryId: value })} options={categoryOptions} placeholder="Select expense category" />
          <input placeholder="Budget amount" inputMode="decimal" value={form.amount} onChange={(e) => handleAmountChange(e.target.value)} />
          <AppSelect value={form.alertThresholdPercent} onChange={(value) => setForm({ ...form, alertThresholdPercent: value })} options={thresholdOptions} placeholder="Alert at 80%" />
          <button className="button primary budgets-action-button budgets-inline-action" onClick={saveBudget}>Set Budget</button>
        </div>
        {error ? <p className="form-error budget-form-error">{error}</p> : null}
      </div>

      <div className="table-shell transactions-table-shell budgets-table-shell">
        <div className="budget-grid-live structured-budget-list budgets-table-scrollable">
          {(budgets.data ?? []).map((item) => (
            <div key={item.id} className="budget-live-row structured-budget-row budgets-table-row">
              <div className="budgets-table-main"><strong>{item.category}</strong><p>{item.actualSpend} / {item.amount}</p></div>
              <div className="budgets-table-progress">
                <div className="meter" title={`Spent ${Number(item.actualSpend).toFixed(2)} of ${Number(item.amount).toFixed(2)} (${item.percentUsed}%)`}><span style={{ width: `${Math.min(item.percentUsed, 100)}%` }} /></div>
                <div className="progress-caption"><span>0%</span><span>${item.actualSpend} / ${item.amount}</span><span>100%</span></div>
              </div>
              <strong className="budgets-table-percent">{item.percentUsed}%</strong>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}


