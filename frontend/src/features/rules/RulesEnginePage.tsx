import { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../../services/api";
import { AppSelect } from "../../components/FormControls";

type Rule = {
  id: string;
  name: string;
  field: "MERCHANT" | "AMOUNT" | "CATEGORY";
  operator: "EQUALS" | "CONTAINS" | "GREATER_THAN" | "LESS_THAN";
  value: string;
  actionType: "SET_CATEGORY" | "ADD_TAG" | "TRIGGER_ALERT";
  actionValue: string;
  priority: number;
  active: boolean;
  createdAt: string;
};

type Category = { id: string; name: string; type: "INCOME" | "EXPENSE" };

type RuleForm = {
  id?: string;
  name: string;
  field: Rule["field"];
  operator: Rule["operator"];
  value: string;
  actionType: Rule["actionType"];
  actionValue: string;
  priority: string;
  active: boolean;
};

function createInitialForm(): RuleForm {
  return {
    name: "",
    field: "MERCHANT",
    operator: "CONTAINS",
    value: "",
    actionType: "SET_CATEGORY",
    actionValue: "",
    priority: "100",
    active: true,
  };
}

export function RulesEnginePage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RuleForm>(() => createInitialForm());
  const [error, setError] = useState<string | null>(null);
  const [ruleToDelete, setRuleToDelete] = useState<Rule | null>(null);
  const rules = useQuery({ queryKey: ["rules"], queryFn: async () => (await api.get<Rule[]>("/api/rules")).data });
  const categories = useQuery({ queryKey: ["categories", "rules"], queryFn: async () => (await api.get<Category[]>("/api/categories")).data });

  const fieldOptions = [
    { value: "MERCHANT", label: "Merchant" },
    { value: "AMOUNT", label: "Amount" },
    { value: "CATEGORY", label: "Category" },
  ];
  const operatorOptions = [
    { value: "EQUALS", label: "Equals" },
    { value: "CONTAINS", label: "Contains" },
    { value: "GREATER_THAN", label: "Greater than" },
    { value: "LESS_THAN", label: "Less than" },
  ];
  const actionOptions = [
    { value: "SET_CATEGORY", label: "Set category" },
    { value: "ADD_TAG", label: "Add tag" },
    { value: "TRIGGER_ALERT", label: "Trigger alert" },
  ];

  const categoryOptions = useMemo(
    () => (categories.data ?? []).filter((item) => item.type === "EXPENSE").map((item) => ({ value: item.id, label: item.name })),
    [categories.data],
  );

  useEffect(() => {
    if (form.actionType === "SET_CATEGORY" && !form.actionValue && categoryOptions[0]) {
      setForm((current) => ({ ...current, actionValue: categoryOptions[0].value }));
    }
  }, [categoryOptions, form.actionType, form.actionValue]);

  const updateForm = <K extends keyof RuleForm>(key: K, value: RuleForm[K]) => {
    setForm((current) => {
      if (key === "actionType") {
        const nextActionType = value as RuleForm["actionType"];
        const nextActionValue = nextActionType === "SET_CATEGORY"
          ? current.actionValue && categoryOptions.some((item) => item.value === current.actionValue)
            ? current.actionValue
            : (categoryOptions[0]?.value ?? "")
          : categoryOptions.some((item) => item.value === current.actionValue)
            ? ""
            : current.actionValue;

        return { ...current, actionType: nextActionType, actionValue: nextActionValue };
      }

      return { ...current, [key]: value };
    });
    setError(null);
  };

  const resetForm = () => {
    setForm(createInitialForm());
    setError(null);
  };

  const saveRule = async () => {
    if (!form.name.trim() || !form.value.trim() || !form.actionValue.trim()) {
      setError("Complete the rule name, condition value, and action value.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      field: form.field,
      operator: form.operator,
      value: form.value.trim(),
      actionType: form.actionType,
      actionValue: form.actionValue.trim(),
      priority: Number(form.priority || 100),
      active: form.active,
    };

    try {
      if (form.id) {
        await api.put(`/api/rules/${form.id}`, payload);
      } else {
        await api.post("/api/rules", payload);
      }
      resetForm();
      await queryClient.invalidateQueries({ queryKey: ["rules"] });
    } catch (err: any) {
      setError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? "Failed to save rule");
    }
  };

  const removeRule = async (id: string) => {
    await api.delete(`/api/rules/${id}`);
    await queryClient.invalidateQueries({ queryKey: ["rules"] });
    if (form.id === id) resetForm();
  };

  const confirmDeleteRule = async () => {
    if (!ruleToDelete) return;
    await removeRule(ruleToDelete.id);
    setRuleToDelete(null);
  };

  const editRule = (rule: Rule) => {
    setForm({
      id: rule.id,
      name: rule.name,
      field: rule.field,
      operator: rule.operator,
      value: rule.value,
      actionType: rule.actionType,
      actionValue: rule.actionValue,
      priority: String(rule.priority),
      active: rule.active,
    });
    setError(null);
  };

  const toggleRule = async (rule: Rule) => {
    await api.put(`/api/rules/${rule.id}`, { ...rule, active: !rule.active });
    await queryClient.invalidateQueries({ queryKey: ["rules"] });
  };

  return (
    <>
      <div className="page-fit-layout rules-page-shell">
        <section className="glass-panel nested-panel rules-builder-panel">
          <div className="panel-head">
            <div>
              <h2>Rules Builder</h2>
              <p>Create form-based automation for categorization, tagging, and alerts.</p>
            </div>
          </div>
          <div className="filters-bar structured-filters-bar rules-builder-grid rules-builder-form">
            <input className="rules-input" value={form.name} onChange={(event) => updateForm("name", event.target.value)} placeholder="Rule name" />
            <AppSelect className="rules-select" value={form.field} onChange={(value) => updateForm("field", value as RuleForm["field"])} options={fieldOptions} placeholder="Field" />
            <AppSelect className="rules-select" value={form.operator} onChange={(value) => updateForm("operator", value as RuleForm["operator"])} options={operatorOptions} placeholder="Operator" />
            <input className="rules-input" value={form.value} onChange={(event) => updateForm("value", event.target.value)} placeholder={form.field === "AMOUNT" ? "5000" : "Condition value"} />
            <AppSelect className="rules-select" value={form.actionType} onChange={(value) => updateForm("actionType", value as RuleForm["actionType"])} options={actionOptions} placeholder="Action" />
            {form.actionType === "SET_CATEGORY" ? (
              <AppSelect className="rules-select" value={form.actionValue} onChange={(value) => updateForm("actionValue", value)} options={categoryOptions} placeholder="Category" />
            ) : (
              <input className="rules-input" value={form.actionValue} onChange={(event) => updateForm("actionValue", event.target.value)} placeholder={form.actionType === "ADD_TAG" ? "monthly-food" : "Alert message"} />
            )}
            <input className="rules-input" value={form.priority} onChange={(event) => updateForm("priority", event.target.value.replace(/[^0-9]/g, ""))} placeholder="Priority" />
            <label className="rules-checkbox-inline">
              <input type="checkbox" checked={form.active} onChange={(event) => updateForm("active", event.target.checked)} />
              <span>Enabled</span>
            </label>
          </div>
          {error ? <p className="form-error">{error}</p> : null}
          <div className="modal-actions rules-actions">
            <button className="button ghost" type="button" onClick={resetForm}>Reset</button>
            <button className="button primary" type="button" onClick={saveRule}>{form.id ? "Update Rule" : "Create Rule"}</button>
          </div>
        </section>

        <section className="glass-panel nested-panel rules-list-panel">
          <div className="panel-head">
            <div>
              <h2>Active Rules</h2>
              <p>Prioritized rules apply when transactions are created or imported.</p>
            </div>
          </div>
          <div className="rules-list-grid rules-cards-grid">
            {(rules.data ?? []).map((rule) => (
              <article key={rule.id} className="rule-card rules-list-card">
                <div className="rule-card-head">
                  <strong>{rule.name}</strong>
                  <button className={rule.active ? "button ghost rule-state-button active" : "button ghost rule-state-button"} type="button" onClick={() => toggleRule(rule)}>
                    {rule.active ? "Disable" : "Enable"}
                  </button>
                </div>
                <div className="rule-summary-block">
                  <span className="rule-summary-label">If</span>
                  <p><strong>{rule.field.toLowerCase()}</strong> <strong>{rule.operator.toLowerCase().replace(/_/g, " ")}</strong> <strong>{rule.value}</strong></p>
                  <span className="rule-summary-label">Then</span>
                  <p><strong>{rule.actionType.toLowerCase().replace(/_/g, " ")}</strong> <strong>{rule.actionType === "SET_CATEGORY" ? categoryOptions.find((item) => item.value === rule.actionValue)?.label ?? rule.actionValue : rule.actionValue}</strong></p>
                </div>
                <span className="rule-meta">Priority {rule.priority}</span>
                <div className="modal-actions rules-actions compact-actions">
                  <button className="button ghost" type="button" onClick={() => editRule(rule)}>Edit</button>
                  <button className="button danger" type="button" onClick={() => setRuleToDelete(rule)}>Delete</button>
                </div>
              </article>
            ))}
          </div>
        </section>
      </div>

      {ruleToDelete ? (
        <div className="modal-backdrop" onClick={() => setRuleToDelete(null)}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-confirm-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>Delete rule</h2>
                <p>This action cannot be undone.</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={() => setRuleToDelete(null)} aria-label="Close delete rule dialog" />
            </div>
            <div className="delete-confirm-copy">
              <strong>{ruleToDelete.name}</strong>
              <p>The automation for this rule will be removed immediately.</p>
            </div>
            <div className="modal-actions delete-confirm-actions">
              <button className="button ghost" type="button" onClick={() => setRuleToDelete(null)}>Cancel</button>
              <button className="button primary delete-confirm-button" type="button" onClick={confirmDeleteRule}>Delete</button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
