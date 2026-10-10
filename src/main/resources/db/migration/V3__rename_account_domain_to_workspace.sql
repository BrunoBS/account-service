ALTER TABLE account_approvers
DROP FOREIGN KEY fk_account_approvers_account;

RENAME TABLE accounts TO workspaces,
account_approvers TO workspace_approvers;

ALTER TABLE workspaces
DROP CHECK ck_accounts_account_type,
DROP CHECK ck_accounts_lifecycle,
RENAME COLUMN account_type TO workspace_type,
RENAME INDEX uk_accounts_identifier TO uk_workspaces_identifier,
RENAME INDEX uk_accounts_name TO uk_workspaces_name,
RENAME INDEX idx_accounts_lifecycle TO idx_workspaces_lifecycle,
RENAME INDEX idx_accounts_account_type TO idx_workspaces_workspace_type;

ALTER TABLE workspaces
ADD CONSTRAINT ck_workspaces_workspace_type CHECK (workspace_type IN ('ADMIN', 'MANAGER')),
ADD CONSTRAINT ck_workspaces_lifecycle CHECK (lifecycle IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE workspace_approvers
RENAME COLUMN account_id TO workspace_id,
RENAME INDEX idx_account_approvers_account_id TO idx_workspace_approvers_workspace_id;

ALTER TABLE workspace_approvers
ADD CONSTRAINT fk_workspace_approvers_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE;

UPDATE tags
SET
    owner_type = 'WORKSPACE'
WHERE
    owner_type = 'ACCOUNT';
