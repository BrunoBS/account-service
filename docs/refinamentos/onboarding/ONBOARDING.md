# Onboarding

Mantém a capacidade, os contratos e os conceitos reais. Organização por responsabilidade: `completion` contém WorkspaceOnboardingCompletion, repository, comando de registro de conclusão de fase e adapter; `progress` contém facade, query/command de progresso e validator; `phase` contém outputs/status de fase.

Não foi criada entidade Onboarding artificial. A relação com Workspace, os adaptadores acionados pelas fases e as regras de requester/status permanecem iguais. API `/api/v1/workspaces/{workspaceIdentifier}/onboarding` e tabela `workspace_onboarding_completions` já usam nomenclatura correta; não existe mudança física nem migration vazia.

[Execução e testes](../features-golden/EXECUCAO.md).
