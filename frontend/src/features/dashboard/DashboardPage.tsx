import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "../../services/api";

type DashboardSummary = {
  currentMonthIncome: string;
  currentMonthExpense: string;
  netBalance: string;
  savingsRate: string;
  activeBudgets: number;
  upcomingBills: number;
};

type RecentItem = { merchant: string; category: string; amount: string; type: string; date: string };
type RecurringItem = { title: string; nextRunDate: string; amount: string };
type TrendPoint = { period: string; income: number; expense: number };
type CategorySpend = { category: string; amount: number };
type Goal = { id: string; name: string; targetAmount: string; currentAmount: string; targetDate: string; progressPercent: number };
type ForecastMonth = { currentBalance: string; projectedBalance: string; knownIncome: string; knownExpense: string; averageDailyNet: string; safeToSpend: string; warning: string | null; daysRemaining: number };
type ForecastDaily = { points: Array<{ date: string; projectedBalance: number }> };
type HealthScore = { score: number; factors: Array<{ label: string; score: number; detail: string }>; suggestions: string[] };

type DonutSegment = CategorySpend & {
  color: string;
  fraction: number;
  dash: number;
  offset: number;
};

const spendColors = ["#1d4ed8", "#5b3f99", "#8f3f97", "#d94688", "#fb7185", "#fb923c", "#facc15", "#94a3b8"];

