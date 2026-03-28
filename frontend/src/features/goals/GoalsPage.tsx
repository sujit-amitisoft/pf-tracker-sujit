import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { handlePermissionDenied } from "../../services/apiErrors";
import { AppDateField, AppSelect } from "../../components/FormControls";
import { useState } from "react";
import { createPortal } from "react-dom";

type Goal = { id: string; name: string; targetAmount: string; currentAmount: string; targetDate: string; status: string; progressPercent: number; linkedAccountId: string | null; linkedAccountName: string | null; shared: boolean };
type Account = { id: string; name: string; accessRole: string };

type GoalForm = { id?: string; name: string; targetAmount: string; targetDate: string; linkedAccountId: string };

function createInitialGoalForm(): GoalForm {
  return { name: "", targetAmount: "", targetDate: "", linkedAccountId: "" };
}

export function GoalsPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<GoalForm>(() => createInitialGoalForm());
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
    const payload = {
      name: form.name,
      targetAmount: Number(form.targetAmount),
      targetDate: form.targetDate || null,
      linkedAccountId: form.linkedAccountId || null,
      icon: "target",
      color: "teal",
    };
    try {
      if (form.id) {
        await api.put(`/api/goals/${form.id}`, payload);
      } else {
        await api.post("/api/goals", payload);
      }
      setForm(createInitialGoalForm());
      await queryClient.invalidateQueries({ queryKey: ["goals"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["notifications"] });
    } catch (err: any) {
      handlePermissionDenied(err, setError, form.id ? "Failed to update goal" : "Failed to create goal");
    }
  };

  const editGoal = (item: Goal) => {
    setForm({
      id: item.id,
      name: item.name,
      targetAmount: item.targetAmount,
      targetDate: item.targetDate || "",
      linkedAccountId: item.linkedAccountId || "",
    });
    setError(null);
  };

  const confirmDeleteGoal = async () => {
    if (!deleteConfirmGoalId) return;
    try {
      await api.delete(`/api/goals/${deleteConfirmGoalId}`);
      setDeleteConfirmGoalId(null);
      if (form.id === deleteConfirmGoalId) setForm(createInitialGoalForm());
      await queryClient.invalidateQueries({ queryKey: ["goals"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["notifications"] });
    } catch (err: any) {
      handlePermissionDenied(err, setError, "Failed to delete goal");
    }
  };

  const accountOptions = [
    { value: "", label: "Optional linked account" },
    ...((accounts.data ?? []).map((item) => ({ value: item.id, label: `${item.name} (${item.accessRole})` }))),
  ];

  const updateForm = <K extends keyof GoalForm>(key: K, value: GoalForm[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    setError(null);
  };

  const handleTargetAmountChange = (value: string) => {
    const numericValue = value.replace(/[^\d.]/g, "");
    const normalizedValue = numericValue
      .replace(/(\..*)\./g, "$1")
      .replace(/^(\d*\.\d{0,2}).*$/, "$1");

    updateForm("targetAmount", normalizedValue);
  };

  const deleteTarget = (goals.data ?? []).find((item) => item.id === deleteConfirmGoalId) ?? null;

  return (
    <>
      <section className="glass-panel goals-page-shell goals-sticky-layout">
        <div className="sticky-section-head goals-sticky-head">
          <div className="panel-head budgets-head">
            <div>
              <h2>Savings Planner</h2>
              <p>Create personal or shared savings goals and connect them to accessible accounts.</p>
            </div>
          </div>
          <div className="form-grid compact-form-grid panel-enter app-form-strip goals-form-strip goals-form-row">
            <input placeholder="Goal name" value={form.name} onChange={(e) => updateForm("name", e.target.value)} />
            <input placeholder="Target amount" inputMode="decimal" value={form.targetAmount} onChange={(e) => handleTargetAmountChange(e.target.value)} />
            <AppDateField value={form.targetDate} onChange={(e) => updateForm("targetDate", e.target.value)} />
            <AppSelect value={form.linkedAccountId} onChange={(value) => updateForm("linkedAccountId", value)} options={accountOptions} placeholder="Optional linked account" className="goals-linked-account-select" />
            <button className="button primary budgets-action-button goals-inline-action" onClick={saveGoal}>{form.id ? "Update Goal" : "Create Goal"}</button>
          </div>
          {error ? <p className="form-error budget-form-error">{error}</p> : null}
        </div>

        <div className="table-shell transactions-table-shell goals-table-shell transactions-list-shell">
          <div className="table-row table-head table-row-transactions goals-table-head-row">
            <span>Name</span>
            <span>Saved</span>
            <span>Target</span>
            <span>Progress</span>
            <span>Due</span>
            <span>Status</span>
            <span className="actions-header">Actions</span>
          </div>
          <div className="transactions-table-body transactions-table-scrollable">
            {(goals.data ?? []).length ? (goals.data ?? []).map((item) => (
              <div key={item.id} className="table-row table-row-transactions transaction-data-row goals-table-data-row">
                <span className="transaction-merchant-cell"><strong>{item.name}</strong><small>{item.linkedAccountName ? `${item.shared ? "Shared" : "Linked"}: ${item.linkedAccountName}` : "Standalone goal"}</small></span>
                <span>${item.currentAmount}</span>
                <span>${item.targetAmount}</span>
                <span className="budgets-inline-progress-cell">
                  <div className="meter" title={`Saved $${Number(item.currentAmount).toFixed(2)} of $${Number(item.targetAmount).toFixed(2)} (${item.progressPercent}%)`}><span style={{ width: `${item.progressPercent}%` }} /></div>
                  <small>{item.progressPercent}% complete</small>
                </span>
                <span>{item.targetDate || "No date"}</span>
                <span><span className={`status-chip ${item.shared ? "paused" : "active"}`}>{item.status}</span></span>
                <div className="row-actions transactions-row-actions compact-row-actions compact-row-actions-inline">
                  <button className="button ghost small transaction-action-button edit-action-button" type="button" onClick={() => editGoal(item)}>Edit</button>
                  <button className="button ghost small transaction-action-button danger-button delete-action-button" type="button" onClick={() => { setError(null); setDeleteConfirmGoalId(item.id); }}>Delete</button>
                </div>
              </div>
            )) : <div className="empty-state">No goals created yet.</div>}
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
              <p>{deleteTarget ? `${deleteTarget.currentAmount} / ${deleteTarget.targetAmount} - ${deleteTarget.linkedAccountName ?? "No linked account"}` : "This action cannot be undone."}</p>
            </div>
            {error ? <p className="form-error delete-modal-error">{error}</p> : null}
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

