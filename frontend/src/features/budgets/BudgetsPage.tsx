import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppSelect } from "../../components/FormControls";

type Budget = { id: string; categoryId: string; category: string; accountId: string | null; account: string; month: number; year: number; amount: string; actualSpend: string; percentUsed: number; alertThresholdPercent: number; shared: boolean };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };
type Account = { id: string; name: string; accessRole: string };

type BudgetForm = { id?: string; categoryId: string; accountId: string; amount: string; alertThresholdPercent: string };

function createInitialForm(): BudgetForm {
  return { categoryId: "", accountId: "ALL", amount: "", alertThresholdPercent: "80" };
}

export function BudgetsPage() {
  const now = new Date();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<BudgetForm>(() => createInitialForm());
  const [error, setError] = useState<string | null>(null);
  const budgets = useQuery({ queryKey: ["budgets", now.getMonth() + 1, now.getFullYear()], queryFn: async () => (await api.get<Budget[]>(`/api/budgets?month=${now.getMonth() + 1}&year=${now.getFullYear()}`)).data });
  const categories = useQuery({ queryKey: ["categories"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });
  const accounts = useQuery({ queryKey: ["accounts", "budget-targets"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });

  const saveBudget = async () => {
    if (!form.categoryId || !form.amount) {
      setError("Select a category and enter a budget amount.");
      return;
    }
    setError(null);
    const payload = {
      categoryId: form.categoryId,
      accountId: form.accountId === "ALL" ? null : form.accountId,
      month: now.getMonth() + 1,
      year: now.getFullYear(),
      amount: Number(form.amount),
      alertThresholdPercent: Number(form.alertThresholdPercent),
    };
    if (form.id) {
      await api.put(`/api/budgets/${form.id}`, payload);
    } else {
      await api.post("/api/budgets", payload);
    }
    setForm(createInitialForm());
    await queryClient.invalidateQueries({ queryKey: ["budgets"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  const deleteBudget = async (id: string) => {
    await api.delete(`/api/budgets/${id}`);
    if (form.id === id) setForm(createInitialForm());
    await queryClient.invalidateQueries({ queryKey: ["budgets"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  const editBudget = (item: Budget) => {
    setForm({
      id: item.id,
      categoryId: item.categoryId,
      accountId: item.accountId ?? "ALL",
      amount: item.amount,
      alertThresholdPercent: String(item.alertThresholdPercent),
    });
    setError(null);
  };

  const categoryOptions = useMemo(() => [
    { value: "", label: "Select expense category" },
    ...((categories.data ?? []).filter((item) => item.type === "EXPENSE").map((item) => ({ value: item.id, label: item.name }))),
  ], [categories.data]);

  const accountOptions = useMemo(() => [
    { value: "ALL", label: "All accessible accounts" },
    ...((accounts.data ?? []).map((item) => ({ value: item.id, label: `${item.name} (${item.accessRole})` }))),
  ], [accounts.data]);

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
            <p>Set personal or shared budgets and compare actual spending against category limits.</p>
          </div>
        </div>
        <div className="form-grid compact-form-grid panel-enter app-form-strip budgets-form-row budgets-form-row-wide">
          <AppSelect value={form.categoryId} onChange={(value) => setForm({ ...form, categoryId: value })} options={categoryOptions} placeholder="Select expense category" />
          <AppSelect value={form.accountId} onChange={(value) => setForm({ ...form, accountId: value })} options={accountOptions} placeholder="All accessible accounts" />
          <input placeholder="Budget amount" inputMode="decimal" value={form.amount} onChange={(e) => handleAmountChange(e.target.value)} />
          <AppSelect value={form.alertThresholdPercent} onChange={(value) => setForm({ ...form, alertThresholdPercent: value })} options={thresholdOptions} placeholder="Alert at 80%" />
          <button className="button primary budgets-action-button budgets-inline-action" onClick={saveBudget}>{form.id ? "Update Budget" : "Set Budget"}</button>
        </div>
        {error ? <p className="form-error budget-form-error">{error}</p> : null}
      </div>

      <div className="table-shell transactions-table-shell budgets-table-shell transactions-list-shell">
        <div className="table-row table-head table-row-transactions budgets-table-head-row">
          <span>Category</span>
          <span>Account</span>
          <span>Budget</span>
          <span>Spent</span>
          <span>Usage</span>
          <span>Mode</span>
          <span className="actions-header">Actions</span>
        </div>
        <div className="transactions-table-body transactions-table-scrollable">
          {(budgets.data ?? []).length ? (budgets.data ?? []).map((item) => (
            <div key={item.id} className="table-row table-row-transactions transaction-data-row budgets-table-data-row">
              <span>{item.category}</span>
              <span>{item.account}</span>
              <span>${item.amount}</span>
              <span>${item.actualSpend}</span>
              <span className="budgets-inline-progress-cell">
                <div className="meter" title={`Spent ${Number(item.actualSpend).toFixed(2)} of ${Number(item.amount).toFixed(2)} (${item.percentUsed}%)`}><span style={{ width: `${Math.min(item.percentUsed, 100)}%` }} /></div>
                <small>{item.percentUsed}% at {item.alertThresholdPercent}% alert</small>
              </span>
              <span><span className={`status-chip ${item.shared ? "paused" : "active"}`}>{item.shared ? "Shared" : "Personal"}</span></span>
              <div className="row-actions transactions-row-actions compact-row-actions compact-row-actions-inline">
                <button className="button ghost small transaction-action-button edit-action-button" type="button" onClick={() => editBudget(item)}>Edit</button>
                <button className="button ghost small transaction-action-button danger-button delete-action-button" type="button" onClick={() => deleteBudget(item.id)}>Delete</button>
              </div>
            </div>
          )) : <div className="empty-state">No budgets for this period yet.</div>}
        </div>
      </div>
    </section>
  );
}
