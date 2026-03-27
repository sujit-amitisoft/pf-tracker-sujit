import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { api } from "../../services/api";
import { AppDateField, AppSelect } from "../../components/FormControls";

type ReportRow = { id: string; date: string; merchant: string; category: string; account: string; type: "INCOME" | "EXPENSE"; amount: string; note: string };
type Summary = { totalIncome: string; totalExpense: string; net: string; transactionCount: number; topCategory: string };
type Account = { id: string; name: string };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };
type TrendPoint = { period: string; income: number; expense: number };
type SavingsRatePoint = { period: string; savingsRate: number };
type NetWorthPoint = { period: string; netWorth: number };
type CategorySpend = { category: string; amount: number };

type RangeOption = "THIS_MONTH" | "LAST_3_MONTHS" | "THIS_YEAR" | "CUSTOM";

function buildParams(range: RangeOption, accountId: string, type: string, categoryId: string, startDate: string, endDate: string) {
  const params = new URLSearchParams();
  params.set("range", range);
  if (accountId !== "ALL") params.set("accountId", accountId);
  if (type !== "ALL") params.set("type", type);
  if (categoryId !== "ALL") params.set("categoryId", categoryId);
  if (range === "CUSTOM" && startDate) params.set("startDate", startDate);
  if (range === "CUSTOM" && endDate) params.set("endDate", endDate);
  return params.toString();
}

