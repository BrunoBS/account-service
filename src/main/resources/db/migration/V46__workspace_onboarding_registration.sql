CREATE TABLE workspace_onboarding_completions
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    workspace_id         BIGINT       NOT NULL,
    onboarding_phase_code VARCHAR(50)  NOT NULL,
    user_identifier      VARCHAR(200) NOT NULL,
    completed_at         DATETIME(6)  NOT NULL,
    CONSTRAINT pk_workspace_onboarding_completions PRIMARY KEY (id),
    CONSTRAINT uk_workspace_onboarding_phase UNIQUE (workspace_id, onboarding_phase_code),
    CONSTRAINT fk_workspace_onboarding_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_workspace_onboarding_phase FOREIGN KEY (onboarding_phase_code) REFERENCES type_onboardings (code)
);

INSERT INTO type_onboardings (code, label, description, sort_order, is_active, settings)
VALUES
    ('WORKSPACE_REGISTRATION', 'Cadastro do workspace', 'Cadastro dos dados principais do workspace.', 1, TRUE, '{"orientation":"Revise os dados principais do workspace."}'),
    ('WORKSPACE_FIRST_ENVIRONMENT', 'Configuração do ambiente do workspace', 'Configuração do primeiro ambiente do workspace.', 2, TRUE, '{"orientation":"Configure o ambiente padrão do workspace quando a configuração de ambientes estiver disponível."}'),
    ('FIRST_APPLICATION_REGISTRATION', 'Cadastro da primeira aplicação', 'Cadastro da primeira aplicação vinculada ao workspace.', 3, TRUE, '{"orientation":"Cadastre a primeira aplicação vinculada ao workspace."}'),
    ('APPLICATION_FIRST_ENVIRONMENT', 'Configuração do ambiente da aplicação', 'Configuração do primeiro ambiente da aplicação.', 4, TRUE, '{"orientation":"Configure o ambiente padrão da aplicação quando a configuração de ambientes estiver disponível."}');
