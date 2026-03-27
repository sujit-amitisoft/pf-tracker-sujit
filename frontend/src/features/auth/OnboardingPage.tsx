import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { api } from "../../services/api";
import { getPreferences } from "../../services/preferences";
import { AppSelect } from "../../components/FormControls";

type AccountResponse = {
  id: string;
  name: string;
  type: string;
  currentBalance: string;
  institutionName: string;
  shared: boolean;
  accessRole: string;
  memberCount?: number;
};

type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };

type AccountForm = {
  name: string;
  type: string;
  openingBalance: string;
  institutionName: string;
};

type BudgetForm = {
  categoryId: string;
  accountId: string;
  amount: string;
  alertThresholdPercent: string;
};

function createInitialAccountForm(): AccountForm {
  return {
    name: "",
    type: "BANK_ACCOUNT",
    openingBalance: "0",
    institutionName: "",
  };
}

function createInitialBudgetForm(): BudgetForm {
  return {
    categoryId: "",
    accountId: "ALL",
    amount: "",
    alertThresholdPercent: "80",
  };
}

export function OnboardingPage() {
  const navigate = useNavigate();
  const preferences = getPreferences();
  const theme = preferences.theme === "dark" && preferences.amoledDark ? "amoled" : preferences.theme;
  const now = new Date();
  const [step, setStep] = useState<"account" | "budget">("account");
  const [accountForm, setAccountForm] = useState<AccountForm>(() => createInitialAccountForm());
  const [budgetForm, setBudgetForm] = useState<BudgetForm>(() => createInitialBudgetForm());
  const [createdAccount, setCreatedAccount] = useState<AccountResponse | null>(null);
  const [accountError, setAccountError] = useState<string | null>(null);
  const [budgetError, setBudgetError] = useState<string | null>(null);
  const [accountLoading, setAccountLoading] = useState(false);
  const [budgetLoading, setBudgetLoading] = useState(false);

  const categories = useQuery({
    queryKey: ["categories", "onboarding"],
    queryFn: async () => (await api.get<Category[]>("/api/categories")).data,
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

  const budgetCategoryOptions = useMemo(
    () => [
      { value: "", label: "Select expense category" },
      ...((categories.data ?? [])
        .filter((item) => item.type === "EXPENSE")
        .map((item) => ({ value: item.id, label: item.name }))),
    ],
    [categories.data],
  );

  const budgetAccountOptions = useMemo(() => {
    if (!createdAccount) {
      return [{ value: "ALL", label: "All accessible accounts" }];
    }
    return [
      { value: createdAccount.id, label: `${createdAccount.name} (${createdAccount.accessRole})` },
      { value: "ALL", label: "All accessible accounts" },
    ];
  }, [createdAccount]);

  const budgetThresholdOptions = [
    { value: "80", label: "Alert at 80%" },
    { value: "100", label: "Alert at 100%" },
    { value: "120", label: "Alert at 120%" },
  ];

  const updateAccountForm = (key: keyof AccountForm, value: string) => {
    const nextValue = key === "openingBalance" ? value.replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1") : value;
    setAccountForm((current) => ({ ...current, [key]: nextValue }));
    setAccountError(null);
  };

  const updateBudgetForm = (key: keyof BudgetForm, value: string) => {
    const nextValue = key === "amount" ? value.replace(/[^0-9.]/g, "").replace(/(\..*)\./g, "$1") : value;
    setBudgetForm((current) => ({ ...current, [key]: nextValue }));
    setBudgetError(null);
  };

  const createAccount = async () => {
    if (!accountForm.name.trim()) {
      setAccountError("Enter an account name to continue.");
      return;
    }

    setAccountLoading(true);
    setAccountError(null);
    try {
      const payload = {
        name: accountForm.name.trim(),
        type: accountForm.type,
        openingBalance: accountForm.openingBalance ? Number(accountForm.openingBalance) : 0,
        institutionName: accountForm.institutionName.trim() || accountForm.name.trim(),
      };
      const { data } = await api.post<AccountResponse>("/api/accounts", payload);
      setCreatedAccount(data);
      setBudgetForm((current) => ({
        ...current,
        accountId: data.id,
      }));
      setStep("budget");
    } catch (err: any) {
      setAccountError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? "Failed to create account");
    } finally {
      setAccountLoading(false);
    }
  };

  const createBudget = async () => {
    if (!budgetForm.categoryId || !budgetForm.amount) {
      setBudgetError("Select a category and enter a monthly budget amount.");
      return;
    }

    setBudgetLoading(true);
    setBudgetError(null);
    try {
      await api.post("/api/budgets", {
        categoryId: budgetForm.categoryId,
        accountId: budgetForm.accountId === "ALL" ? null : budgetForm.accountId,
        month: now.getMonth() + 1,
        year: now.getFullYear(),
        amount: Number(budgetForm.amount),
        alertThresholdPercent: Number(budgetForm.alertThresholdPercent),
      });
      navigate("/", { replace: true });
    } catch (err: any) {
      setBudgetError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? "Failed to create budget");
    } finally {
      setBudgetLoading(false);
    }
  };

  return (
    <div className="auth-screen auth-screen-onboarding" data-theme={theme}>
      <div className="auth-orb auth-orb-a" />
      <div className="auth-orb auth-orb-b" />
      <section className="auth-hero glass-panel onboarding-hero onboarding-flow-shell">
        <div className="auth-copy onboarding-copy">
          <div>
            <p className="eyebrow">Onboarding</p>
            <h1>{step === "account" ? "Create your first account" : "Set your first monthly budget"}</h1>
          </div>
          <p className="auth-subcopy">
            {step === "account"
              ? "Start with the main account that will receive income and track spending. You can add more accounts later."
              : "Add an optional monthly budget now so your dashboard and alerts start with meaningful limits from day one."}
          </p>
          <div className="onboarding-progress">
            <div className={`onboarding-progress-step ${step === "account" ? "active" : "complete"}`}>
              <span>1</span>
              <strong>Account</strong>
            </div>
            <div className={`onboarding-progress-step ${step === "budget" ? "active" : ""}`}>
              <span>2</span>
              <strong>Budget</strong>
            </div>
          </div>
          <div className="auth-highlight-grid onboarding-highlight-grid">
            <article className="auth-highlight">
              <strong>{createdAccount?.name ?? "Your main account"}</strong>
              <span>{createdAccount ? `${createdAccount.type.replace(/_/g, " ")} ready for tracking.` : "Add a bank account, wallet, credit card, or savings account."}</span>
            </article>
            <article className="auth-highlight">
              <strong>{step === "budget" ? "Budget is optional" : "Monthly budget next"}</strong>
              <span>{step === "budget" ? "Skip this step anytime and finish setup on the dashboard." : "After the account is ready, you can add one starter monthly budget."}</span>
            </article>
          </div>
        </div>

        <div className="auth-form-card onboarding-form-card">
          {step === "account" ? (
            <div className="form-grid auth-form-grid onboarding-form-grid">
              <div className="onboarding-step-head">
                <strong>Step 1 of 2</strong>
                <span>Create the account you want to use first.</span>
              </div>
              <input placeholder="Account name" value={accountForm.name} onChange={(e) => updateAccountForm("name", e.target.value)} />
              <AppSelect value={accountForm.type} onChange={(value) => updateAccountForm("type", value)} options={accountTypeOptions} placeholder="Select account type" />
              <input placeholder="Opening balance" inputMode="decimal" value={accountForm.openingBalance} onChange={(e) => updateAccountForm("openingBalance", e.target.value)} />
              <input placeholder="Institution name" value={accountForm.institutionName} onChange={(e) => updateAccountForm("institutionName", e.target.value)} />
              {accountError ? <p className="form-error">{accountError}</p> : null}
              <div className="onboarding-actions">
                <button className="button ghost" type="button" onClick={() => navigate("/", { replace: true })}>Skip for now</button>
                <button className="button primary" type="button" onClick={createAccount} disabled={accountLoading}>{accountLoading ? "Creating..." : "Continue"}</button>
              </div>
            </div>
          ) : (
            <div className="form-grid auth-form-grid onboarding-form-grid">
              <div className="onboarding-step-head">
                <strong>Step 2 of 2</strong>
                <span>Add one budget now or skip and finish setup.</span>
              </div>
              <AppSelect value={budgetForm.categoryId} onChange={(value) => updateBudgetForm("categoryId", value)} options={budgetCategoryOptions} placeholder="Select expense category" />
              <AppSelect value={budgetForm.accountId} onChange={(value) => updateBudgetForm("accountId", value)} options={budgetAccountOptions} placeholder="Select budget account" />
              <input placeholder="Monthly budget amount" inputMode="decimal" value={budgetForm.amount} onChange={(e) => updateBudgetForm("amount", e.target.value)} />
              <AppSelect value={budgetForm.alertThresholdPercent} onChange={(value) => updateBudgetForm("alertThresholdPercent", value)} options={budgetThresholdOptions} placeholder="Alert at 80%" />
              {budgetError ? <p className="form-error">{budgetError}</p> : null}
              <div className="onboarding-actions">
                <button className="button ghost" type="button" onClick={() => setStep("account")}>Back</button>
                <div className="onboarding-actions-right">
                  <button className="button ghost" type="button" onClick={() => navigate("/", { replace: true })}>Skip for now</button>
                  <button className="button primary" type="button" onClick={createBudget} disabled={budgetLoading || categories.isLoading}>{budgetLoading ? "Saving..." : "Finish setup"}</button>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}




