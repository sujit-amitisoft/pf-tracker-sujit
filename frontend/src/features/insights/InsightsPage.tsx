import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../../services/api";

type HealthFactor = { label: string; score: number; detail: string };
type HealthScore = { score: number; factors: HealthFactor[]; suggestions: string[] };
type InsightCard = { title: string; message: string; severity: string };
type InsightSummary = { currentMonthSavingsRate: string; previousMonthSavingsRate: string; cards: InsightCard[] };
type NetWorthPoint = { period: string; netWorth: number };
type SavingsRatePoint = { period: string; savingsRate: number };
type TrendPoint = { period: string; income: number; expense: number };

export function InsightsPage() {
  const health = useQuery({ queryKey: ["insights", "health-score"], queryFn: async () => (await api.get<HealthScore>("/api/insights/health-score")).data });
  const summary = useQuery({ queryKey: ["insights", "summary"], queryFn: async () => (await api.get<InsightSummary>("/api/insights")).data });
  const netWorth = useQuery({ queryKey: ["reports", "net-worth"], queryFn: async () => (await api.get<NetWorthPoint[]>("/api/reports/net-worth")).data });
  const savingsTrend = useQuery({ queryKey: ["reports", "trends"], queryFn: async () => (await api.get<SavingsRatePoint[]>("/api/reports/trends")).data });
  const incomeExpense = useQuery({ queryKey: ["reports", "income-vs-expense", "insights"], queryFn: async () => (await api.get<TrendPoint[]>("/api/reports/income-vs-expense")).data });

  const maxNetWorth = useMemo(() => Math.max(1, ...(netWorth.data ?? []).map((item) => Number(item.netWorth))), [netWorth.data]);
  const maxSavingsRate = useMemo(() => Math.max(1, ...(savingsTrend.data ?? []).map((item) => Number(item.savingsRate))), [savingsTrend.data]);
  const maxTrend = useMemo(() => Math.max(1, ...(incomeExpense.data ?? []).flatMap((item) => [Number(item.income), Number(item.expense)])), [incomeExpense.data]);

  return (
    <div className="insights-page-shell page-fit-layout">
      <div className="summary-row finance-summary-grid insights-summary-grid insights-summary-cards">
        <article className="summary-card finance-summary-card goal-card">
          <span>Health Score</span>
          <strong>{health.data?.score ?? 0}/100</strong>
        </article>
        <article className="summary-card finance-summary-card income-card">
          <span>This Month Savings</span>
          <strong>{summary.data?.currentMonthSavingsRate ?? "0.00"}%</strong>
        </article>
        <article className="summary-card finance-summary-card expense-card">
          <span>Last Month Savings</span>
          <strong>{summary.data?.previousMonthSavingsRate ?? "0.00"}%</strong>
        </article>
      </div>

      <section className="glass-panel nested-panel insights-page-panel">
        <div className="panel-head"><div><h2>Highlights</h2><p>Key findings and financial signals from your recent activity.</p></div></div>
        <div className="insights-highlight-grid">
          {(summary.data?.cards ?? []).map((card) => (
            <article key={`${card.title}-${card.message}`} className={`insight-highlight-card severity-${card.severity.toLowerCase()}`}>
              <strong>{card.title}</strong>
              <p>{card.message}</p>
            </article>
          ))}
        </div>
      </section>

      <div className="report-grid structured-report-grid insights-grid">
        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Health Breakdown</h2><p>How the score is calculated.</p></div></div>
          <div className="chart-placeholder report-chart-list">
            {(health.data?.factors ?? []).map((factor) => (
              <div key={factor.label} className="chart-list-row report-chart-row">
                <span>{factor.label}</span>
                <div className="report-bar-stack">
                  <div className="mini-bar" title={`${factor.score} out of 100`}><span style={{ width: `${factor.score}%` }} /></div>
                  <div className="progress-caption"><span>{factor.detail}</span><span>{factor.score}/100</span></div>
                </div>
              </div>
            ))}
          </div>
          <div className="insights-suggestions">
            {(health.data?.suggestions ?? []).map((item) => <div key={item} className="top-categories-card">{item}</div>)}
          </div>
        </section>

        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Net Worth Tracking</h2><p>Balances and accumulated cash flow over recent months.</p></div></div>
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

      <div className="report-grid structured-report-grid insights-grid insights-grid-bottom">
        <section className="glass-panel nested-panel report-card">
          <div className="panel-head"><div><h2>Savings Rate Trend</h2><p>Monthly savings strength over time.</p></div></div>
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
          <div className="panel-head"><div><h2>Income vs Expense</h2><p>Compare how cash flow has changed across recent months.</p></div></div>
          <div className="trend-list report-trend-list">
            {(incomeExpense.data ?? []).map((point) => (
              <div key={point.period} className="trend-row report-trend-row">
                <span>{point.period}</span>
                <div className="report-bar-stack">
                  <div className="trend-bars">
                    <div className="trend-bar income" title={`Income $${Number(point.income).toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(Number(point.income) / maxTrend) * 100}%` }} /></div>
                    <div className="trend-bar expense" title={`Expense $${Number(point.expense).toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(Number(point.expense) / maxTrend) * 100}%` }} /></div>
                  </div>
                  <div className="progress-caption"><span>Income ${Number(point.income).toFixed(0)}</span><span>Max ${maxTrend.toFixed(0)}</span><span>Expense ${Number(point.expense).toFixed(0)}</span></div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