export function ReportsPage() {
  const navigate = useNavigate();
  const [range, setRange] = useState<RangeOption>("THIS_MONTH");
  const [accountId, setAccountId] = useState("ALL");
  const [type, setType] = useState("ALL");
  const [categoryId, setCategoryId] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const accounts = useQuery({ queryKey: ["accounts", "reports"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });
  const categories = useQuery({ queryKey: ["categories", "reports"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });
  const query = useMemo(() => buildParams(range, accountId, type, categoryId, startDate, endDate), [range, accountId, type, categoryId, startDate, endDate]);

  const summary = useQuery({ queryKey: ["reports", "summary", query], queryFn: async () => (await api.get<Summary>(`/api/reports/summary?${query}`)).data });
  const transactions = useQuery({ queryKey: ["reports", "transactions", query], queryFn: async () => (await api.get<ReportRow[]>(`/api/reports/transactions?${query}`)).data });
  const categorySpend = useQuery({ queryKey: ["reports", "category-spend", query], queryFn: async () => (await api.get<CategorySpend[]>(`/api/reports/category-spend?${query}`)).data });
  const incomeExpense = useQuery({ queryKey: ["reports", "income-vs-expense", query], queryFn: async () => (await api.get<TrendPoint[]>(`/api/reports/income-vs-expense?${query}`)).data });
  const savingsTrend = useQuery({ queryKey: ["reports", "trends", query], queryFn: async () => (await api.get<SavingsRatePoint[]>(`/api/reports/trends?${query}`)).data });
  const netWorth = useQuery({ queryKey: ["reports", "net-worth", query], queryFn: async () => (await api.get<NetWorthPoint[]>(`/api/reports/net-worth?${query}`)).data });

  const rangeOptions = [
    { value: "THIS_MONTH", label: "This Month" },
    { value: "LAST_3_MONTHS", label: "Last 3 Months" },
    { value: "THIS_YEAR", label: "This Year" },
    { value: "CUSTOM", label: "Custom Range" },
  ];
  const accountOptions = useMemo(() => [{ value: "ALL", label: "All accounts" }, ...((accounts.data ?? []).map((item) => ({ value: item.id, label: item.name })))], [accounts.data]);
  const categoryOptions = useMemo(() => [{ value: "ALL", label: "All categories" }, ...((categories.data ?? []).map((item) => ({ value: item.id, label: item.name })))], [categories.data]);
  const typeOptions = [
    { value: "ALL", label: "All types" },
    { value: "INCOME", label: "Income" },
    { value: "EXPENSE", label: "Expense" },
  ];

  const maxTrend = Math.max(1, ...((incomeExpense.data ?? []).flatMap((point) => [Number(point.income), Number(point.expense)])));
  const maxSpend = Math.max(1, ...((categorySpend.data ?? []).map((item) => Number(item.amount))));
  const maxSavingsRate = Math.max(1, ...((savingsTrend.data ?? []).map((item) => Number(item.savingsRate))));
  const maxNetWorth = Math.max(1, ...((netWorth.data ?? []).map((item) => Number(item.netWorth))));

  const exportCsv = () => {
    window.open(`${api.defaults.baseURL}/api/reports/export/csv?${query}`, "_blank");
  };

  return (
    <section className="glass-panel reports-page-shell">
      <div className="panel-head reports-head">
        <div><h2>Insight Center</h2><p>Filtered spend, trend, and key findings powered by report APIs.</p></div>
        <div className="reports-export-actions">
          <button className="button primary reports-export-button reports-open-insights-button" type="button" onClick={() => navigate("/insights")}>Open Insights</button>
          <button className="button primary reports-export-button" type="button" onClick={exportCsv}>Export CSV</button>
          <button className="button primary reports-export-button" type="button" onClick={async () => { const response = await api.get(`/api/reports/export/pdf?${query}`, { responseType: "blob" }); const url = URL.createObjectURL(response.data); const link = document.createElement("a"); link.href = url; link.download = "finance-report.pdf"; link.click(); URL.revokeObjectURL(url); }}>Export PDF</button>
        </div>
      </div>
      <div className="filters-bar structured-filters-bar reports-filters reports-filters-wide">
        <AppSelect value={range} onChange={(value) => setRange(value as RangeOption)} options={rangeOptions} placeholder="This Month" />
        <AppSelect value={accountId} onChange={setAccountId} options={accountOptions} placeholder="All accounts" />
        <AppSelect value={type} onChange={setType} options={typeOptions} placeholder="All types" />
        <AppSelect value={categoryId} onChange={setCategoryId} options={categoryOptions} placeholder="All categories" />
        {range === "CUSTOM" ? <AppDateField value={startDate} onChange={(event) => setStartDate(event.target.value)} placeholder="From date" /> : null}
        {range === "CUSTOM" ? <AppDateField value={endDate} onChange={(event) => setEndDate(event.target.value)} placeholder="To date" /> : null}
      </div>

      <div className="summary-row finance-summary-grid reports-summary-row reports-summary-cards">
        <article className="summary-card finance-summary-card income-card"><span>Total Income</span><strong>${summary.data?.totalIncome ?? "0.00"}</strong></article>
        <article className="summary-card finance-summary-card expense-card"><span>Total Expense</span><strong>${summary.data?.totalExpense ?? "0.00"}</strong></article>
        <article className="summary-card finance-summary-card savings-card"><span>Net</span><strong>${summary.data?.net ?? "0.00"}</strong></article>
        <article className="summary-card finance-summary-card goal-card"><span>Transactions</span><strong>{summary.data?.transactionCount ?? 0}</strong></article>
        <article className="summary-card finance-summary-card balance-card"><span>Top Category</span><strong>{summary.data?.topCategory ?? "None"}</strong></article>
      </div>

      <div className="report-grid structured-report-grid reports-grid">
        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Category Spend</h2><p>Expense totals inside the selected filters.</p></div></div>
          <div className="chart-placeholder report-chart-list">
            {(categorySpend.data ?? []).length ? (categorySpend.data ?? []).map((item) => (
              <div key={item.category} className="chart-list-row report-chart-row">
                <span>{item.category}</span>
                <div className="report-bar-stack">
                  <div className="mini-bar" title={`$${Number(item.amount).toFixed(2)} of $${maxSpend.toFixed(2)}`}><span style={{ width: `${(Number(item.amount) / maxSpend) * 100}%` }} /></div>
                  <div className="progress-caption"><span>$0</span><span>${Number(item.amount).toFixed(0)} / ${maxSpend.toFixed(0)}</span><span>Max</span></div>
                </div>
                <strong>${Number(item.amount).toFixed(2)}</strong>
              </div>
            )) : <div className="empty-state">No category spend found for these filters.</div>}
          </div>
        </section>
        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Income vs Expense Trend</h2><p>Monthly comparison within the selected range.</p></div></div>
          <div className="trend-list report-trend-list">
            {(incomeExpense.data ?? []).length ? (incomeExpense.data ?? []).map((point) => (
              <div key={point.period} className="trend-row report-trend-row">
                <span>{point.period}</span>
                <div className="report-bar-stack">
                  <div className="trend-bars">
                    <div className="trend-bar income" title={`Income $${Number(point.income).toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(Number(point.income) / maxTrend) * 100}%` }} /></div>
                    <div className="trend-bar expense" title={`Expense $${Number(point.expense).toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(Number(point.expense) / maxTrend) * 100}%` }} /></div>
                  </div>
                  <div className="progress-caption"><span>Income $${Number(point.income).toFixed(0)}</span><span>Max $${maxTrend.toFixed(0)}</span><span>Expense $${Number(point.expense).toFixed(0)}</span></div>
                </div>
              </div>
            )) : <div className="empty-state">No trend data found for these filters.</div>}
          </div>
        </section>
      </div>

      <div className="report-grid structured-report-grid reports-grid reports-grid-bottom">
        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Savings Rate Trend</h2><p>Track how efficiently income is turning into savings.</p></div></div>
          <div className="trend-list report-trend-list">
            {(savingsTrend.data ?? []).map((point) => (
              <div key={point.period} className="trend-row report-trend-row">
                <span>{point.period}</span>
                <div className="report-bar-stack">
                  <div className="trend-bar income" title={`${Number(point.savingsRate).toFixed(2)}%`}><span style={{ width: `${(Number(point.savingsRate) / maxSavingsRate) * 100}%` }} /></div>
                  <div className="progress-caption"><span>0%</span><span>{Number(point.savingsRate).toFixed(0)}%</span><span>Max {maxSavingsRate.toFixed(0)}%</span></div>
                </div>
              </div>
            ))}
          </div>
        </section>
        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Net Worth Tracking</h2><p>Monitor account growth across recent months.</p></div></div>
          <div className="chart-placeholder report-chart-list">
            {(netWorth.data ?? []).map((point) => (
              <div key={point.period} className="chart-list-row report-chart-row">
                <span>{point.period}</span>
                <div className="report-bar-stack">
                  <div className="mini-bar" title={`$${Number(point.netWorth).toFixed(2)} of $${maxNetWorth.toFixed(2)}`}><span style={{ width: `${(Number(point.netWorth) / maxNetWorth) * 100}%` }} /></div>
                  <div className="progress-caption"><span>$0</span><span>${Number(point.netWorth).toFixed(0)}</span><span>Peak ${maxNetWorth.toFixed(0)}</span></div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section className="glass-panel nested-panel report-card report-transactions-card">
        <div className="panel-head"><div><h2>Report Transactions</h2><p>Detailed rows for the same filters used in the charts.</p></div></div>
        <div className="table-shell transactions-table-shell reports-table-shell">
          <div className="table-row table-head table-row-transactions">
            <span>Date</span><span>Merchant</span><span>Category</span><span>Account</span><span>Type</span><span>Amount</span><span>Note</span>
          </div>
          <div className="transactions-table-body reports-table-body">
            {(transactions.data ?? []).length ? (transactions.data ?? []).map((item) => (
              <div key={item.id} className="table-row table-row-transactions transaction-data-row">
                <span>{item.date}</span><span>{item.merchant}</span><span>{item.category}</span><span>{item.account}</span><span>{item.type}</span><strong>${item.amount}</strong><span>{item.note || "-"}</span>
              </div>
            )) : <div className="empty-state">No transactions matched this report filter.</div>}
          </div>
        </div>
      </section>
    </section>
  );
}



