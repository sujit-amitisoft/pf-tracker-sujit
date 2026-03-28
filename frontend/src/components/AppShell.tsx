import { startTransition, useEffect, useMemo, useRef, useState, type SVGProps } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { api } from "../services/api";
import { handlePermissionDenied, resolveApiError } from "../services/apiErrors";
import { getPreferences, preferenceEventName, setPreferences, type Preferences } from "../services/preferences";
import { clearSession, getSession } from "../services/session";
import { appToastEventName, showAppToast, type AppToastDetail } from "../services/toast";
import { AppDateField, AppSelect } from "./FormControls";

type Account = { id: string; name: string };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };
type Transaction = { id: string; merchant: string; category: string; account: string; type: string; amount: string; date: string; note: string; tags?: string[] };
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
  { title: "Main", items: [["Dashboard", "/"], ["Transactions", "/transactions"], ["Budgets", "/budgets"], ["Goals", "/goals"], ["Reports", "/reports"], ["Insights", "/insights"]] },
  { title: "Manage", items: [["Accounts", "/accounts"], ["Shared Accounts", "/shared-accounts"], ["Recurring", "/recurring"], ["Categories", "/categories"]] },
  { title: "Tools", items: [["Rules Engine", "/rules-engine"], ["Settings", "/settings"]] },
] as const;

