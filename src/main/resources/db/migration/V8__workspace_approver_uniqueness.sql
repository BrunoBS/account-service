ALTER TABLE workspace_approvers
    ADD CONSTRAINT uk_workspace_approvers_functional
        UNIQUE (workspace_id, functional),
    ADD CONSTRAINT uk_workspace_approvers_email
        UNIQUE (workspace_id, email);
