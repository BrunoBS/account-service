# Resource Sharing

Capacidade de compartilhamento com Contract e Participant. Contract define o destino; Participant identifica a origem autorizada. Preserva relação 1:N, escopos, autorização, políticas de transição, publication mode e mapeamento de ambientes. `EnvironmentMapping` permanece componente real de Participant; não é uma nova entidade inventada.

Cada camada se organiza por `contract` e `participant`. Validação e referências realmente comuns permanecem na camada apropriada. Não há prefixos SharedContract/SharedParticipation.

Rotas sob `/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/resource-sharing`: `contracts`, `participants`; itens de contrato em `/contracts/{contractIdentifier}/participants`. Parâmetro de item `participantIdentifier`; ações e códigos de status preservados. Sem aliases.

V80 renomeia tabelas para `resource_sharing_contracts`, `resource_sharing_participants`, `resource_sharing_environment_mappings` e coluna `participant_id`. Renomeia índices e FKs, preservando dados, versões, referências e cascatas. Instalação limpa e upgrade com dados são testados. Não altera o enum SHARED de escopo da Application, que é outro conceito de negócio.

[Checkpoint e resultados](../features-golden/EXECUCAO.md).
