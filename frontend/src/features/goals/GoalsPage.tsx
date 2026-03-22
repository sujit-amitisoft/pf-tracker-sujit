import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppDateField, AppSelect } from "../../components/FormControls";
import { useState } from "react";
import { createPortal } from "react-dom";

type Goal = { id: string; name: string; targetAmount: string; currentAmount: string; targetDate: string; status: string; progressPercent: number };
type Account = { id: string; name: string };

export function GoalsPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState({ name: "", targetAmount: "", targetDate: "", linkedAccountId: "" });
  const [error, setError] = useState<string | null>(null);
  const [deleteConfirmGoalId, setDeleteConfirmGoalId] = useState<string | null>(null);
  const goals = useQuery({ queryKey: ["goals"], queryFn: async () => (await api.get<Goal[]>("/api/goals")).data });
  const accounts = useQuery({ queryKey: ["accounts"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });

  const saveGoal = async () => {
    if (!form.name || !form.targetAmount) {
      setError("Enter a goal name and target amount.");
      return;
    }
    setError(null);
    await api.post("/api/goals", {
      name: form.name,
      targetAmount: Number(form.targetAmount),
      targetDate: form.targetDate || null,
      linkedAccountId: form.linkedAccountId || null,
      icon: "target",
      color: "teal",
    });
    setForm({ name: "", targetAmount: "", targetDate: "", linkedAccountId: "" });
    await queryClient.invalidateQueries({ queryKey: ["goals"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  const confirmDeleteGoal = async () => {
    if (!deleteConfirmGoalId) return;
    await api.delete(`/api/goals/${deleteConfirmGoalId}`);
    setDeleteConfirmGoalId(null);
    await queryClient.invalidateQueries({ queryKey: ["goals"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  const accountOptions = [
    { value: "", label: "Optional linked account" },
    ...((accounts.data ?? []).map((item) => ({ value: item.id, label: item.name }))),
  ];

  const handleTargetAmountChange = (value: string) => {
    const numericValue = value.replace(/[^\d.]/g, "");
    const normalizedValue = numericValue
      .replace(/(\..*)\./g, "$1")
      .replace(/^(\d*\.\d{0,2}).*$/, "$1");

    setForm({ ...form, targetAmount: normalizedValue });
  };

  const deleteTarget = (goals.data ?? []).find((item) => item.id === deleteConfirmGoalId) ?? null;

  return (
    <>
      <section className="glass-panel goals-page-shell goals-sticky-layout">
        <div className="sticky-section-head goals-sticky-head">
          <div className="panel-head budgets-head">
            <div>
              <h2>Savings Planner</h2>
              <p>Create and track savings goals with deadlines.</p>
            </div>
          </div>
          <div className="form-grid compact-form-grid panel-enter app-form-strip goals-form-strip goals-form-row">
            <input placeholder="Goal name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <input placeholder="Target amount" inputMode="decimal" value={form.targetAmount} onChange={(e) => handleTargetAmountChange(e.target.value)} />
            <AppDateField value={form.targetDate} onChange={(e) => setForm({ ...form, targetDate: e.target.value })} />
            <AppSelect value={form.linkedAccountId} onChange={(value) => setForm({ ...form, linkedAccountId: value })} options={accountOptions} placeholder="Optional linked account" className="goals-linked-account-select" />
            <button className="button primary budgets-action-button goals-inline-action" onClick={saveGoal}>Add Contribution</button>
          </div>
          {error ? <p className="form-error budget-form-error">{error}</p> : null}
        </div>

        <div className="table-shell transactions-table-shell goals-table-shell">
          <div className="goal-list structured-goal-list goals-table-list goals-table-scrollable">
            {(goals.data ?? []).map((item) => (
              <article key={item.id} className="goal-card structured-goal-card goals-table-row">
                <div className="goals-table-main">
                  <strong>{item.name}</strong>
                  <span>{item.currentAmount} / {item.targetAmount}</span>
                </div>
                <div className="goals-table-progress">
                  <div className="meter" title={`Saved $${Number(item.currentAmount).toFixed(2)} of $${Number(item.targetAmount).toFixed(2)} (${item.progressPercent}%)`}><span style={{ width: `${item.progressPercent}%` }} /></div>
                  <div className="progress-caption"><span>0%</span><span>${item.currentAmount} / ${item.targetAmount}</span><span>100%</span></div>
                  <div className="goal-percent">{item.progressPercent}%</div>
                </div>
                <div className="goals-table-due">Due: {item.targetDate || "No date"}</div>
                <div className="goals-row-trash-wrap">
                  <button className="goal-trash-button" type="button" onClick={() => setDeleteConfirmGoalId(item.id)} aria-label={`Delete ${item.name}`}>
                    <span className="goal-trash-icon" aria-hidden="true" />
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      {deleteConfirmGoalId ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={() => setDeleteConfirmGoalId(null)}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-confirm-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>Delete Goal</h2>
                <p>Confirm removal of this goal from your records.</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={() => setDeleteConfirmGoalId(null)} aria-label="Close delete goal modal" />
            </div>
            <div className="delete-confirm-copy">
              <strong>{deleteTarget?.name ?? "Goal"}</strong>
              <p>{deleteTarget ? `${deleteTarget.currentAmount} / ${deleteTarget.targetAmount} - Due: ${deleteTarget.targetDate || "No date"}` : "This action cannot be undone."}</p>
            </div>
            <div className="modal-actions delete-modal-actions">
              <button className="button ghost" type="button" onClick={() => setDeleteConfirmGoalId(null)}>Cancel</button>
              <button className="button primary delete-confirm-button" type="button" onClick={confirmDeleteGoal}>Delete</button>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}
    </>
  );
}



