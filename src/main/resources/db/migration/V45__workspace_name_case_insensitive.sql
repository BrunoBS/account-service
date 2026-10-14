-- Enforce global case-insensitive Workspace name uniqueness regardless of database defaults.
-- The existing uk_workspaces_name index uses the column collation.
ALTER TABLE workspaces
MODIFY COLUMN name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
