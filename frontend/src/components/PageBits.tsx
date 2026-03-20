import type { ReactNode } from "react";

export function PageSection(props: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <h2>{props.title}</h2>
          {props.subtitle ? <p>{props.subtitle}</p> : null}
        </div>
      </div>
      {props.children}
    </section>
  );
}

export function SummaryCard(props: { label: string; value: string; tone?: "good" | "warn" | "neutral" }) {
  return (
    <article className={`summary-card ${props.tone ?? "neutral"}`}>
      <span>{props.label}</span>
      <strong>{props.value}</strong>
    </article>
  );
}

export function EmptyState(props: { title: string; message: string }) {
  return (
    <div className="empty-state">
      <strong>{props.title}</strong>
      <p>{props.message}</p>
    </div>
  );
}
