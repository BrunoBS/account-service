-- Workspace types are restricted to ADMIN and MANAGER.
UPDATE type_workspaces
SET
    is_active = false
WHERE
    code = 'CATALOG';

ALTER TABLE workspaces
ADD CONSTRAINT ck_workspaces_supported_type CHECK (workspace_type_code IN ('ADMIN', 'MANAGER'));
