import { useMemo, useState } from "react";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import { useQuery } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppDateField, AppSelect } from "../../components/FormControls";

type Transaction = { id: string; merchant: string; category: string; account: string; type: "INCOME" | "EXPENSE"; amount: string; date: string; note: string };
type Account = { id: string; name: string };
type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };
type TrendPoint = { period: string; income: number; expense: number };

type RangeOption = "THIS_MONTH" | "LAST_3_MONTHS" | "THIS_YEAR" | "CUSTOM";

function parseLocalDate(value: string) {
  return new Date(`${value}T00:00:00`);
}

function sameMonth(left: Date, right: Date) {
  return left.getFullYear() === right.getFullYear() && left.getMonth() === right.getMonth();
}

function formatMonthShort(date: Date) {
  return date.toLocaleDateString("en-US", { month: "short" });
}

function rangeBounds(range: RangeOption, startDate: string, endDate: string) {
  const today = new Date();
  const end = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59, 999);

  if (range === "THIS_MONTH") {
    return {
      start: new Date(today.getFullYear(), today.getMonth(), 1),
      end,
    };
  }

  if (range === "LAST_3_MONTHS") {
    return {
      start: new Date(today.getFullYear(), today.getMonth() - 2, 1),
      end,
    };
  }

  if (range === "THIS_YEAR") {
    return {
      start: new Date(today.getFullYear(), 0, 1),
      end,
    };
  }

  return {
    start: startDate ? new Date(`${startDate}T00:00:00`) : null,
    end: endDate ? new Date(`${endDate}T23:59:59.999`) : null,
  };
}

function buildTrendPoints(range: RangeOption, transactions: Transaction[], startDate: string, endDate: string): TrendPoint[] {
  const now = new Date();

  if (range === "THIS_MONTH") {
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    const points: TrendPoint[] = [];

    for (let week = 0; week < 5; week += 1) {
      const weekStart = new Date(start.getFullYear(), start.getMonth(), 1 + week * 7);
      if (weekStart.getMonth() !== start.getMonth() || weekStart.getDate() > daysInMonth) break;
      const weekEnd = new Date(start.getFullYear(), start.getMonth(), Math.min(daysInMonth, weekStart.getDate() + 6), 23, 59, 59, 999);
      const bucket = transactions.filter((item) => {
        const date = parseLocalDate(item.date);
        return date >= weekStart && date <= weekEnd;
      });
      points.push({
        period: `Week ${week + 1}`,
        income: bucket.filter((item) => item.type === "INCOME").reduce((sum, item) => sum + Number(item.amount), 0),
        expense: bucket.filter((item) => item.type === "EXPENSE").reduce((sum, item) => sum + Number(item.amount), 0),
      });
    }

    return points;
  }

  if (range === "CUSTOM") {
    const buckets = new Map<string, TrendPoint>();
    transactions.forEach((item) => {
      const date = parseLocalDate(item.date);
      const key = `${date.getFullYear()}-${date.getMonth()}`;
      const existing = buckets.get(key) ?? { period: formatMonthShort(date), income: 0, expense: 0 };
      if (item.type === "INCOME") existing.income += Number(item.amount);
      if (item.type === "EXPENSE") existing.expense += Number(item.amount);
      buckets.set(key, existing);
    });
    return Array.from(buckets.values());
  }

  const monthCount = range === "LAST_3_MONTHS" ? 3 : now.getMonth() + 1;
  const monthPoints: TrendPoint[] = [];

  for (let index = monthCount - 1; index >= 0; index -= 1) {
    const monthDate = range === "LAST_3_MONTHS"
      ? new Date(now.getFullYear(), now.getMonth() - index, 1)
      : new Date(now.getFullYear(), monthCount - 1 - index, 1);

    const bucket = transactions.filter((item) => sameMonth(parseLocalDate(item.date), monthDate));
    monthPoints.push({
      period: formatMonthShort(monthDate),
      income: bucket.filter((item) => item.type === "INCOME").reduce((sum, item) => sum + Number(item.amount), 0),
      expense: bucket.filter((item) => item.type === "EXPENSE").reduce((sum, item) => sum + Number(item.amount), 0),
    });
  }

  return monthPoints;
}

