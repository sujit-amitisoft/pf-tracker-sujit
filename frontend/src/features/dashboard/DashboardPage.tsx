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

type DonutSegment = CategorySpend & {
  color: string;
  fraction: number;
  dash: number;
  offset: number;
  labelX: number;
  labelY: number;
  lineX: number;
  lineY: number;
  lineEndX: number;
  textAnchor: "start" | "end";
  translateX: number;
  translateY: number;
};

const spendColors = ["#1d4ed8", "#5b3f99", "#8f3f97", "#d94688", "#fb7185", "#fb923c", "#facc15", "#94a3b8"];

export function DashboardPage() {
  const [hoveredCategory, setHoveredCategory] = useState<string | null>(null);
  const summary = useQuery({ queryKey: ["dashboard", "summary"], queryFn: async () => (await api.get<DashboardSummary>("/api/dashboard/summary")).data });
  const recent = useQuery({ queryKey: ["dashboard", "recent"], queryFn: async () => (await api.get<RecentItem[]>("/api/dashboard/recent-transactions")).data });
  const recurring = useQuery({ queryKey: ["dashboard", "upcoming"], queryFn: async () => (await api.get<RecurringItem[]>("/api/dashboard/upcoming-recurring")).data });
  const trend = useQuery({ queryKey: ["reports", "trend"], queryFn: async () => (await api.get<TrendPoint[]>("/api/reports/income-vs-expense")).data });
  const categorySpend = useQuery({ queryKey: ["reports", "category-spend"], queryFn: async () => (await api.get<CategorySpend[]>("/api/reports/category-spend")).data });
  const goals = useQuery({ queryKey: ["goals"], queryFn: async () => (await api.get<Goal[]>("/api/goals")).data });

  const maxTrend = Math.max(1, ...(trend.data ?? []).flatMap((point) => [Number(point.income), Number(point.expense)]));
  const primaryGoal = goals.data?.[0];

  const donutData = useMemo(() => {
    const spend = (categorySpend.data ?? []).slice(0, 8);
    const total = spend.reduce((sum, item) => sum + Number(item.amount), 0);
    const radius = 76;
    const circumference = 2 * Math.PI * radius;
    let offset = 0;
    let currentAngle = -90;

    return {
      total,
      segments: spend.map((item, index) => {
        const amount = Number(item.amount);
        const fraction = total > 0 ? amount / total : 0;
        const dash = circumference * fraction;
        const sweep = fraction * 360;
        const midAngle = currentAngle + sweep / 2;
        const radians = (midAngle * Math.PI) / 180;
        const outerRadius = radius + 8;
        const lineRadius = radius + 24;
        const labelRadius = radius + 46;
        const lineX = 150 + Math.cos(radians) * outerRadius;
        const lineY = 150 + Math.sin(radians) * outerRadius;
        const lineEndX = 150 + Math.cos(radians) * lineRadius + (Math.cos(radians) >= 0 ? 16 : -16);
        const labelX = 150 + Math.cos(radians) * labelRadius + (Math.cos(radians) >= 0 ? 22 : -22);
        const labelY = 150 + Math.sin(radians) * labelRadius;
        const segment: DonutSegment = {
          ...item,
          color: spendColors[index % spendColors.length],
          fraction,
          dash,
          offset,
          labelX,
          labelY,
          lineX,
          lineY,
          lineEndX,
          textAnchor: Math.cos(radians) >= 0 ? "start" : "end",
          translateX: Math.cos(radians) * 10,
          translateY: Math.sin(radians) * 10,
        };
        currentAngle += sweep;
        offset += dash;
        return segment;
      }),
      radius,
      circumference,
    };
  }, [categorySpend.data]);

  const activeSegment = hoveredCategory ? donutData.segments.find((item) => item.category === hoveredCategory) ?? null : null;

  return (
    <div className="dashboard-structured compact-dashboard-layout">
      <div className="summary-row structured-summary-row finance-summary-grid compact-summary-row">
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
      </div>

      <div className="report-grid structured-report-grid compact-dashboard-grid">
        <section className="glass-panel compact-dashboard-panel">
          <div className="panel-head compact-panel-head"><div><h2>Spending by Category</h2><p>Current month category distribution.</p></div></div>
          <div className="dashboard-pie-card">
            <div className={activeSegment ? "dashboard-pie-wrap has-active-segment" : "dashboard-pie-wrap"} aria-label="Spending by category chart">
              <svg viewBox="0 0 300 300" className="dashboard-pie-chart" role="img">
                <circle cx="150" cy="150" r={donutData.radius} className="dashboard-donut-track" />
                {donutData.segments.map((item) => {
                  const isActive = activeSegment?.category === item.category;
                  return (
                    <g
                      key={item.category}
                      className={`dashboard-pie-group ${isActive ? "active" : ""}`}
                      style={{
                        transform: `translate(${isActive ? item.translateX : 0}px, ${isActive ? item.translateY : 0}px) scale(${isActive ? 1.11 : 1})`,
                        transformOrigin: "150px 150px",
                        transformBox: "fill-box",
                      }}
                    >
                      <circle
                        cx="150"
                        cy="150"
                        r={isActive ? donutData.radius + 2 : donutData.radius}
                        fill="none"
                        stroke={item.color}
                        strokeWidth={isActive ? "42" : "32"}
                        strokeLinecap="butt"
                        strokeDasharray={`${item.dash} ${donutData.circumference - item.dash}`}
                        strokeDashoffset={-item.offset}
                        transform="rotate(-90 150 150)"
                        className={`dashboard-donut-segment ${isActive ? "active" : ""}`}
                        onMouseEnter={() => setHoveredCategory(item.category)}
                        onMouseMove={() => setHoveredCategory(item.category)}
                        onMouseLeave={() => setHoveredCategory(null)}
                        style={{ pointerEvents: "stroke" }}
                      />
                      <line x1={item.lineX} y1={item.lineY} x2={item.lineEndX} y2={item.labelY} className={`dashboard-pie-line ${isActive ? "active" : ""}`} />
                      <text x={item.labelX} y={item.labelY - 3} textAnchor={item.textAnchor} className={`dashboard-pie-label ${isActive ? "active" : ""}`}>
                        {item.category} {Math.round(item.fraction * 100)}%
                      </text>
                    </g>
                  );
                })}
              </svg>
              <div className="dashboard-donut-center compact-donut-center">
                <span>{activeSegment ? activeSegment.category : "Total Spend"}</span>
                <strong>${activeSegment ? Number(activeSegment.amount).toFixed(2) : donutData.total.toFixed(2)}</strong>
                <small>{activeSegment ? `${Math.round(activeSegment.fraction * 100)}% of spend` : "Hover a slice"}</small>
              </div>
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

      <div className="report-grid structured-report-grid compact-dashboard-grid compact-dashboard-bottom-row">
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

