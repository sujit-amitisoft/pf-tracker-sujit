import { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { showAppToast } from "../../services/toast";
import { AppSelect } from "../../components/FormControls";

type Account = { id: string; name: string; type: string; currentBalance: string; institutionName: string };

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
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [form, setForm] = useState<AccountForm>(() => createInitialAccountForm());
  const queryClient = useQueryClient();
  const accounts = useQuery({ queryKey: ["accounts"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });

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
      setToastMessage("Account created");
      window.setTimeout(() => setToastMessage(null), 2600);
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
      await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    } catch (err: any) {
      setFormError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? "Failed to create account");
    }
  };

  return (
    <>
      <section className="glass-panel accounts-page-shell">
        <div className="panel-head transactions-head">
          <div><h2>Money Accounts</h2><p>Profile-backed wallets and balances.</p></div>
          <button className="button primary transactions-add-button" type="button" onClick={openModal}>Add Account</button>
        </div>
        <div className="table-shell accounts-table-shell">
          <div className="table-row table-head accounts-table-row"><span>Name</span><span>Type</span><span>Institution</span><span>Balance</span></div>
          {(accounts.data ?? []).map((item) => (
            <div key={item.id} className="table-row accounts-table-row">
              <span>{item.name}</span>
              <span>{item.type.replace(/_/g, " ")}</span>
              <span>{item.institutionName || "-"}</span>
              <strong>${item.currentBalance}</strong>
            </div>
          ))}
        </div>
      </section>

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

      {toastMessage ? <div className="toast">{toastMessage}</div> : null}
    </>
  );
}











