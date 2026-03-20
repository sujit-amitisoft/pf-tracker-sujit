import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/AppShell";
import { AuthPage } from "./features/auth/AuthPage";
import { OnboardingPage } from "./features/auth/OnboardingPage";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { TransactionsPage } from "./features/transactions/TransactionsPage";
import { BudgetsPage } from "./features/budgets/BudgetsPage";
import { GoalsPage } from "./features/goals/GoalsPage";
import { ReportsPage } from "./features/reports/ReportsPage";
import { RecurringPage } from "./features/recurring/RecurringPage";
import { AccountsPage } from "./features/accounts/AccountsPage";
import { CategoriesPage } from "./features/categories/CategoriesPage";
import { SettingsPage } from "./features/settings/SettingsPage";
import { isAuthenticated } from "./services/session";

function ProtectedRoutes() {
  if (!isAuthenticated()) {
    return <Navigate to="/auth" replace />;
  }
  return <AppShell />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/auth" element={isAuthenticated() ? <Navigate to="/" replace /> : <AuthPage />} />
        <Route path="/onboarding" element={isAuthenticated() ? <OnboardingPage /> : <Navigate to="/auth" replace />} />
        <Route path="/" element={<ProtectedRoutes />}>
          <Route index element={<DashboardPage />} />
          <Route path="transactions" element={<TransactionsPage />} />
          <Route path="budgets" element={<BudgetsPage />} />
          <Route path="goals" element={<GoalsPage />} />
          <Route path="reports" element={<ReportsPage />} />
          <Route path="recurring" element={<RecurringPage />} />
          <Route path="accounts" element={<AccountsPage />} />
          <Route path="categories" element={<CategoriesPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
        <Route path="*" element={<Navigate to={isAuthenticated() ? "/" : "/auth"} replace />} />
      </Routes>
    </BrowserRouter>
  );
}
