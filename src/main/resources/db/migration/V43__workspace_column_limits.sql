-- Match Workspace storage limits to its structural resource schema.
ALTER TABLE workspaces
DROP FOREIGN KEY fk_workspaces_workspace_type,
DROP FOREIGN KEY fk_workspaces_lifecycle;

ALTER TABLE workspaces
MODIFY COLUMN workspace_type_code VARCHAR(20) NOT NULL,
MODIFY COLUMN name VARCHAR(80) NOT NULL,
MODIFY COLUMN description VARCHAR(200) NOT NULL,
MODIFY COLUMN requester VARCHAR(10) NOT NULL,
MODIFY COLUMN authorizer_group VARCHAR(20) NULL,
MODIFY COLUMN email_group VARCHAR(200) NOT NULL,
MODIFY COLUMN lifecycle_code VARCHAR(20) NOT NULL,
ADD CONSTRAINT fk_workspaces_workspace_type FOREIGN KEY (workspace_type_code) REFERENCES type_workspaces (code),
ADD CONSTRAINT fk_workspaces_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code);

ALTER TABLE workspace_approvers
MODIFY COLUMN functional VARCHAR(10) NOT NULL,
MODIFY COLUMN email VARCHAR(200) NOT NULL;

-- System tags include the Workspace name (up to 80 characters).
-- Manual tags are limited to 50 characters by the Workspace resource schema.
ALTER TABLE workspace_tag
MODIFY COLUMN name VARCHAR(80) COLLATE utf8mb4_unicode_ci NOT NULL;
