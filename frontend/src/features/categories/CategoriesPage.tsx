import { createPortal } from "react-dom";
import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { AppSelect } from "../../components/FormControls";
import { api } from "../../services/api";
import { showAppToast } from "../../services/toast";

type Category = { id: string; name: string; type: "INCOME" | "EXPENSE"; color: string | null; icon: string | null; archived: boolean };

type CategoryForm = {
  name: string;
  type: "EXPENSE" | "INCOME";
  color: string;
};

type CategoryConfirmAction = {
  id: string;
  mode: "archive" | "unarchive";
};

const categoryColors = [
  "#19e0c5",
  "#4cc9ff",
  "#2563eb",
  "#7c3aed",
  "#ec4899",
  "#f97316",
  "#f59e0b",
  "#22c55e",
  "#14b8a6",
  "#64748b",
];

function createInitialForm(): CategoryForm {
  return {
    name: "",
    type: "EXPENSE",
    color: "#19e0c5",
  };
}

function normalizeColorInput(value: string) {
  const cleaned = value.replace(/[^#a-fA-F0-9]/g, "");
  if (!cleaned) return "";
  if (cleaned.startsWith("#")) return cleaned.slice(0, 7);
  return `#${cleaned.slice(0, 6)}`;
}

function resolveCategoryColor(color: string | null) {
  return normalizeColorInput(color ?? "") || "#94a3b8";
}

export function CategoriesPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<CategoryForm>(() => createInitialForm());
  const [formError, setFormError] = useState<string | null>(null);
  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [editingCategoryId, setEditingCategoryId] = useState<string | null>(null);
  const [editingCategoryIcon, setEditingCategoryIcon] = useState<string | null>(null);
  const [editingCategoryArchived, setEditingCategoryArchived] = useState(false);
  const [confirmAction, setConfirmAction] = useState<CategoryConfirmAction | null>(null);
  const [viewFilter, setViewFilter] = useState<"active" | "archived">("active");
  const categories = useQuery({ queryKey: ["categories"], queryFn: async () => (await api.get<Category[]>('/api/categories')).data });


  const categoryTypeOptions = useMemo(
    () => [
      { value: "EXPENSE", label: "Expense" },
      { value: "INCOME", label: "Income" },
    ],
    [],
  );

  const sortedCategories = useMemo(
    () =>
      [...(categories.data ?? [])].sort((left, right) => {
        if (left.archived !== right.archived) return left.archived ? 1 : -1;
        if (left.type !== right.type) return left.type.localeCompare(right.type);
        return left.name.localeCompare(right.name);
      }),
    [categories.data],
  );

  const visibleCategories = useMemo(
    () => sortedCategories.filter((item) => (viewFilter === "archived" ? item.archived : !item.archived)),
    [sortedCategories, viewFilter],
  );

  const resetModal = () => {
    setEditingCategoryId(null);
    setEditingCategoryIcon(null);
    setEditingCategoryArchived(false);
    setForm(createInitialForm());
    setFormError(null);
  };

  const openCreateModal = () => {
    resetModal();
    setShowCategoryModal(true);
  };

  const openEditModal = (category: Category) => {
    setShowCategoryModal(true);
    setEditingCategoryId(category.id);
    setEditingCategoryIcon(category.icon);
    setEditingCategoryArchived(category.archived);
    setForm({
      name: category.name,
      type: category.type,
      color: category.color || "#19e0c5",
    });
    setFormError(null);
  };

  const closeModal = () => {
    setShowCategoryModal(false);
    resetModal();
  };

  const updateForm = (key: keyof CategoryForm, value: string) => {
    const nextValue = key === "color" ? normalizeColorInput(value) : value;
    setForm((current) => ({ ...current, [key]: nextValue as CategoryForm[keyof CategoryForm] }));
    setFormError(null);
  };

  const saveCategory = async () => {
    if (!form.name.trim()) {
      setFormError("Enter a category name.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      type: form.type,
      color: form.color.trim() || null,
      icon: editingCategoryIcon,
      archived: editingCategoryArchived,
    };

    try {
      if (editingCategoryId) {
        await api.put(`/api/categories/${editingCategoryId}`, payload);
        showAppToast("Category updated");
      } else {
        await api.post('/api/categories', { ...payload, icon: null, archived: false });
        showAppToast('Category created');
      }
      closeModal();
      await queryClient.invalidateQueries({ queryKey: ['categories'] });
    } catch (err: any) {
      setFormError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? `Failed to ${editingCategoryId ? 'update' : 'create'} category`);
    }
  };

  const confirmCategoryStateChange = async () => {
    if (!confirmAction) return;

    const target = sortedCategories.find((item) => item.id === confirmAction.id);
    if (!target) {
      setConfirmAction(null);
      return;
    }

    try {
      await api.put(`/api/categories/${target.id}`, {
        name: target.name,
        type: target.type,
        color: target.color,
        icon: target.icon,
        archived: confirmAction.mode === 'archive',
      });
      setConfirmAction(null);
      if (editingCategoryId === target.id) {
        setEditingCategoryArchived(confirmAction.mode === 'archive');
      }
      showAppToast(confirmAction.mode === 'archive' ? 'Category archived' : 'Category unarchived');
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['categories'] }),
        queryClient.invalidateQueries({ queryKey: ['budgets'] }),
        queryClient.invalidateQueries({ queryKey: ['transactions'] }),
      ]);
    } catch (err: any) {
      setConfirmAction(null);
      setFormError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? `Failed to ${confirmAction.mode} category`);
    }
  };

  const confirmTarget = sortedCategories.find((item) => item.id === confirmAction?.id) ?? null;
  const activeModalTitle = editingCategoryId ? 'Edit Category' : 'Create Category';
  const activeModalCopy = editingCategoryId ? 'Update the category details and save the changes.' : 'Create a category with type and color selection.';
  const selectedColor = form.color || '#19e0c5';
  const modalToggleLabel = editingCategoryArchived ? 'Unarchive Category' : 'Archive Category';
  const activeCount = sortedCategories.filter((item) => !item.archived).length;
  const archivedCount = sortedCategories.filter((item) => item.archived).length;

  return (
    <>
      <section className="glass-panel categories-page-shell categories-sticky-layout">
        <div className="sticky-section-head categories-sticky-head">
          <div className="panel-head budgets-head categories-head-row">
            <div className="categories-head-copy">
              <p>Manage income and expense categories, review status, and switch between active and archived items.</p>
              <div className="categories-view-switch" role="tablist" aria-label="Category list views">
                <button className={viewFilter === 'active' ? 'button active categories-view-button' : 'button categories-view-button'} type="button" onClick={() => setViewFilter('active')}>UnArchived ({activeCount})</button>
                <button className={viewFilter === 'archived' ? 'button active categories-view-button' : 'button categories-view-button'} type="button" onClick={() => setViewFilter('archived')}>Archived ({archivedCount})</button>
              </div>
            </div>
            <button className="button primary transactions-add-button" type="button" onClick={openCreateModal}>Add Category</button>
          </div>
          {formError ? <p className="form-error budget-form-error">{formError}</p> : null}
        </div>

        <div className="categories-card-grid categories-card-scroll status-grid">
          {visibleCategories.map((item) => (
            <article key={item.id} className={`categories-card rich-chip ${item.archived ? 'is-archived' : ''}`}>
              <div className="categories-card-head">
                <div className="categories-card-title-wrap">
                  <span className="categories-color-swatch" style={{ backgroundColor: resolveCategoryColor(item.color) }} aria-hidden="true" />
                  <div>
                    <strong>{item.name}</strong>
                  </div>
                </div>
                <span className={`badge ${item.archived ? 'neutral' : item.type === 'EXPENSE' ? 'warn' : 'success'}`}>{item.archived ? 'Archived' : item.type === 'EXPENSE' ? 'Expense' : 'Income'}</span>
              </div>
              <div className="categories-card-body">
                <div className="categories-card-detail">
                  <span>Type</span>
                  <strong>{item.type === 'EXPENSE' ? 'Expense' : 'Income'}</strong>
                </div>
                <div className="categories-card-detail">
                  <span>Color</span>
                  <strong>{item.color ? resolveCategoryColor(item.color) : 'Default'}</strong>
                </div>
                <div className="categories-card-detail">
                  <span>Status</span>
                  <strong>{item.archived ? 'Locked' : 'Active'}</strong>
                </div>
              </div>
              <div className="categories-card-actions">
                <button className="button ghost small transaction-action-button edit-action-button" type="button" onClick={() => openEditModal(item)}>Edit</button>
                <button className="button ghost small transaction-action-button danger-button delete-action-button" type="button" onClick={() => setConfirmAction({ id: item.id, mode: item.archived ? 'unarchive' : 'archive' })}>{item.archived ? 'Unarchive' : 'Archive'}</button>
              </div>
            </article>
          ))}
        </div>
      </section>

      {showCategoryModal ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={closeModal}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-form-modal account-dialog-card category-dialog-card" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>{activeModalTitle}</h2>
                <p>{activeModalCopy}</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={closeModal} aria-label="Close category modal" />
            </div>
            <div className="form-grid structured-form-grid app-form-grid category-form-grid">
              <div className="modal-field-wrap"><input placeholder="Category name" value={form.name} onChange={(event) => updateForm('name', event.target.value)} /></div>
              <div className="modal-field-wrap"><AppSelect value={form.type} onChange={(value) => updateForm('type', value)} options={categoryTypeOptions} placeholder="Select type" /></div>
              <div className="modal-field-wrap category-color-input-wrap">
                <input placeholder="#19e0c5" value={form.color} onChange={(event) => updateForm('color', event.target.value)} />
                <span className="category-selected-color-chip">
                  <span className="categories-color-swatch categories-color-swatch-inline" style={{ backgroundColor: selectedColor }} aria-hidden="true" />
                </span>
              </div>
              <div className="category-color-field">
                <div className="category-color-field-copy">
                  <strong>Color palette</strong>
                  <p>Select a preset or fine-tune with the picker.</p>
                </div>
                <div className="category-color-palette">
                  {categoryColors.map((color) => (
                    <button
                      key={color}
                      type="button"
                      className={`category-color-option ${selectedColor.toLowerCase() === color.toLowerCase() ? 'active' : ''}`}
                      style={{ backgroundColor: color }}
                      onClick={() => updateForm('color', color)}
                      aria-label={`Use ${color}`}
                    />
                  ))}
                  <label className="category-color-custom" aria-label="Pick custom color">
                    <span className="category-color-picker-icon" aria-hidden="true" />
                    <input type="color" value={selectedColor} onChange={(event) => updateForm('color', event.target.value)} />
                  </label>
                </div>
              </div>
              {formError ? <p className="form-error">{formError}</p> : null}
              <div className="modal-actions transaction-modal-actions category-modal-actions">
                <div className="category-modal-side-action">
                  {editingCategoryId ? <button className="button ghost category-modal-toggle-button" type="button" onClick={() => setConfirmAction({ id: editingCategoryId, mode: editingCategoryArchived ? 'unarchive' : 'archive' })}>{modalToggleLabel}</button> : null}
                </div>
                <div className="category-modal-main-actions">
                  <button className="button ghost" type="button" onClick={closeModal}>Cancel</button>
                  <button className="button primary" type="button" onClick={saveCategory}>{editingCategoryId ? 'Save Changes' : 'Create Category'}</button>
                </div>
              </div>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}

      {confirmAction ? createPortal(
        <div className="modal-backdrop transaction-modal-backdrop" onClick={() => setConfirmAction(null)}>
          <div className="modal-card modal-card-structured solid-modal-card transaction-dialog-card transaction-confirm-modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-head overlay-head">
              <div>
                <h2>{confirmAction.mode === 'archive' ? 'Archive Category' : 'Unarchive Category'}</h2>
                <p>{confirmAction.mode === 'archive' ? 'Confirm archive for this category. Existing records remain unchanged.' : 'Restore this category so it can be used again in the app.'}</p>
              </div>
              <button className="icon-button quiet close-icon-button" type="button" onClick={() => setConfirmAction(null)} aria-label="Close category state modal" />
            </div>
            <div className="delete-confirm-copy">
              <strong>{confirmTarget?.name ?? 'Category'}</strong>
              <p>{confirmTarget ? `${confirmAction.mode === 'archive' ? 'Move' : 'Restore'} ${confirmTarget.name} ${confirmAction.mode === 'archive' ? 'to archived categories.' : 'to active categories.'}` : 'This action cannot be undone.'}</p>
            </div>
            <div className="modal-actions delete-modal-actions">
              <button className="button ghost" type="button" onClick={() => setConfirmAction(null)}>Cancel</button>
              <button className="button primary delete-confirm-button" type="button" onClick={confirmCategoryStateChange}>{confirmAction.mode === 'archive' ? 'Archive' : 'Unarchive'}</button>
            </div>
          </div>
        </div>,
        document.body,
      ) : null}


    </>
  );
}








