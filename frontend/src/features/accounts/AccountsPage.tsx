import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppSelect } from "../../components/FormControls";

type Account = { id: string; name: string; type: string; currentBalance: string; institutionName: string; shared: boolean; accessRole: string; memberCount: number };
type Member = { userId: string; email: string; displayName: string; role: "OWNER" | "EDITOR" | "VIEWER"; owner: boolean; addedAt: string };
type Activity = { id: string; activityType: string; subjectType: string; subjectId: string | null; actorName: string; actorEmail: string | null; summary: string; createdAt: string };

type AccountForm = {
  name: string;
  type: string;
  openingBalance: string;
  institutionName: string;
};

function createInitialAccountForm(): AccountForm {
  return {
    name: "",
    type: "BANK_ACCOUNT",
    openingBalance: "",
    institutionName: "",
  };
}

export function AccountsPage() {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState<AccountForm>(() => createInitialAccountForm());
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const queryClient = useQueryClient();
  const accounts = useQuery({ queryKey: ["accounts"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });

  useEffect(() => {
    if (!selectedAccountId && accounts.data?.[0]) {
      setSelectedAccountId(accounts.data[0].id);
    }
  }, [accounts.data, selectedAccountId]);

  const selectedAccount = accounts.data?.find((item) => item.id === selectedAccountId) ?? accounts.data?.[0] ?? null;
  const members = useQuery({
    queryKey: ["account-members", selectedAccount?.id],
    enabled: Boolean(selectedAccount?.id),
    queryFn: async () => (await api.get<Member[]>(`/api/accounts/${selectedAccount?.id}/members`)).data,
  });
  const activity = useQuery({
    queryKey: ["account-activity", selectedAccount?.id],
    enabled: Boolean(selectedAccount?.id),
    queryFn: async () => (await api.get<Activity[]>(`/api/accounts/${selectedAccount?.id}/activity`)).data,
  });

  const accountTypeOptions = useMemo(
    () => [
      { value: "BANK_ACCOUNT", label: "Bank account" },
      { value: "CREDIT_CARD", label: "Credit card" },
      { value: "CASH_WALLET", label: "Cash wallet" },
      { value: "SAVINGS_ACCOUNT", label: "Savings account" },
    ],
    [],
  );

  const resetModal = () => {
    setForm(createInitialAccountForm());
    setFormError(null);
  };

  const openModal = () => {
    resetModal();
    setShowCreateModal(true);
  };

  const closeModal = () => {
    setShowCreateModal(false);
    resetModal();
  };

  const updateForm = (key: keyof AccountForm, value: string) => {
    const nextValue = key === "openingBalance" ? value.replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1") : value;
    setForm((current) => ({ ...current, [key]: nextValue }));
    setFormError(null);
  };

  const saveAccount = async () => {
    if (!form.name.trim()) {
      setFormError("Enter an account name.");
      return;
    }

    try {
      await api.post("/api/accounts", {
        name: form.name.trim(),
        type: form.type,
        openingBalance: form.openingBalance ? Number(form.openingBalance) : 0,
        institutionName: form.institutionName.trim(),
      });
      closeModal();
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    } catch (err: any) {
      setFormError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? "Failed to create account");
    }
  };

  return (
    <>
      <div className="page-fit-layout accounts-page-layout">
        <section className="glass-panel accounts-page-shell">
          <div className="panel-head transactions-head">
            <div><h2>Money Accounts</h2><p>Profile-backed wallets, shared access, and recent account activity.</p></div>
            <button className="button primary transactions-add-button" type="button" onClick={openModal}>Add Account</button>
          </div>
          <div className="table-shell accounts-table-shell">
            <div className="table-row table-head accounts-table-row accounts-table-head-row"><span>Name</span><span>Type</span><span>Institution</span><span>Access</span><span>Balance</span></div>
            <div className="accounts-table-scrollable">
              {(accounts.data ?? []).map((item) => (
                <button key={item.id} type="button" className={`table-row accounts-table-row accounts-table-button ${selectedAccount?.id === item.id ? "active" : ""}`} onClick={() => setSelectedAccountId(item.id)}>
                  <span>{item.name}</span>
                  <span>{item.type.replace(/_/g, " ")}</span>
                  <span>{item.institutionName || "-"}</span>
                  <span>{item.accessRole} - {item.memberCount}</span>
                  <strong>${item.currentBalance}</strong>
                </button>
              ))}
            </div>
          </div>
        </section>

        <section className="glass-panel nested-panel account-side-panel">
          <div className="panel-head"><div><h2>Shared With</h2><p>Membership and activity for the selected account.</p></div></div>
          {selectedAccount ? (
            <>
              <div className="top-categories-card compact-info-card">
                <strong>{selectedAccount.name}</strong>
                <p>{selectedAccount.shared ? "Shared account" : "Personal account"} - {selectedAccount.accessRole}</p>
              </div>
              <div className="account-side-section">
                <h3>Members</h3>
                <div className="shared-members-list compact-shared-list">
                  {(members.data ?? []).map((member) => (
                    <article key={member.userId} className="rule-card shared-member-card compact-member-card">
                      <div className="rule-card-head"><strong>{member.displayName || member.email}</strong><span className={`status-chip ${member.owner ? "active" : "paused"}`}>{member.role}</span></div>
                      <p>{member.email}</p>
                    </article>
                  ))}
                </div>
              </div>
              <div className="account-side-section">
                <h3>Activity</h3>
                <div className="shared-members-list compact-shared-list account-activity-list">
                  {(activity.data ?? []).slice(0, 10).map((item) => (
                    <article key={item.id} className="rule-card shared-member-card compact-member-card">
                      <div className="rule-card-head"><strong>{item.actorName || item.actorEmail || "User"}</strong><span className="status-chip active">{item.activityType.replace(/_/g, " ")}</span></div>
                      <p>{item.summary}</p>
                      <span className="rule-meta">{new Date(item.createdAt).toLocaleString()}</span>
                    </article>
                  ))}
                </div>
              </div>
            </>
          ) : <div className="empty-state">Create an account to start using shared account management.</div>}
        </section>
      </div>

      {showCreateModal ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={closeModal}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-form-modal account-dialog-card" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>Add Account</h2>
                <p>Create a new account with type, institution, and opening balance.</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={closeModal} aria-label="Close account modal" />
            </div>
            <div className="form-grid structured-form-grid app-form-grid account-form-grid">
              <div className="modal-field-wrap"><input placeholder="Account name" value={form.name} onChange={(event) => updateForm("name", event.target.value)} /></div>
              <div className="modal-field-wrap"><AppSelect value={form.type} onChange={(value) => updateForm("type", value)} options={accountTypeOptions} placeholder="Select account type" /></div>
              <div className="modal-field-wrap"><input placeholder="Opening balance" inputMode="decimal" value={form.openingBalance} onChange={(event) => updateForm("openingBalance", event.target.value)} /></div>
              <div className="modal-field-wrap"><input placeholder="Institution name" value={form.institutionName} onChange={(event) => updateForm("institutionName", event.target.value)} /></div>
              {formError ? <p className="form-error">{formError}</p> : null}
              <div className="modal-actions transaction-modal-actions account-modal-actions">
                <button className="button ghost" type="button" onClick={closeModal}>Cancel</button>
                <button className="button primary" type="button" onClick={saveAccount}>Save</button>
              </div>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}
    </>
  );
}


