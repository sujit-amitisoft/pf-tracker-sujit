import { useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { api } from "../../services/api";
import { AppSelect } from "../../components/FormControls";
import { getPendingInviteToken, setPendingInviteToken } from "../../services/session";
import { showAppToast } from "../../services/toast";

type Account = { id: string; name: string; type: string; currentBalance: string; institutionName: string; shared: boolean; accessRole: string };
type Member = { userId: string; email: string; displayName: string; role: "OWNER" | "EDITOR" | "VIEWER"; owner: boolean; addedAt: string };
type PendingInvite = { id: string; email: string; role: "OWNER" | "EDITOR" | "VIEWER"; expiresAt: string; createdAt: string; accepted: boolean };
type InvitePreview = { accountName: string; invitedEmail: string; role: "OWNER" | "EDITOR" | "VIEWER"; ownerName: string; expiresAt: string; accepted: boolean };

export function SharedAccountsPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const inviteToken = searchParams.get("invite") ?? getPendingInviteToken() ?? "";
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<"EDITOR" | "VIEWER">("EDITOR");
  const [error, setError] = useState<string | null>(null);
  const accounts = useQuery({ queryKey: ["accounts", "shared-management"], queryFn: async () => (await api.get<Account[]>("/api/accounts")).data });

  const manageableAccounts = useMemo(() => (accounts.data ?? []).filter((item) => item.accessRole === "OWNER" || item.accessRole === "EDITOR" || item.shared), [accounts.data]);
  const selectedAccount = manageableAccounts.find((item) => item.id === selectedAccountId) ?? manageableAccounts[0] ?? null;

  useEffect(() => {
    if (!selectedAccountId && manageableAccounts[0]) {
      setSelectedAccountId(manageableAccounts[0].id);
    }
  }, [manageableAccounts, selectedAccountId]);

  const members = useQuery({
    queryKey: ["accounts", "members", selectedAccount?.id],
    enabled: Boolean(selectedAccount?.id),
    queryFn: async () => (await api.get<Member[]>(`/api/accounts/${selectedAccount?.id}/members`)).data,
  });

  const pendingInvites = useQuery({
    queryKey: ["accounts", "invites", selectedAccount?.id],
    enabled: Boolean(selectedAccount?.id && selectedAccount?.accessRole === "OWNER"),
    queryFn: async () => (await api.get<PendingInvite[]>(`/api/accounts/${selectedAccount?.id}/invites`)).data,
  });

  const invitePreview = useQuery({
    queryKey: ["shared-invite", inviteToken],
    enabled: Boolean(inviteToken),
    queryFn: async () => (await api.get<InvitePreview>(`/api/accounts/invites/${inviteToken}`)).data,
  });

  const accountOptions = manageableAccounts.map((item) => ({ value: item.id, label: `${item.name} (${item.accessRole})` }));
  const roleOptions = [
    { value: "EDITOR", label: "Editor" },
    { value: "VIEWER", label: "Viewer" },
  ];

  const invite = async () => {
    if (!selectedAccount) return;
    try {
      const { data } = await api.post<{ message: string }>(`/api/accounts/${selectedAccount.id}/invite`, { email: inviteEmail.trim(), role: inviteRole });
      setInviteEmail("");
      setError(null);
      showAppToast(data.message);
      await queryClient.invalidateQueries({ queryKey: ["accounts", "invites", selectedAccount.id] });
    } catch (err: any) {
      setError(err?.response?.data?.details?.[0] ?? err?.response?.data?.message ?? "Failed to invite member");
    }
  };

  const acceptInvite = async () => {
    if (!inviteToken) return;
    try {
      const { data } = await api.post<{ message: string }>(`/api/accounts/invites/${inviteToken}/accept`);
      showAppToast(data.message);
      setPendingInviteToken(null);
      setSearchParams({});
      await queryClient.invalidateQueries({ queryKey: ["accounts"] });
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Failed to accept invitation");
    }
  };

  const updateRole = async (member: Member, role: "EDITOR" | "VIEWER") => {
    if (!selectedAccount) return;
    await api.put(`/api/accounts/${selectedAccount.id}/members/${member.userId}`, { role });
    await queryClient.invalidateQueries({ queryKey: ["accounts", "members", selectedAccount.id] });
  };

  return (
    <div className="page-fit-layout shared-page-shell shared-page-layout">
      {inviteToken ? (
        <section className="glass-panel nested-panel shared-summary-panel invite-preview-panel">
          <div className="panel-head"><div><h2>Shared Account Invitation</h2><p>Review and accept your pending account invitation.</p></div></div>
          {invitePreview.data ? (
            <div className="top-categories-card compact-info-card">
              <strong>{invitePreview.data.accountName}</strong>
              <p>Invited as {invitePreview.data.role} by {invitePreview.data.ownerName}</p>
              <p>{invitePreview.data.invitedEmail} · Expires {new Date(invitePreview.data.expiresAt).toLocaleString()}</p>
              <div className="modal-actions compact-actions"><button className="button primary" type="button" onClick={acceptInvite}>Accept Invitation</button></div>
            </div>
          ) : invitePreview.isError ? <div className="empty-state">This invitation is invalid or expired.</div> : <div className="empty-state">Loading invitation...</div>}
        </section>
      ) : null}

      <section className="glass-panel nested-panel shared-summary-panel shared-panel">
        <div className="panel-head"><div><h2>Family Mode</h2><p>Manage shared access, email invites, and roles for collaborative accounts.</p></div></div>
        <div className="summary-row finance-summary-grid shared-summary-cards">
          <article className="summary-card finance-summary-card balance-card"><span>Accessible Accounts</span><strong>{manageableAccounts.length}</strong></article>
          <article className="summary-card finance-summary-card income-card"><span>Owned</span><strong>{manageableAccounts.filter((item) => item.accessRole === "OWNER").length}</strong></article>
          <article className="summary-card finance-summary-card expense-card"><span>Shared With You</span><strong>{manageableAccounts.filter((item) => item.accessRole !== "OWNER").length}</strong></article>
        </div>
      </section>

      <div className="report-grid structured-report-grid shared-grid">
        <section className="glass-panel nested-panel shared-selector-panel shared-panel">
          <div className="panel-head"><div><h2>Accounts</h2><p>Select a shared or owned account to review members.</p></div></div>
          <AppSelect className="shared-account-select" value={selectedAccount?.id ?? ""} onChange={setSelectedAccountId} options={accountOptions} placeholder="Select account" />
          <div className="rules-list-grid shared-account-list">
            {manageableAccounts.map((account) => (
              <button key={account.id} type="button" className={`rule-card shared-account-card shared-account-tile ${selectedAccount?.id === account.id ? "active" : ""}`} onClick={() => setSelectedAccountId(account.id)}>
                <div className="rule-card-head"><strong>{account.name}</strong><span className="status-chip active">{account.accessRole}</span></div>
                <p>{account.institutionName || account.type.replace(/_/g, " ")}</p>
                <span className="rule-meta">Balance ${account.currentBalance}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="glass-panel nested-panel shared-members-panel shared-panel">
          <div className="panel-head"><div><h2>Shared With</h2><p>Invite family members by email and adjust their role-based access.</p></div></div>
          {selectedAccount?.accessRole === "OWNER" ? (
            <div className="filters-bar structured-filters-bar rules-builder-grid shared-invite-grid shared-invite-form">
              <input className="shared-invite-input" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} placeholder="Invite by email" />
              <AppSelect className="shared-role-select" value={inviteRole} onChange={(value) => setInviteRole(value as "EDITOR" | "VIEWER")} options={roleOptions} placeholder="Role" />
              <button className="button primary shared-invite-button" type="button" onClick={invite}>Send Invite Email</button>
            </div>
          ) : <div className="top-categories-card">Only the account owner can invite or change members for this account.</div>}
          {error ? <p className="form-error">{error}</p> : null}
          {selectedAccount?.accessRole === "OWNER" && (pendingInvites.data ?? []).length ? (
            <div className="top-categories-card compact-info-card">
              <strong>Pending Invites</strong>
              {(pendingInvites.data ?? []).map((invite) => (
                <p key={invite.id}>{invite.email} · {invite.role} · expires {new Date(invite.expiresAt).toLocaleDateString()}</p>
              ))}
            </div>
          ) : null}
          <div className="shared-members-list shared-members-grid">
            {(members.data ?? []).map((member) => (
              <article key={member.userId} className="rule-card shared-member-card">
                <div className="rule-card-head"><strong>{member.displayName || member.email}</strong><span className={`status-chip ${member.owner ? "active" : "paused"}`}>{member.role}</span></div>
                <p>{member.email}</p>
                {!member.owner && selectedAccount?.accessRole === "OWNER" ? (
                  <div className="modal-actions compact-actions">
                    <button className="button ghost" type="button" onClick={() => updateRole(member, "EDITOR")}>Editor</button>
                    <button className="button ghost" type="button" onClick={() => updateRole(member, "VIEWER")}>Viewer</button>
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}


