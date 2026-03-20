import { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { api } from "../services/api";
import { getPreferences, preferenceEventName, setPreferences, type Preferences } from "../services/preferences";
import { clearSession, getSession } from "../services/session";
import { appToastEventName, showAppToast, type AppToastDetail } from "../services/toast";
import { AppDateField, AppSelect } from "./FormControls";

type Account = { id: string; name: string };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };
type Transaction = { id: string; merchant: string; category: string; account: string; type: string; amount: string; date: string; note: string };
type Notification = { id: string; severity: "INFO" | "WARNING" | "SUCCESS" | "DANGER"; message: string; source: string; createdAt: string };

type TransactionForm = {
  type: "EXPENSE" | "INCOME";
  amount: string;
  date: string;
  merchant: string;
  accountId: string;
  categoryId: string;
  note: string;
  tags: string;
};

type TransactionFormErrors = Partial<Record<keyof TransactionForm, string>>;

const navSections = [
  { title: "Main", items: [["Dashboard", "/"], ["Transactions", "/transactions"], ["Budgets", "/budgets"], ["Goals", "/goals"], ["Reports", "/reports"]] },
  { title: "Manage", items: [["Accounts", "/accounts"], ["Recurring", "/recurring"], ["Categories", "/categories"]] },
  { title: "Tools", items: [["Settings", "/settings"]] },
] as const;

function createInitialTransactionForm(): TransactionForm {
  return {
    type: "EXPENSE",
    amount: "",
    date: new Date().toISOString().slice(0, 10),
    merchant: "",
    accountId: "",
    categoryId: "",
    note: "",
    tags: "",
  };
}

function validateTransactionForm(form: TransactionForm): TransactionFormErrors {
  const errors: TransactionFormErrors = {};
  const amount = Number(form.amount);

  if (!form.amount.trim()) {
    errors.amount = "Enter an amount.";
  } else if (Number.isNaN(amount) || amount <= 0) {
    errors.amount = "Amount must be greater than 0.";
  }

  if (!form.date) {
    errors.date = "Select a date.";
  }

  if (!form.accountId) {
    errors.accountId = "Choose an account.";
  }

  if (!form.categoryId) {
    errors.categoryId = "Choose a category.";
  }

  if (!form.merchant.trim()) {
    errors.merchant = "Enter a merchant or source.";
  }

  if (form.note.trim().length > 180) {
    errors.note = "Keep the note under 180 characters.";
  }

  if (form.tags.length > 120) {
    errors.tags = "Keep tags under 120 characters.";
  }

  return errors;
}