function formatPeriodLabel(date: string) {
  const parsed = new Date(date);
  if (Number.isNaN(parsed.getTime())) return date;
  return parsed.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

export function DashboardPage() {
  const [hoveredCategory, setHoveredCategory] = useState<string | null>(null);
  const summary = useQuery({ queryKey: ["dashboard", "summary"], queryFn: async () => (await api.get<DashboardSummary>("/api/dashboard/summary")).data });
  const recent = useQuery({ queryKey: ["dashboard", "recent"], queryFn: async () => (await api.get<RecentItem[]>("/api/dashboard/recent-transactions")).data });
  const recurring = useQuery({ queryKey: ["dashboard", "upcoming"], queryFn: async () => (await api.get<RecurringItem[]>("/api/dashboard/upcoming-recurring")).data });
  const trend = useQuery({ queryKey: ["reports", "trend"], queryFn: async () => (await api.get<TrendPoint[]>("/api/reports/income-vs-expense")).data });
  const categorySpend = useQuery({ queryKey: ["reports", "category-spend"], queryFn: async () => (await api.get<CategorySpend[]>("/api/reports/category-spend")).data });
  const goals = useQuery({ queryKey: ["goals"], queryFn: async () => (await api.get<Goal[]>("/api/goals")).data });
  const forecastMonth = useQuery({ queryKey: ["forecast", "month"], queryFn: async () => (await api.get<ForecastMonth>("/api/forecast/month")).data });
  const forecastDaily = useQuery({ queryKey: ["forecast", "daily"], queryFn: async () => (await api.get<ForecastDaily>("/api/forecast/daily")).data });
  const healthScore = useQuery({ queryKey: ["insights", "health-score", "dashboard"], queryFn: async () => (await api.get<HealthScore>("/api/insights/health-score")).data });

  const maxTrend = Math.max(1, ...(trend.data ?? []).flatMap((point) => [Number(point.income), Number(point.expense)]));
  const primaryGoal = goals.data?.[0];

  const donutData = useMemo(() => {
    const spend = (categorySpend.data ?? []).slice(0, 6);
    const total = spend.reduce((sum, item) => sum + Number(item.amount), 0);
    const radius = 76;
    const circumference = 2 * Math.PI * radius;
    let offset = 0;

    return {
      total,
      segments: spend.map((item, index) => {
        const amount = Number(item.amount);
        const fraction = total > 0 ? amount / total : 0;
        const dash = circumference * fraction;
        const segment: DonutSegment = {
          ...item,
          color: spendColors[index % spendColors.length],
          fraction,
          dash,
          offset,
        };
        offset += dash;
        return segment;
      }),
      radius,
      circumference,
    };
  }, [categorySpend.data]);

  const activeSegment = hoveredCategory ? donutData.segments.find((item) => item.category === hoveredCategory) ?? null : donutData.segments[0] ?? null;

  const forecastChart = useMemo(() => {
    const points = forecastDaily.data?.points ?? [];
    if (!points.length) {
      return { linePath: "", areaPath: "", min: 0, max: 0, start: null as null | string, end: null as null | string };
    }
    const width = 340;
    const height = 160;
    const values = points.map((item) => Number(item.projectedBalance));
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = Math.max(1, max - min);
    const coords = points.map((point, index) => {
      const x = (index / Math.max(points.length - 1, 1)) * width;
      const y = height - (((Number(point.projectedBalance) - min) / range) * height);
      return { x, y, date: point.date, value: Number(point.projectedBalance) };
    });
    const linePath = coords.map((point, index) => `${index === 0 ? "M" : "L"}${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(" ");
    const areaPath = `${linePath} L ${coords[coords.length - 1].x.toFixed(1)},160 L 0,160 Z`;
    return {
      linePath,
      areaPath,
      min,
      max,
      start: formatPeriodLabel(coords[0].date),
      end: formatPeriodLabel(coords[coords.length - 1].date),
    };
  }, [forecastDaily.data]);

  const forecastDelta = forecastMonth.data ? (Number(forecastMonth.data.projectedBalance) - Number(forecastMonth.data.currentBalance)).toFixed(2) : "0.00";

  return (
    <div className="dashboard-structured compact-dashboard-layout">
      <div className="summary-row structured-summary-row finance-summary-grid compact-summary-row dashboard-v2-summary-grid">
        <article className="summary-card finance-summary-card compact-summary-card balance-card">
          <span>Balance</span>
          <strong>${summary.data?.netBalance ?? "0.00"}</strong>
        </article>
        <article className="summary-card finance-summary-card compact-summary-card income-card">
          <span>Income</span>
          <strong>${summary.data?.currentMonthIncome ?? "0.00"}</strong>
        </article>
        <article className="summary-card finance-summary-card compact-summary-card expense-card">
          <span>Expense</span>
          <strong>${summary.data?.currentMonthExpense ?? "0.00"}</strong>
        </article>
        <article className="summary-card finance-summary-card compact-summary-card goal-card">
          <span>Savings Goal</span>
          <strong>{primaryGoal ? `${primaryGoal.progressPercent}%` : `${summary.data?.savingsRate ?? 0}%`}</strong>
        </article>
        <article className="summary-card finance-summary-card compact-summary-card income-card">
          <span>Projected Balance</span>
          <strong>${forecastMonth.data?.projectedBalance ?? "0.00"}</strong>
        </article>
        <article className="summary-card finance-summary-card compact-summary-card balance-card">
          <span>Health Score</span>
          <strong>{healthScore.data?.score ?? 0}/100</strong>
        </article>
      </div>

      <div className="report-grid structured-report-grid compact-dashboard-grid">
        <section className="glass-panel compact-dashboard-panel">
          <div className="panel-head compact-panel-head"><div><h2>Spending by Category</h2><p>Current month category distribution.</p></div></div>
          <div className="dashboard-pie-card dashboard-pie-card-legend">
            <div className="dashboard-pie-wrap" aria-label="Spending by category chart">
              <svg viewBox="0 0 300 300" className="dashboard-pie-chart" role="img">
                <circle cx="150" cy="150" r={donutData.radius} className="dashboard-donut-track" />
                {donutData.segments.map((item) => {
                  const isActive = activeSegment?.category === item.category;
                  return (
                    <circle
                      key={item.category}
                      cx="150"
                      cy="150"
                      r={isActive ? donutData.radius + 2 : donutData.radius}
                      fill="none"
                      stroke={item.color}
                      strokeWidth={isActive ? "42" : "32"}
                      strokeLinecap="round"
                      strokeDasharray={`${item.dash} ${donutData.circumference - item.dash}`}
                      strokeDashoffset={-item.offset}
                      transform="rotate(-90 150 150)"
                      className={`dashboard-donut-segment ${isActive ? "active" : ""}`}
                      onMouseEnter={() => setHoveredCategory(item.category)}
                      onMouseLeave={() => setHoveredCategory(null)}
                      style={{ pointerEvents: "stroke" }}
                    />
                  );
                })}
              </svg>
              <div className="dashboard-donut-center compact-donut-center">
                <span>{activeSegment ? activeSegment.category : "Total Spend"}</span>
                <strong>${activeSegment ? Number(activeSegment.amount).toFixed(2) : donutData.total.toFixed(2)}</strong>
                <small>{activeSegment ? `${Math.round(activeSegment.fraction * 100)}% of spend` : "Move over a slice"}</small>
              </div>
            </div>
            <div className="dashboard-pie-legend">
              {donutData.segments.map((item) => (
                <button
                  key={item.category}
                  type="button"
                  className={`dashboard-pie-legend-row ${activeSegment?.category === item.category ? "active" : ""}`}
                  onMouseEnter={() => setHoveredCategory(item.category)}
                  onMouseLeave={() => setHoveredCategory(null)}
                >
                  <span className="dashboard-pie-legend-main">
                    <span className="dashboard-pie-legend-dot" style={{ background: item.color }} />
                    <span>{item.category}</span>
                  </span>
                  <strong>{Math.round(item.fraction * 100)}%</strong>
                </button>
              ))}
            </div>
          </div>
        </section>
        <section className="glass-panel compact-dashboard-panel">
          <div className="panel-head compact-panel-head"><div><h2>Income vs Expense Trend</h2><p>Monthly trend comparison.</p></div></div>
          <div className="trend-list compact-trend-list">
            {(trend.data ?? []).map((point) => (
              <div key={point.period} className="trend-row compact-trend-row">
                <span>{point.period}</span>
                <div className="trend-bars">
                  <div className="trend-bar income" title={`Income $${Number(point.income).toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(Number(point.income) / maxTrend) * 100}%` }} /></div>
                  <div className="trend-bar expense" title={`Expense $${Number(point.expense).toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(Number(point.expense) / maxTrend) * 100}%` }} /></div>
                </div>
                <div className="progress-caption"><span>Income $${Number(point.income).toFixed(0)}</span><span>Max $${maxTrend.toFixed(0)}</span><span>Expense $${Number(point.expense).toFixed(0)}</span></div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className="report-grid structured-report-grid compact-dashboard-grid compact-dashboard-bottom-row dashboard-v2-bottom-row">
        <section className="glass-panel compact-dashboard-panel compact-list-panel forecast-panel">
          <div className="panel-head compact-panel-head"><div><h2>Projected Balance</h2><p>{forecastMonth.data?.warning ?? `${forecastMonth.data?.daysRemaining ?? 0} days remaining this month.`}</p></div></div>
          <div className="forecast-line-wrap forecast-card-enhanced">
            <div className="forecast-stats-row">
              <article className="forecast-stat-card">
                <span>Current</span>
                <strong>${forecastMonth.data?.currentBalance ?? "0.00"}</strong>
              </article>
              <article className="forecast-stat-card">
                <span>Projected</span>
                <strong>${forecastMonth.data?.projectedBalance ?? "0.00"}</strong>
              </article>
              <article className="forecast-stat-card">
                <span>Change</span>
                <strong className={Number(forecastDelta) >= 0 ? "positive" : "negative"}>{Number(forecastDelta) >= 0 ? "+" : ""}${forecastDelta}</strong>
              </article>
            </div>
            <svg viewBox="0 0 340 190" className="forecast-line-chart" role="img">
              <defs>
                <linearGradient id="forecast-area" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="rgba(99,102,241,0.34)" />
                  <stop offset="100%" stopColor="rgba(99,102,241,0.02)" />
                </linearGradient>
              </defs>
              <line x1="0" y1="18" x2="340" y2="18" className="forecast-grid-line" />
              <line x1="0" y1="90" x2="340" y2="90" className="forecast-grid-line" />
              <line x1="0" y1="160" x2="340" y2="160" className="forecast-grid-line" />
              {forecastChart.areaPath ? <path d={forecastChart.areaPath} className="forecast-area-fill" /> : null}
              {forecastChart.linePath ? <path d={forecastChart.linePath} className="forecast-line-stroke" /> : null}
            </svg>
            <div className="forecast-axis-row">
              <span>{forecastChart.start ?? "Start"}</span>
              <span>{forecastChart.end ?? "End"}</span>
            </div>
            <div className="progress-caption forecast-caption-grid">
              <span>Low ${forecastChart.min.toFixed(0)}</span>
              <span>High ${forecastChart.max.toFixed(0)}</span>
              <span>Safe to spend ${forecastMonth.data?.safeToSpend ?? "0.00"}</span>
            </div>
          </div>
        </section>
        <section className="glass-panel compact-dashboard-panel compact-list-panel">
          <div className="panel-head compact-panel-head"><div><h2>Recent Transactions</h2><p>Latest activity in your accounts.</p></div></div>
          <div className="simple-list compact-simple-list">
            {(recent.data ?? []).slice(0, 4).map((item, index) => (
              <div key={`${item.merchant}-${index}`} className="simple-list-row compact-simple-list-row">
                <span>{item.merchant}</span>
                <strong>{item.type === "INCOME" ? "+" : "-"}${item.amount}</strong>
              </div>
            ))}
          </div>
        </section>
        <section className="glass-panel compact-dashboard-panel compact-list-panel">
          <div className="panel-head compact-panel-head"><div><h2>Upcoming Bills</h2><p>Recurring expenses due soon.</p></div></div>
          <div className="simple-list compact-simple-list">
            {(recurring.data ?? []).slice(0, 4).map((item) => (
              <div key={`${item.title}-${item.nextRunDate}`} className="simple-list-row compact-simple-list-row">
                <span>{item.title} {item.nextRunDate}</span>
                <strong>${item.amount}</strong>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
