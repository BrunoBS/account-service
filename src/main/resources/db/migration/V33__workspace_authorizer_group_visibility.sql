UPDATE workspaces
SET
    authorizer_group = NULLIF(UPPER(TRIM(authorizer_group)), '')
WHERE
    authorizer_group IS NOT NULL;

CREATE INDEX idx_workspaces_authorizer_group ON workspaces (authorizer_group);