export function ReportsPage() {
  const [range, setRange] = useState<RangeOption>("THIS_MONTH");
  const [account, setAccount] = useState("ALL");
  const [type, setType] = useState("ALL");
  const [category, setCategory] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const transactions = useQuery({ queryKey: ["transactions", "reports"], queryFn: async () => (await api.get<Transaction[]>("/api/transactions")).data });
  const accounts = useQuery({ queryKey: ["accounts", "reports"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });
  const categories = useQuery({ queryKey: ["categories", "reports"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });

  const rangeOptions = [
    { value: "THIS_MONTH", label: "This Month" },
    { value: "LAST_3_MONTHS", label: "Last 3 Months" },
    { value: "THIS_YEAR", label: "This Year" },
    { value: "CUSTOM", label: "Custom Range" },
  ];

  const accountOptions = useMemo(
    () => [{ value: "ALL", label: "All accounts" }, ...((accounts.data ?? []).map((item) => ({ value: item.name, label: item.name })))],
    [accounts.data],
  );

  const categoryOptions = useMemo(
    () => [{ value: "ALL", label: "All categories" }, ...((categories.data ?? []).map((item) => ({ value: item.name, label: item.name })))],
    [categories.data],
  );

  const typeOptions = [
    { value: "ALL", label: "All types" },
    { value: "INCOME", label: "Income" },
    { value: "EXPENSE", label: "Expense" },
  ];

  const bounds = useMemo(() => rangeBounds(range, startDate, endDate), [range, startDate, endDate]);

  const filteredTransactions = useMemo(() => {
    return (transactions.data ?? []).filter((item) => {
      const date = parseLocalDate(item.date);
      const inRange = (!bounds.start || date >= bounds.start) && (!bounds.end || date <= bounds.end);
      const accountPass = account === "ALL" || item.account === account;
      const typePass = type === "ALL" || item.type === type;
      const categoryPass = category === "ALL" || item.category === category;
      return inRange && accountPass && typePass && categoryPass;
    });
  }, [transactions.data, bounds, account, type, category]);

  const categorySpend = useMemo(() => {
    const grouped = new Map<string, number>();
    filteredTransactions
      .filter((item) => item.type === "EXPENSE")
      .forEach((item) => grouped.set(item.category, (grouped.get(item.category) ?? 0) + Number(item.amount)));

    return Array.from(grouped.entries())
      .map(([name, amount]) => ({ category: name, amount }))
      .sort((left, right) => right.amount - left.amount);
  }, [filteredTransactions]);

  const trend = useMemo(() => buildTrendPoints(range, filteredTransactions, startDate, endDate), [range, filteredTransactions, startDate, endDate]);

  const maxTrend = Math.max(1, ...trend.flatMap((point) => [point.income, point.expense]));
  const maxSpend = Math.max(1, ...categorySpend.map((item) => item.amount));
  const topCategories = useMemo(() => categorySpend.slice(0, 3).map((item) => item.category).join(", "), [categorySpend]);

  const rangeLabel = rangeOptions.find((item) => item.value === range)?.label ?? "This Month";
  const accountLabel = accountOptions.find((item) => item.value === account)?.label ?? "All accounts";
  const typeLabel = typeOptions.find((item) => item.value === type)?.label ?? "All types";
  const categoryLabel = categoryOptions.find((item) => item.value === category)?.label ?? "All categories";

  const exportCsv = () => {
    const rows = [
      ["Date", "Merchant", "Category", "Account", "Type", "Amount", "Note"],
      ...filteredTransactions.map((item) => [item.date, item.merchant, item.category, item.account, item.type, item.amount, item.note ?? ""]),
    ];
    const csv = rows.map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "finance-report.csv";
    link.click();
    URL.revokeObjectURL(url);
  };


  const exportPdf = () => {
    const doc = new jsPDF({ unit: "pt", format: "a4" });
    const marginX = 48;
    const titleY = 54;

    doc.setFontSize(18);
    doc.text("Finance Report", marginX, titleY);

    doc.setFontSize(10);
    const meta = [`Range: ${rangeLabel}`, `Account: ${accountLabel}`, `Type: ${typeLabel}`, `Category: ${categoryLabel}`].join("   |   ");
    doc.text(meta, marginX, titleY + 18);

    const rows = filteredTransactions.map((item) => ([
      item.date,
      item.merchant,
      item.category,
      item.account,
      item.type,
      `$${item.amount}`,
      item.note ?? "",
    ]));

    (autoTable as any)(doc, {
      startY: titleY + 36,
      head: [["Date", "Merchant", "Category", "Account", "Type", "Amount", "Note"]],
      body: rows.slice(0, 80),
      styles: { fontSize: 9, cellPadding: 3 },
      headStyles: { fillColor: [30, 41, 59] },
      theme: "striped",
      margin: { left: marginX, right: marginX },
    });

    doc.save("finance-report.pdf");
  };

  return (
    <>
      <section className="glass-panel reports-page-shell">
        <div className="panel-head reports-head"><div><h2>Insight Center</h2><p>Filtered spend, trend, and top spending categories.</p></div><div className="reports-export-actions"><button className="button primary reports-export-button" type="button" onClick={exportCsv}>Export CSV</button><button className="button primary reports-export-button" type="button" onClick={exportPdf}>Export PDF</button></div></div>
        <div className="filters-bar structured-filters-bar reports-filters reports-filters-wide">
          <AppSelect value={range} onChange={(value) => setRange(value as RangeOption)} options={rangeOptions} placeholder="This Month" />
          <AppSelect value={account} onChange={setAccount} options={accountOptions} placeholder="All accounts" />
          <AppSelect value={type} onChange={setType} options={typeOptions} placeholder="All types" />
          <AppSelect value={category} onChange={setCategory} options={categoryOptions} placeholder="All categories" />
          {range === "CUSTOM" ? <AppDateField value={startDate} onChange={(event) => setStartDate(event.target.value)} placeholder="From date" /> : null}
          {range === "CUSTOM" ? <AppDateField value={endDate} onChange={(event) => setEndDate(event.target.value)} placeholder="To date" /> : null}
        </div>
        <div className="report-grid structured-report-grid reports-grid">
          <section className="glass-panel nested-panel report-card">
            <div className="panel-head"><div><h2>Category Spend</h2><p>{`${rangeLabel} | ${accountLabel} | ${typeLabel} | ${categoryLabel}`}</p></div></div>
            <div className="chart-placeholder report-chart-list">
              {categorySpend.length ? categorySpend.map((item) => (
                <div key={item.category} className="chart-list-row report-chart-row">
                  <span>{item.category}</span>
                  <div className="report-bar-stack">
                    <div className="mini-bar" title={`$${item.amount.toFixed(2)} of $${maxSpend.toFixed(2)}`}><span style={{ width: `${(item.amount / maxSpend) * 100}%` }} /></div>
                    <div className="progress-caption"><span>$0</span><span>${item.amount.toFixed(0)} / ${maxSpend.toFixed(0)}</span><span>Max</span></div>
                  </div>
                  <strong>${item.amount.toFixed(2)}</strong>
                </div>
              )) : <div className="empty-state">No category spend found for these filters.</div>}
            </div>
          </section>
          <section className="glass-panel nested-panel report-card">
            <div className="panel-head"><div><h2>Income vs Expense Trend</h2><p>{`${rangeLabel} trend overview`}</p></div></div>
            <div className="trend-list report-trend-list">
              {trend.length ? trend.map((point) => (
                <div key={point.period} className="trend-row report-trend-row">
                  <span>{point.period}</span>
                  <div className="report-bar-stack">
                    <div className="trend-bars">
                      <div className="trend-bar income" title={`Income $${point.income.toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(point.income / maxTrend) * 100}%` }} /></div>
                      <div className="trend-bar expense" title={`Expense $${point.expense.toFixed(2)} of $${maxTrend.toFixed(2)}`}><span style={{ width: `${(point.expense / maxTrend) * 100}%` }} /></div>
                    </div>
                    <div className="progress-caption"><span>Income $${point.income.toFixed(0)}</span><span>Max $${maxTrend.toFixed(0)}</span><span>Expense $${point.expense.toFixed(0)}</span></div>
                  </div>
                </div>
              )) : <div className="empty-state">No trend data found for these filters.</div>}
            </div>
          </section>
        </div>
        <div className="top-categories-card report-top-categories">Top Categories: {topCategories || "No matching categories"}</div>
      </section>
    </>
  );
}





