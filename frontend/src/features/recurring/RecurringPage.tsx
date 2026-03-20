import { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppDateField, AppSelect } from "../../components/FormControls";

type Recurring = {
  id: string;
  title: string;
  type: "INCOME" | "EXPENSE";
  amount: string;
  nextRunDate: string;
  autoCreateTransaction: boolean;
  paused: boolean;
  category: string | null;
  account: string | null;
  frequency: string;
};

type Account = { id: string; name: string };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };

type RecurringForm = {
  title: string;
  amount: string;
  categoryId: string;
  accountId: string;
  frequency: string;
  startDate: string;
};

function createInitialForm(): RecurringForm {
  return {
    title: "",
    amount: "",
    categoryId: "",
    accountId: "",
    frequency: "MONTHLY",
    startDate: new Date().toISOString().slice(0, 10),
  };
}

export function RecurringPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RecurringForm>(() => createInitialForm());
  const [error, setError] = useState<string | null>(null);
  const [deleteConfirmRecurringId, setDeleteConfirmRecurringId] = useState<string | null>(null);

  const recurring = useQuery({ queryKey: ["recurring"], queryFn: async () => (await api.get<Recurring[]>("/api/recurring")).data });
  const accounts = useQuery({ queryKey: ["accounts"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });
  const categories = useQuery({ queryKey: ["categories"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });

  const filteredCategories = (categories.data ?? []).filter((item) => item.type === "EXPENSE");

  const categoryOptions = useMemo(
    () => [{ value: "", label: "Select category" }, ...filteredCategories.map((item) => ({ value: item.id, label: item.name }))],
    [filteredCategories],
  );

  const accountOptions = useMemo(
    () => [{ value: "", label: "Select account" }, ...(accounts.data ?? []).map((item) => ({ value: item.id, label: item.name }))],
    [accounts.data],
  );

  const frequencyOptions = [
    { value: "MONTHLY", label: "Monthly" },
    { value: "WEEKLY", label: "Weekly" },
    { value: "YEARLY", label: "Yearly" },
    { value: "DAILY", label: "Daily" },
  ];

  const updateForm = <K extends keyof RecurringForm>(key: K, value: RecurringForm[K]) => {
    const nextValue = key === "amount" ? String(value).replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1") : value;
    setForm((current) => ({ ...current, [key]: nextValue }));
    setError(null);
  };

  const saveRecurring = async () => {
    if (!form.title.trim() || !form.amount || !form.categoryId || !form.accountId || !form.frequency) {
      setError("Enter title, amount, category, account, and frequency.");
      return;
    }

    await api.post("/api/recurring", {
      title: form.title.trim(),
      type: "EXPENSE",
      amount: Number(form.amount),
      categoryId: form.categoryId,
      accountId: form.accountId,
      frequency: form.frequency,
      startDate: form.startDate,
      endDate: null,
      autoCreateTransaction: true,
    });

    setForm(createInitialForm());
    setError(null);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["recurring"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard", "upcoming"] }),
      queryClient.invalidateQueries({ queryKey: ["notifications"] }),
    ]);
  };

  const confirmDeleteRecurring = async () => {
    if (!deleteConfirmRecurringId) return;
    await api.delete(`/api/recurring/${deleteConfirmRecurringId}`);
    setDeleteConfirmRecurringId(null);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["recurring"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard", "upcoming"] }),
      queryClient.invalidateQueries({ queryKey: ["notifications"] }),
    ]);
  };

  const deleteTarget = (recurring.data ?? []).find((item) => item.id === deleteConfirmRecurringId) ?? null;

  return (
    <>
      <section className="glass-panel recurring-page-shell recurring-sticky-layout">
        <div className="sticky-section-head recurring-sticky-head">
          <div className="panel-head budgets-head recurring-head-copy">
            <div>
              <h2>Bill Schedule</h2>
              <p>Define recurring bills and subscriptions, then watch upcoming recurring expenses.</p>
            </div>
          </div>
          <div className="recurring-form-stack">
            <div className="form-grid compact-form-grid panel-enter app-form-strip recurring-form-row recurring-form-row-primary recurring-form-row-expense-only recurring-form-row-single-line">
              <input placeholder="Title" value={form.title} onChange={(event) => updateForm("title", event.target.value)} />
              <input placeholder="Amount" inputMode="decimal" value={form.amount} onChange={(event) => updateForm("amount", event.target.value)} />
              <AppSelect value={form.categoryId} onChange={(value) => updateForm("categoryId", value)} options={categoryOptions} placeholder="Select category" />
              <AppSelect value={form.accountId} onChange={(value) => updateForm("accountId", value)} options={accountOptions} placeholder="Select account" />
              <AppSelect value={form.frequency} onChange={(value) => updateForm("frequency", value)} options={frequencyOptions} placeholder="Frequency" />
              <AppDateField className="recurring-date-field" value={form.startDate} onChange={(event) => updateForm("startDate", event.target.value)} />
              <button className="button primary budgets-action-button recurring-inline-action" type="button" onClick={saveRecurring}>New Recurring Item</button>
            </div>
          </div>
          {error ? <p className="form-error budget-form-error">{error}</p> : null}
        </div>

        <div className="table-shell transactions-table-shell recurring-table-shell">
          <div className="table-row table-head recurring-table-row recurring-table-head-row">
            <span>Title</span>
            <span>Category</span>
            <span>Account</span>
            <span>Frequency</span>
            <span>Next run</span>
            <span className="amount-header">Amount</span>
            <span className="actions-header">Actions</span>
          </div>
          <div className="recurring-table-scrollable">
            {(recurring.data ?? []).map((item) => (
              <div key={item.id} className="table-row recurring-table-row recurring-data-row">
                <div className="recurring-title-cell">
                  <strong>{item.title}</strong>
                  <small>Expense</small>
                </div>
                <span>{item.category ?? "-"}</span>
                <span>{item.account ?? "-"}</span>
                <span>{item.frequency}</span>
                <span>{item.nextRunDate}</span>
                <strong className="transaction-amount expense">-${item.amount}</strong>
                <div className="goals-row-trash-wrap recurring-row-trash-wrap">
                  <button className="goal-trash-button" type="button" onClick={() => setDeleteConfirmRecurringId(item.id)} aria-label={`Delete ${item.title}`}>
                    <span className="goal-trash-icon" aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {deleteConfirmRecurringId ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={() => setDeleteConfirmRecurringId(null)}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-confirm-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>Delete Recurring Item</h2>
                <p>Confirm removal of this recurring item from your records.</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={() => setDeleteConfirmRecurringId(null)} aria-label="Close delete recurring modal" />
            </div>
            <div className="delete-confirm-copy">
              <strong>{deleteTarget?.title ?? "Recurring item"}</strong>
              <p>{deleteTarget ? `${deleteTarget.amount} / ${deleteTarget.frequency} - Due: ${deleteTarget.nextRunDate}` : "This action cannot be undone."}</p>
            </div>
            <div className="modal-actions delete-modal-actions">
              <button className="button ghost" type="button" onClick={() => setDeleteConfirmRecurringId(null)}>Cancel</button>
              <button className="button primary delete-confirm-button" type="button" onClick={confirmDeleteRecurring}>Delete</button>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}
    </>
  );
}