export function AppShell() {
  const [preferences, setLocalPreferences] = useState<Preferences>(() => getPreferences());
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [showTransactionModal, setShowTransactionModal] = useState(false);
  const [toast, setToast] = useState<{ id: number; message: string } | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [form, setForm] = useState<TransactionForm>(() => createInitialTransactionForm());
  const [formError, setFormError] = useState<string | null>(null);
  const [formErrors, setFormErrors] = useState<TransactionFormErrors>({});
  const location = useLocation();
  const queryClient = useQueryClient();
  const session = getSession();
  const avatarLabel = (session?.displayName?.slice(0, 2) ?? session?.email?.slice(0, 2) ?? "PF").toUpperCase();

  const resetTransactionModal = () => {
    setForm(createInitialTransactionForm());
    setFormError(null);
    setFormErrors({});
  };

  const openTransactionModal = () => {
    resetTransactionModal();
    setShowTransactionModal(true);
  };

  const closeTransactionModal = () => {
    setShowTransactionModal(false);
    resetTransactionModal();
  };

  const updateForm = <K extends keyof TransactionForm>(key: K, value: TransactionForm[K]) => {
    const nextValue = key === "amount" ? String(value).replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1") : value;
    setForm((current) => ({ ...current, [key]: nextValue }));
    setFormError(null);
    setFormErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
  };

  useEffect(() => {
    const refresh = () => setLocalPreferences(getPreferences());
    window.addEventListener(preferenceEventName(), refresh);
    return () => window.removeEventListener(preferenceEventName(), refresh);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = preferences.theme;
    document.documentElement.dataset.accent = preferences.accent;
  }, [preferences]);

  useEffect(() => {
    setShowProfile(false);
    setShowNotifications(false);
    setSearchTerm("");
  }, [location.pathname]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setShowProfile(false);
        setShowNotifications(false);
        closeTransactionModal();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, []);

  useEffect(() => {
    window.addEventListener("open-add-transaction", openTransactionModal);
    return () => window.removeEventListener("open-add-transaction", openTransactionModal);
  }, []);

  useEffect(() => {
    const handleToast = (event: Event) => {
      const detail = (event as CustomEvent<AppToastDetail>).detail;
      if (!detail?.message) return;
      setToast({ id: Date.now(), message: detail.message });
    };
    window.addEventListener(appToastEventName(), handleToast as EventListener);
    return () => window.removeEventListener(appToastEventName(), handleToast as EventListener);
  }, []);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 2600);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const accounts = useQuery({ queryKey: ["accounts"], queryFn: async () => (await api.get<Account[]>('/api/accounts')).data });
  const categories = useQuery({ queryKey: ["categories"], queryFn: async () => (await api.get<Category[]>('/api/categories')).data });
  const transactions = useQuery({ queryKey: ["transactions", "search"], queryFn: async () => (await api.get<Transaction[]>('/api/transactions')).data });
  const notifications = useQuery({ queryKey: ["notifications"], queryFn: async () => (await api.get<Notification[]>('/api/notifications')).data });

  const visibleNotifications = useMemo(() => (notifications.data ?? []).filter((item) => {
    if (item.source === "budget" && !preferences.budgetAlerts) return false;
    if (item.source === "recurring" && !preferences.recurringAlerts) return false;
    return true;
  }), [notifications.data, preferences.budgetAlerts, preferences.recurringAlerts]);

  const filteredCategories = (categories.data ?? []).filter((item) => item.type === form.type);
  const accountOptions = [{ value: "", label: "Select account" }, ...((accounts.data ?? []).map((item) => ({ value: item.id, label: item.name })))];
  const categoryOptions = [{ value: "", label: "Select category" }, ...filteredCategories.map((item) => ({ value: item.id, label: item.name }))];

  const pageTitle = useMemo(() => {
    const match = navSections.flatMap((section) => section.items).find(([, path]) => (path === "/" ? location.pathname === "/" : location.pathname.startsWith(path)));
    return match?.[0] ?? "Dashboard";
  }, [location.pathname]);

  const searchResults = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase();
    if (!normalized) return [];
    return (transactions.data ?? [])
      .filter((item) => [item.merchant, item.category, item.account, item.note].some((value) => value?.toLowerCase().includes(normalized)))
      .slice(0, 6);
  }, [searchTerm, transactions.data]);

  const handleSaveTransaction = async () => {
    const validationErrors = validateTransactionForm(form);
    if (Object.keys(validationErrors).length) {
      setFormErrors(validationErrors);
      setFormError("Fix the highlighted fields before saving.");
      return;
    }

    try {
      setFormError(null);
      setFormErrors({});
      await api.post('/api/transactions', {
        type: form.type,
        amount: Number(form.amount),
        date: form.date,
        merchant: form.merchant.trim(),
        accountId: form.accountId,
        categoryId: form.categoryId,
        note: form.note.trim(),
        paymentMethod: 'manual',
        tags: form.tags ? form.tags.split(',').map((item) => item.trim()).filter(Boolean) : [],
      });
      closeTransactionModal();
      showAppToast('Transaction saved');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['transactions'] }),
        queryClient.invalidateQueries({ queryKey: ['budgets'] }),
        queryClient.invalidateQueries({ queryKey: ['reports'] }),
        queryClient.invalidateQueries({ queryKey: ['notifications'] }),
      ]);
      window.setTimeout(() => setToastMessage(null), 2600);
    } catch (err: any) {
      setFormError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? 'Failed to save transaction');
    }
  };

  const clearNotifications = async () => {
    if (!visibleNotifications.length) return;
    await api.post('/api/notifications/clear', { ids: visibleNotifications.map((item) => item.id) });
    showAppToast('Notifications cleared');
    await queryClient.invalidateQueries({ queryKey: ['notifications'] });
  };

  const handleLogout = () => {
    setShowProfile(false);
    setShowNotifications(false);
    clearSession();
    window.location.href = '/auth';
  };

  const toggleTheme = () => {
    const next = { ...preferences, theme: preferences.theme === 'dark' ? 'light' : 'dark' };
    setPreferences(next);
    setLocalPreferences(next);
  };

  const openSearchResult = (id: string) => {
    setSearchTerm('');
    window.location.href = `/transactions?q=${encodeURIComponent(id)}`;
  };

  const submitSearch = () => {
    const term = searchTerm.trim();
    window.location.href = term ? `/transactions?q=${encodeURIComponent(term)}` : '/transactions';
  };

  return (
    <div className="app-frame" data-theme={preferences.theme} data-accent={preferences.accent}>
      <div className="ambient ambient-a" />
      <div className="ambient ambient-b" />
      <div className="shell shell-structured">
        <aside className="sidebar">
          <div className="brand brand-text-only"><strong className="brand-title">Personal Finance Tracker</strong></div>

          <div className="sidebar-sections">
            {navSections.map((section) => (
              <section key={section.title} className="sidebar-group">
                <p className="sidebar-label">{section.title}</p>
                <nav className="nav">
                  {section.items.map(([label, to]) => (
                    <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                      <span className="nav-dot" />
                      {label}
                    </NavLink>
                  ))}
                </nav>
              </section>
            ))}
          </div>
        </aside>

        <main className="main">
          <header className="topbar topbar-structured">
            <div className="topbar-copy">
              <h1>{pageTitle}</h1>
            </div>
            <div className="topbar-actions">
              <div className="search-box">
                <label className="search-wrap">
                  <input className="search" placeholder="Search" value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') submitSearch(); }} />
                </label>
                {searchTerm.trim() ? (
                  <div className="search-results panel-enter">
                    {searchResults.length ? searchResults.map((item) => (
                      <button key={item.id} className="search-result" onClick={() => openSearchResult(item.id)}>
                        <div>
                          <strong>{item.merchant}</strong>
                          <p>{item.category} · {item.account}</p>
                        </div>
                        <span>${item.amount}</span>
                      </button>
                    )) : <div className="search-empty">No matching transactions</div>}
                    <button className="search-footer" onClick={submitSearch}>Open filtered transactions</button>
                  </div>
                ) : null}
              </div>
              <button className="icon-button" onClick={() => setShowNotifications(true)} aria-label="Open notifications">{visibleNotifications.length}</button>
              <button className="avatar avatar-button" onClick={() => setShowProfile(true)} aria-label="Open profile panel">{avatarLabel}</button>
            </div>
          </header>

          <div className="page-enter"><Outlet /></div>

          {showTransactionModal ? (
            <div className="modal-backdrop transaction-modal-backdrop" onClick={closeTransactionModal}>
              <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card" onClick={(event) => event.stopPropagation()}>
                <div className="panel-head overlay-head">
                  <div>
                    <h2>Add Transaction</h2>
                    <p>Expense, income, or transfer with tags and notes.</p>
                  </div>
                  <button className="icon-button quiet close-icon-button" onClick={closeTransactionModal} aria-label="Close modal" />
                </div>
                <div className="transaction-type-row">
                  <label><input type="radio" checked={form.type === 'EXPENSE'} onChange={() => { updateForm('type', 'EXPENSE'); updateForm('categoryId', ''); }} /> Expense</label>
                  <label><input type="radio" checked={form.type === 'INCOME'} onChange={() => { updateForm('type', 'INCOME'); updateForm('categoryId', ''); }} /> Income</label>
                </div>
                <div className="form-grid structured-form-grid app-form-grid">
                  <div className="modal-field-wrap">
                    <input className={formErrors.amount ? "input-invalid" : ""} inputMode="decimal" placeholder="Amount" value={form.amount} onChange={(e) => updateForm('amount', e.target.value)} />
                  </div>
                  <div className="modal-field-wrap">
                    <AppDateField className={formErrors.date ? "field-invalid" : ""} value={form.date} onChange={(e) => updateForm('date', e.target.value)} />
                  </div>
                  <div className="modal-field-wrap">
                    <AppSelect className={formErrors.accountId ? "field-invalid" : ""} value={form.accountId} onChange={(value) => updateForm('accountId', value)} options={accountOptions} placeholder="Select account" />
                  </div>
                  <div className="modal-field-wrap">
                    <AppSelect key={`category-${form.type}`} className={formErrors.categoryId ? "field-invalid" : ""} value={form.categoryId} onChange={(value) => updateForm('categoryId', value)} options={categoryOptions} placeholder="Select category" />
                  </div>
                  <div className="modal-field-wrap">
                    <input className={formErrors.merchant ? "input-invalid" : ""} placeholder="Merchant" value={form.merchant} onChange={(e) => updateForm('merchant', e.target.value)} />
                  </div>
                  <div className="modal-field-wrap">
                    <input className={formErrors.note ? "input-invalid" : ""} placeholder="Note" value={form.note} onChange={(e) => updateForm('note', e.target.value)} />
                  </div>
                  <div className="modal-field-wrap wide-input">
                    <input className={formErrors.tags ? "input-invalid" : ""} placeholder="Tags (comma separated)" value={form.tags} onChange={(e) => updateForm('tags', e.target.value)} />
                  </div>
                  {formError ? <p className="form-error">{formError}</p> : null}
                  <div className="modal-actions transaction-modal-actions">
                    <button className="button ghost" onClick={closeTransactionModal}>Cancel</button>
                    <button className="button primary" onClick={handleSaveTransaction}>Save</button>
                  </div>
                </div>
              </div>
            </div>
          ) : null}

          {location.pathname === "/" ? <button className="floating-add" onClick={openTransactionModal}>+ Add Transaction</button> : null}

          {toast ? (
            <div key={toast.id} className="toast category-toast toast-centered app-shell-toast" role="status" aria-live="polite">
              <div className="category-toast-body">
                <strong>{toast.message}</strong>
                <button className="category-toast-close" type="button" onClick={() => setToast(null)} aria-label="Dismiss notification">x</button>
              </div>
              <div className="category-toast-progress" />
            </div>
          ) : null}

          {showNotifications ? (
            <div className="drawer-backdrop open drawer-scroll-shell" onClick={() => setShowNotifications(false)}>
              <aside className="notification-drawer open scrollable-drawer" onClick={(event) => event.stopPropagation()}>
                <div className="panel-head overlay-head sticky-drawer-head app-drawer-head">
                  <div>
                    <h2>Notifications</h2>
                    <p>Budget, recurring, and goal alerts.</p>
                  </div>
                  <div className="drawer-head-actions">
                    <button className="button ghost drawer-clear-button" type="button" onClick={clearNotifications} disabled={!visibleNotifications.length}>Clear</button>
                    <button className="icon-button quiet close-icon-button" onClick={() => setShowNotifications(false)} aria-label="Close notifications" />
                  </div>
                </div>
                <div className="drawer-section app-drawer-body">
                  {visibleNotifications.length ? (
                    <div className="list notification-list">
                      {visibleNotifications.map((item) => (
                        <div key={item.id} className="list-row notification-row notification-card-row">
                          <div>
                            <strong>{item.source}</strong>
                            <p>{item.message}</p>
                          </div>
                          <span className={`badge ${item.severity === 'WARNING' || item.severity === 'DANGER' ? 'warn' : item.severity === 'SUCCESS' ? 'success' : 'info'}`}>{item.severity}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="drawer-empty-state">
                      <strong>No notifications</strong>
                      <p>You're all caught up for now.</p>
                    </div>
                  )}
                </div>
              </aside>
            </div>
          ) : null}

          {showProfile ? (
            <div className="drawer-backdrop open drawer-scroll-shell" onClick={() => setShowProfile(false)}>
              <aside className="notification-drawer open profile-drawer scrollable-drawer" onClick={(event) => event.stopPropagation()}>
                <div className="panel-head overlay-head sticky-drawer-head">
                  <div>
                    <h2>Profile</h2>
                    <p>Account details and quick actions.</p>
                  </div>
                  <button className="icon-button quiet close-icon-button" onClick={() => setShowProfile(false)} aria-label="Close profile" />
                </div>
                <div className="profile-summary">
                  <div className="avatar large">{avatarLabel}</div>
                  <div>
                    <strong>{session?.displayName}</strong>
                    <p>{session?.email}</p>
                  </div>
                </div>
                <div className="preference-stack">
                  <button className="preference-card" onClick={() => { setShowProfile(false); toggleTheme(); }}>
                    <strong>Theme mode</strong>
                    <span>{preferences.theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}</span>
                  </button>
                  <button className="preference-card" onClick={() => { setShowProfile(false); window.location.href = '/settings'; }}>
                    <strong>Customize preferences</strong>
                    <span>Theme, colors, and notifications</span>
                  </button>
                  <button className="preference-card" onClick={() => { setShowProfile(false); window.location.href = '/reports'; }}>
                    <strong>View reports</strong>
                    <span>Spending trends and category summaries</span>
                  </button>
                </div>
                <div className="upgrade-actions profile-actions drawer-footer-actions">
                  <button className="button ghost" onClick={() => { setShowProfile(false); window.location.href = '/settings'; }}>Settings</button>
                  <button className="button primary" onClick={handleLogout}>Log Out</button>
                </div>
              </aside>
            </div>
          ) : null}
        </main>
      </div>
    </div>
  );
}