function NavIcon(props: SVGProps<SVGSVGElement> & { label: string }) {
  const common = {
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.9,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
  };

  switch (props.label) {
    case "Dashboard":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><rect x="3" y="3" width="7" height="7" rx="2" {...common} /><rect x="14" y="3" width="7" height="5" rx="2" {...common} /><rect x="14" y="12" width="7" height="9" rx="2" {...common} /><rect x="3" y="14" width="7" height="7" rx="2" {...common} /></svg>;
    case "Transactions":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><path d="M4 7h14" {...common} /><path d="M4 12h10" {...common} /><path d="M4 17h8" {...common} /><path d="m18 11 2 2-2 2" {...common} /></svg>;
    case "Budgets":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><path d="M4 6h16" {...common} /><path d="M4 12h11" {...common} /><path d="M4 18h7" {...common} /><path d="M18 9v9" {...common} /><path d="M14 14h8" {...common} /></svg>;
    case "Goals":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><circle cx="12" cy="12" r="8" {...common} /><circle cx="12" cy="12" r="4" {...common} /><path d="M19 5 14.5 9.5" {...common} /></svg>;
    case "Reports":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><path d="M5 19V9" {...common} /><path d="M12 19V5" {...common} /><path d="M19 19v-7" {...common} /></svg>;
    case "Accounts":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><rect x="3" y="6" width="18" height="12" rx="3" {...common} /><path d="M3 10h18" {...common} /><path d="M7 14h3" {...common} /></svg>;
    case "Recurring":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><path d="M20 11a8 8 0 0 0-14.9-3" {...common} /><path d="M4 5v4h4" {...common} /><path d="M4 13a8 8 0 0 0 14.9 3" {...common} /><path d="M20 19v-4h-4" {...common} /></svg>;
    case "Categories":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><path d="M4 7h7" {...common} /><path d="M4 12h16" {...common} /><path d="M4 17h11" {...common} /><circle cx="16.5" cy="7" r="1.5" fill="currentColor" stroke="none" /></svg>;
    case "Settings":
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><circle cx="12" cy="12" r="3" {...common} /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1.04 1.55V21a2 2 0 0 1-4 0v-.09a1.7 1.7 0 0 0-1.04-1.55 1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.55-1.04H3a2 2 0 0 1 0-4h.09A1.7 1.7 0 0 0 4.64 8.4a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.87.34H9a1.7 1.7 0 0 0 1.04-1.55V3a2 2 0 0 1 4 0v.09A1.7 1.7 0 0 0 15.08 4h.05a1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.4 8.4v.05A1.7 1.7 0 0 0 20.95 9H21a2 2 0 0 1 0 4h-.09A1.7 1.7 0 0 0 19.4 15Z" {...common} /></svg>;
    default:
      return <svg viewBox="0 0 24 24" aria-hidden="true" {...props}><circle cx="12" cy="12" r="7" {...common} /></svg>;
  }
}

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
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [notificationPopups, setNotificationPopups] = useState<Array<{ id: string; message: string; severity: Notification["severity"]; source: string }>>([]);
  const notificationSeenIds = useRef<Set<string>>(new Set());
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
  const [showEditProfile, setShowEditProfile] = useState(false);
  const [profileForm, setProfileForm] = useState({ displayName: session?.displayName ?? "" });
  const [profileError, setProfileError] = useState<string | null>(null);
  const [avatarBust, setAvatarBust] = useState(0);
  const searchBoxRef = useRef<HTMLDivElement | null>(null);

  const me = useQuery({
    queryKey: ["me"],
    queryFn: async () => (await api.get<{ id: string; email: string; displayName: string; hasAvatar: boolean }>("/api/users/me")).data,
  });

  const avatarUrl = me.data?.hasAvatar ? `/api/users/me/avatar?t=${avatarBust}` : null;

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
    document.documentElement.dataset.theme = preferences.theme === "dark" && preferences.amoledDark ? "amoled" : preferences.theme;
    document.documentElement.dataset.accent = preferences.accent;
  }, [preferences]);

  useEffect(() => {
    setShowProfile(false);
    setShowEditProfile(false);
    setShowNotifications(false);
    setSidebarOpen(false);
    setSearchTerm("");
    setProfileError(null);
  }, [location.pathname]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setShowProfile(false);
        setShowNotifications(false);
        setSidebarOpen(false);
        setSearchTerm("");
        closeTransactionModal();
      }
    };

    const onPointerDown = (event: MouseEvent) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(event.target as Node)) {
        setSearchTerm("");
      }
    };

    window.addEventListener("keydown", onKeyDown);
    window.addEventListener("mousedown", onPointerDown);
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      window.removeEventListener("mousedown", onPointerDown);
    };
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

  useEffect(() => {
    if (!preferences.popupNotifications) return;
    const seen = notificationSeenIds.current;
    const newlyAdded = visibleNotifications.filter((item) => !seen.has(item.id));
    newlyAdded.forEach((item) => seen.add(item.id));

    if (!newlyAdded.length) return;

    setNotificationPopups((current) => {
      const merged = [...newlyAdded.map((item) => ({ id: item.id, message: item.message, severity: item.severity, source: item.source })), ...current];
      return merged.slice(0, 3);
    });
  }, [preferences.popupNotifications, visibleNotifications]);

  useEffect(() => {
    if (!notificationPopups.length) return;
    const timers = notificationPopups.map((popup) => window.setTimeout(() => {
      setNotificationPopups((current) => current.filter((item) => item.id !== popup.id));
    }, 4200));
    return () => timers.forEach((timer) => window.clearTimeout(timer));
  }, [notificationPopups]);

  const filteredCategories = (categories.data ?? []).filter((item) => item.type === form.type);
  const accountOptions = [{ value: "", label: "Select account" }, ...((accounts.data ?? []).map((item) => ({ value: item.id, label: item.name })))];
  const categoryOptions = [{ value: "", label: "Select category" }, ...filteredCategories.map((item) => ({ value: item.id, label: item.name }))];

  const pageTitle = useMemo(() => {
    const match = navSections.flatMap((section) => section.items).find(([, path]) => (path === "/" ? location.pathname === "/" : location.pathname.startsWith(path)));
    return match?.[0] ?? "Dashboard";
  }, [location.pathname]);
  const navMatches = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase();
    if (!normalized) return [] as Array<{ label: string; path: string }>;
    return navSections
      .flatMap((section) => section.items)
      .map(([label, path]) => ({ label, path }))
      .filter((item) => item.label.toLowerCase().includes(normalized))
      .slice(0, 6);
  }, [searchTerm]);

  const transactionMatches = useMemo(() => {
    const normalized = searchTerm.trim().toLowerCase();
    if (!normalized) return [] as Transaction[];
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
      const { data } = await api.post<Transaction>('/api/transactions', {
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
      queryClient.setQueryData<Transaction[]>(['transactions'], (current) => data ? [data, ...(current ?? [])] : current ?? []);
      showAppToast('Transaction saved');
      startTransition(() => {
        void Promise.all([
          queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
          queryClient.invalidateQueries({ queryKey: ['transactions'] }),
          queryClient.invalidateQueries({ queryKey: ['budgets'] }),
          queryClient.invalidateQueries({ queryKey: ['reports'] }),
          queryClient.invalidateQueries({ queryKey: ['notifications'] }),
        ]);
      });
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

  const saveProfile = async () => {
    const displayName = profileForm.displayName.trim();
    if (!displayName) {
      setProfileError("Display name is required.");
      return;
    }

    try {
      setProfileError(null);
      const { data } = await api.patch<{ id: string; email: string; displayName: string; hasAvatar: boolean }>("/api/users/me", { displayName });
      const current = getSession();
      if (current) {
        // Update local session so UI updates immediately.
        window.localStorage.setItem("finance_session", JSON.stringify({ ...current, displayName: data.displayName }));
      }
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      showAppToast("Profile updated");
      setShowEditProfile(false);
    } catch (err: any) {
      setProfileError(err?.response?.data?.message ?? "Failed to update profile");
    }
  };

  const uploadAvatar = async (file: File) => {
    try {
      setProfileError(null);
      const formData = new FormData();
      formData.append("file", file);
      await api.post("/api/users/me/avatar", formData, { headers: { "Content-Type": "multipart/form-data" } });
      setAvatarBust(Date.now());
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      showAppToast("Avatar updated");
    } catch (err: any) {
      setProfileError(err?.response?.data?.message ?? "Failed to upload avatar");
    }
  };

  const removeAvatar = async () => {
    try {
      setProfileError(null);
      await api.delete("/api/users/me/avatar");
      setAvatarBust(Date.now());
      await queryClient.invalidateQueries({ queryKey: ["me"] });
      showAppToast("Avatar removed");
    } catch (err: any) {
      setProfileError(err?.response?.data?.message ?? "Failed to remove avatar");
    }
  };

  const openSearchResult = (merchant: string) => {
    setSearchTerm("");
    window.location.href = `/transactions?q=${encodeURIComponent(merchant)}`;
  };

  const submitSearch = () => {
    const term = searchTerm.trim();
    const normalized = term.toLowerCase();
    const exactNav = navMatches.length === 1 && navMatches[0].label.toLowerCase().startsWith(normalized) && normalized.length >= 2;
    if (exactNav) {
      window.location.href = navMatches[0].path;
      return;
    }
    window.location.href = term ? `/transactions?q=${encodeURIComponent(term)}` : "/transactions";
  };

  return (
    <div className="app-frame" data-theme={preferences.theme === "dark" && preferences.amoledDark ? "amoled" : preferences.theme} data-accent={preferences.accent}>
      <div className="ambient ambient-a" />
      <div className="ambient ambient-b" />
      <div className="shell shell-structured">
        {sidebarOpen ? <div className="sidebar-backdrop open" onClick={() => setSidebarOpen(false)} /> : null}
        <aside className={sidebarOpen ? "sidebar sidebar-drawer open" : "sidebar sidebar-drawer"} onClick={(event) => event.stopPropagation()}>
          <div className="brand brand-text-only"><strong className="brand-title">Personal Finance Tracker</strong></div>

          <div className="sidebar-sections">
            {navSections.map((section) => (
              <section key={section.title} className="sidebar-group">
                <p className="sidebar-label">{section.title}</p>
                <nav className="nav">
                  {section.items.map(([label, to]) => (
                    <NavLink key={to} to={to} end={to === '/'} onClick={() => setSidebarOpen(false)} className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
                      <span className="nav-icon-wrap"><NavIcon label={label} className="nav-icon" /></span>
                      <span className="nav-dot" />
                      <span className="nav-text">{label}</span>
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
              <button className="icon-button quiet hamburger-button" type="button" onClick={() => setSidebarOpen((current) => !current)} aria-label="Toggle menu" />
              <div className="search-box" ref={searchBoxRef}>
                <label className="search-wrap">
                  <input className="search" placeholder="Search" value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') submitSearch(); }} />
                </label>
                {searchTerm.trim() ? (
                  <div className="search-results panel-enter">
                    {navMatches.length ? (
                      <>
                        <div className="search-section-title">Navigate</div>
                        {navMatches.map((item) => (
                          <button
                            key={item.path}
                            className="search-result search-result-nav"
                            onClick={() => {
                              setSearchTerm("");
                              window.location.href = item.path;
                            }}
                          >
                            <div>
                              <strong>{item.label}</strong>
                            </div>
                          </button>
                        ))}
                      </>
                    ) : null}

                    {transactionMatches.length ? (
                      <>
                        <div className="search-section-title">Transactions</div>
                        {transactionMatches.map((item) => (
                          <button key={item.id} className="search-result" onClick={() => openSearchResult(item.merchant)}>
                            <div>
                              <strong>{item.merchant}</strong>
                              <p>{item.category} Â· {item.account}</p>
                            </div>
                            <span>${item.amount}</span>
                          </button>
                        ))}
                      </>
                    ) : null}

                    {!navMatches.length && !transactionMatches.length ? <div className="search-empty">No matches</div> : null}
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

          {notificationPopups.length ? (
            <div className="notification-popups" role="status" aria-live="polite">
              {notificationPopups.map((popup) => (
                <div key={popup.id} className={`notification-popup ${popup.severity.toLowerCase()}`}>
                  <div>
                    <strong>{popup.source.replace(/(^|\s)\S/g, (match) => match.toUpperCase())}</strong>
                    <p>{popup.message}</p>
                  </div>
                  <button className="icon-button quiet close-icon-button popup-close" type="button" onClick={() => setNotificationPopups((current) => current.filter((item) => item.id !== popup.id))} aria-label="Dismiss notification" />
                  <div className="popup-progress" />
                </div>
              ))}
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


          {showEditProfile ? (
            <div className="modal-backdrop transaction-modal-backdrop" onClick={() => setShowEditProfile(false)}>
              <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card profile-edit-modal" onClick={(event) => event.stopPropagation()}>
                <div className="panel-head overlay-head">
                  <div>
                    <h2>Edit Profile</h2>
                    <p>Update your display name and profile picture.</p>
                  </div>
                  <button className="icon-button quiet close-icon-button" type="button" onClick={() => setShowEditProfile(false)} aria-label="Close edit profile" />
                </div>
                <div className="form-grid structured-form-grid app-form-grid">
                  <div className="profile-edit-avatar-row">
                    {avatarUrl ? <img className="avatar-image avatar-image-xl" src={avatarUrl} alt="Profile avatar" /> : <div className="avatar avatar-xl">{avatarLabel}</div>}
                    <div className="profile-edit-avatar-actions">
                      <label className="button ghost profile-upload-button">
                        Upload
                        <input type="file" accept="image/*" onChange={(event) => { const file = event.target.files?.[0]; if (file) uploadAvatar(file); event.currentTarget.value = ''; }} />
                      </label>
                      <button className="button ghost danger-button" type="button" onClick={removeAvatar} disabled={!me.data?.hasAvatar}>Remove</button>
                    </div>
                  </div>

                  <input placeholder="Display name" value={profileForm.displayName} onChange={(e) => setProfileForm({ displayName: e.target.value })} />
                  {profileError ? <p className="form-error">{profileError}</p> : null}
                  <div className="modal-actions">
                    <button className="button ghost" type="button" onClick={() => setShowEditProfile(false)}>Cancel</button>
                    <button className="button primary" type="button" onClick={saveProfile}>Save</button>
                  </div>
                </div>
              </div>
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
                  {avatarUrl ? <img className="avatar-image avatar-image-large" src={avatarUrl} alt="Profile avatar" /> : <div className="avatar large">{avatarLabel}</div>}
                  <div>
                    <strong>{session?.displayName}</strong>
                    <p>{session?.email}</p>
                  </div>
                </div>
                                <div className="preference-stack">
                  <button className="preference-card preference-card-compact" onClick={() => { setShowProfile(false); setProfileForm({ displayName: (me.data?.displayName ?? session?.displayName ?? "").trim() }); setProfileError(null); setShowEditProfile(true); }}>
                    <strong>Edit profile</strong>
                    <span>Update display name and profile picture</span>
                  </button>
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





















































