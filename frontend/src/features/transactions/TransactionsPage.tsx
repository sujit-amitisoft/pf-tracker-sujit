import { useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { api } from "../../services/api";
import { showAppToast } from "../../services/toast";
import { AppDateField, AppSelect } from "../../components/FormControls";

type Transaction = { id: string; merchant: string; category: string; account: string; type: "INCOME" | "EXPENSE"; amount: string; date: string; note: string };
type Account = { id: string; name: string };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };

export function TransactionsPage() {
  const [searchParams] = useSearchParams();
  const initialSearch = (searchParams.get("q") ?? "").trim();
  const [filters, setFilters] = useState({ type: "ALL", account: "ALL", category: "ALL", date: "", search: initialSearch });
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [form, setForm] = useState({ type: "EXPENSE", amount: "", date: "", merchant: "", note: "", accountId: "", categoryId: "" });
  const [formError, setFormError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const transactions = useQuery({ queryKey: ["transactions"], queryFn: async () => (await api.get<Transaction[]>("/api/transactions")).data });
  const accounts = useQuery({ queryKey: ["accounts"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });
  const categories = useQuery({ queryKey: ["categories"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });


  const filteredCategories = (categories.data ?? []).filter((item) => item.type === form.type);

  const filtered = useMemo(() => {
    const normalizedSearch = filters.search.trim().toLowerCase();
    return (transactions.data ?? []).filter((item) => {
      const searchPass = !normalizedSearch || [item.id, item.merchant, item.category, item.account, item.note, item.type].some((value) => value?.toLowerCase().includes(normalizedSearch));
      const typePass = filters.type === "ALL" || item.type === filters.type;
      const accountPass = filters.account === "ALL" || item.account === filters.account;
      const categoryPass = filters.category === "ALL" || item.category === filters.category;
      const datePass = !filters.date || item.date === filters.date;
      return searchPass && typePass && accountPass && categoryPass && datePass;
    });
  }, [filters, transactions.data]);

  const startEdit = (item: Transaction) => {
    setEditingId(item.id);
    setFormError(null);
    setForm({
      type: item.type,
      amount: item.amount,
      date: item.date,
      merchant: item.merchant,
      note: item.note ?? "",
      accountId: (accounts.data ?? []).find((entry) => entry.name === item.account)?.id ?? "",
      categoryId: (categories.data ?? []).find((entry) => entry.name === item.category)?.id ?? "",
    });
  };

  const saveEdit = async () => {
    if (!editingId) return;
    if (!form.amount || !form.accountId || !form.categoryId) {
      setFormError("Amount, account, and category are required.");
      return;
    }
    await api.put(`/api/transactions/${editingId}`, {
      type: form.type,
      amount: Number(form.amount),
      date: form.date,
      merchant: form.merchant,
      note: form.note,
      accountId: form.accountId,
      categoryId: form.categoryId,
      paymentMethod: "manual",
      tags: [],
    });
    setEditingId(null);
    setFormError(null);
    showAppToast("Transaction updated");
    await queryClient.invalidateQueries({ queryKey: ["transactions"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["reports"] });
  };

  const confirmDeleteTransaction = async () => {
    if (!deleteConfirmId) return;
    await api.delete(`/api/transactions/${deleteConfirmId}`);
    setDeleteConfirmId(null);
    showAppToast("Transaction deleted");
    await queryClient.invalidateQueries({ queryKey: ["transactions"] });
    await queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    await queryClient.invalidateQueries({ queryKey: ["reports"] });
  };

  const typeOptions = [
    { value: "ALL", label: "All types" },
    { value: "INCOME", label: "Income" },
    { value: "EXPENSE", label: "Expense" },
  ];

  const categoryOptions = [
    { value: "ALL", label: "All categories" },
    ...((categories.data ?? []).map((item) => ({ value: item.name, label: item.name }))),
  ];

  const accountOptions = [
    { value: "ALL", label: "All accounts" },
    ...((accounts.data ?? []).map((item) => ({ value: item.name, label: item.name }))),
  ];

  const editorAccountOptions = [{ value: "", label: "Select account" }, ...((accounts.data ?? []).map((item) => ({ value: item.id, label: item.name })))];
  const editorCategoryOptions = [{ value: "", label: "Select category" }, ...filteredCategories.map((item) => ({ value: item.id, label: item.name }))];
  const deleteTarget = filtered.find((item) => item.id === deleteConfirmId) ?? null;

  return (
    <>
      <section className="glass-panel transactions-page-shell transactions-sticky-layout">
        <div className="transactions-sticky-head">
          <div className="panel-head transactions-head">
            <div>
              <h2>Money History</h2>
              <p>Transactions list with date, type, category, account, and search filters.</p>
            </div>
            <button className="button primary transactions-add-button" type="button" onClick={() => window.dispatchEvent(new Event("open-add-transaction"))}>Add Transaction</button>
          </div>

          <div className="filters-bar structured-filters-bar transactions-filters">
            <AppDateField value={filters.date} onChange={(e) => setFilters({ ...filters, date: e.target.value })} />
            <AppSelect value={filters.type} onChange={(value) => setFilters({ ...filters, type: value })} options={typeOptions} placeholder="All types" />
            <AppSelect value={filters.category} onChange={(value) => setFilters({ ...filters, category: value })} options={categoryOptions} placeholder="All categories" />
            <AppSelect value={filters.account} onChange={(value) => setFilters({ ...filters, account: value })} options={accountOptions} placeholder="All accounts" />
            <input placeholder="Search merchant, note, category..." value={filters.search} onChange={(e) => setFilters({ ...filters, search: e.target.value })} />
          </div>
        </div>

        <div className="table-shell transactions-table-shell transactions-list-shell">
          <div className="table-row table-head table-row-transactions transactions-table-head-row">
            <span>Date</span>
            <span>Merchant</span>
            <span>Category</span>
            <span>Account</span>
            <span>Type</span>
            <span className="amount-header">Amount</span>
            <span className="actions-header">Actions</span>
          </div>
          <div className="transactions-table-body transactions-table-scrollable">
            {filtered.length ? filtered.map((item) => (
              <div key={item.id} className="table-row table-row-transactions transaction-data-row">
                <span>{item.date}</span>
                <span>{item.merchant}</span>
                <span>{item.category}</span>
                <span>{item.account}</span>
                <span><span className={`badge ${item.type === "INCOME" ? "success" : "neutral"}`}>{item.type}</span></span>
                <strong className={`transaction-amount ${item.type === "INCOME" ? "income" : "expense"}`}>{item.type === "INCOME" ? "+" : "-"}${item.amount}</strong>
                <div className="row-actions transactions-row-actions compact-row-actions compact-row-actions-inline">
                  <button className="button ghost small transaction-action-button edit-action-button" type="button" onClick={() => startEdit(item)}>Edit</button>
                  <button className="button ghost small transaction-action-button danger-button delete-action-button" type="button" onClick={() => setDeleteConfirmId(item.id)}>Delete</button>
                </div>
              </div>
            )) : <div className="empty-state">No transactions matched this search and filter combination.</div>}
          </div>
        </div>
      </section>

      {editingId ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={() => setEditingId(null)}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-form-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>Edit Transaction</h2>
                <p>Update the transaction details and save the changes.</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={() => setEditingId(null)} aria-label="Close edit modal" />
            </div>
            <div className="form-grid compact-form-grid app-form-grid edit-transaction-grid">
              <AppSelect value={form.type} onChange={(value) => setForm({ ...form, type: value as "INCOME" | "EXPENSE", categoryId: "" })} options={[{ value: "EXPENSE", label: "Expense" }, { value: "INCOME", label: "Income" }]} placeholder="Type" />
              <input placeholder="Amount" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
              <AppDateField value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
              <input placeholder="Merchant" value={form.merchant} onChange={(e) => setForm({ ...form, merchant: e.target.value })} />
              <AppSelect value={form.accountId} onChange={(value) => setForm({ ...form, accountId: value })} options={editorAccountOptions} placeholder="Select account" />
              <AppSelect value={form.categoryId} onChange={(value) => setForm({ ...form, categoryId: value })} options={editorCategoryOptions} placeholder="Select category" />
              <input className="wide-input" placeholder="Note" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
              {formError ? <p className="form-error">{formError}</p> : null}
              <div className="modal-actions edit-modal-actions">
                <button className="edit-modal-secondary" type="button" onClick={() => setEditingId(null)}>Cancel</button>
                <button className="edit-modal-primary" type="button" onClick={saveEdit}>Save changes</button>
              </div>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}

      {deleteConfirmId ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={() => setDeleteConfirmId(null)}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-confirm-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>Delete Transaction</h2>
                <p>Confirm removal of this transaction from your records.</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={() => setDeleteConfirmId(null)} aria-label="Close delete modal" />
            </div>
            <div className="delete-confirm-copy">
              <strong>{deleteTarget?.merchant ?? "Transaction"}</strong>
              <p>{deleteTarget ? `${deleteTarget.date} · ${deleteTarget.category} · ${deleteTarget.account}` : "This action cannot be undone."}</p>
            </div>
            <div className="modal-actions delete-modal-actions">
              <button className="button ghost" type="button" onClick={() => setDeleteConfirmId(null)}>Cancel</button>
              <button className="button primary delete-confirm-button" type="button" onClick={confirmDeleteTransaction}>Delete</button>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}

      {toastMessage ? <div className="toast">{toastMessage}</div> : null}
    </>
  );
}










