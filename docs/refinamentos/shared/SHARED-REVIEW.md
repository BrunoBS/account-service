> Histórico. Consulte [Resource Sharing](../resource-sharing/RESOURCE-SHARING.md) para contratos e nomes atuais.

# Revisão da feature Shared — 2026-10-10

## Correções confirmadas

| Problema                                                                                 | Evidência                                                                         | Correção                                                                                                              |
| ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| GETs duplicados após padronizar `/shared-contracts`                                      | Spring não iniciava por `Ambiguous mapping`                                       | Contratos próprios em `/shared-contracts`; descoberta em `/shared-contracts/available`, com detalhe no mesmo prefixo. |
| Reenviar configuração com o mesmo destino causava 409                                    | MySQL rejeitava INSERT antes do DELETE pela chave `uk_shared_mapping_destination` | Reutilizar os mapeamentos persistidos por destino; atualizar a origem e remover somente vínculos ausentes.            |
| Filtro por nome de aplicação participante inativa causava 500                            | `UnexpectedRollbackException` após capturar exceção de serviço transacional       | Ler a referência histórica diretamente, sem tentar uma consulta ativa que lança exceção.                              |
| Workspace proprietário inativo bloqueava histórico do participante                       | GET de participações retornava 404 após inativação                                | Separar resolução histórica de UUIDs da validação ativa usada em comandos.                                            |
| Proprietário perdia a consulta de participações rejeitadas/revogadas ou contrato inativo | Filtros fixos escondiam esses estados                                             | Preservar histórico na listagem e detalhe; todos os status válidos podem ser consultados.                             |
| Exclusões não forneciam snapshot do recurso à auditoria                                  | Métodos auditados retornavam `void`                                               | Retornar o snapshot ao aspecto, mantendo HTTP 204 no controller.                                                      |
| Ambientes globais com ancestral inativo eram oferecidos para aprovação                   | Descoberta validava só o lifecycle do próprio ambiente                            | Aplicar a validação da cadeia de ancestrais também aos ambientes globais.                                             |

## Persistência e consultas

- `SharedContract.participations` foi removido. A consulta usa repositório; o banco mantém o cascade de exclusão de participações e mapeamentos.
- `SharedParticipation.mappings` continua lazy e é necessária à gestão de configuração e orphan removal. As listagens carregam os vínculos junto com a consulta, e a conversão de IDs de ambiente em UUIDs usa leitura em lote por participação.
- Aprovação e alteração da configuração reutilizam validação e aplicação atômica; os eventos de auditoria são distintos.
- Os testes da API executam com auditoria habilitada, incluindo eventos de exclusão e ausência de eventos em operações revertidas.
- Consultas de histórico aceitam referências inativas, mas comandos e descoberta continuam exigindo escopos ativos.
- Testes verificam isolamento entre aplicações, ambiente de outro workspace, disponibilidade, paginação, transições, substituição de mapeamentos e rollback.

## Decisões funcionais ainda abertas

- O relacionamento com `platform_features` foi definido e implementado: `featureId` obrigatório, FK, UUID público e feature imutável no contrato. Features precisam ser ativas e `shareable = true` para compartilhamento. A edição bloqueia `shareable = false` enquanto existir qualquer contrato vinculado. A flag tem padrão `false`.

- As listagens de participações e contratos próprios ainda não são paginadas; volumes grandes justificam paginação. A descoberta já é paginada.
- A auditoria da biblioteca registra snapshots posteriores nas ações UPDATE; ela não fornece automaticamente um diff com valores anteriores e novos. As configurações podem ser reconstruídas pela sequência de eventos. Um diff explícito exige ampliar o contrato de auditoria.

## Validação

Regressões reproduzidas antes da correção: falha de inicialização, conflito ao repetir configuração, erro 500 no filtro por nome e erro 404 no histórico.

Validação final: suíte unitária/arquitetura, `SharedApiIT` com auditoria habilitada e `SharedInternalReferencesMigrationIT` (upgrade com dados históricos).

### Validação do relacionamento com features

- `shareable` tem padrão `false`; só features ativas e compartilháveis podem ser associadas a novos contratos.
- A edição da feature retorna 409 enquanto houver qualquer contrato, inclusive inativo, ao tentar desmarcar `shareable`.
- A mudança é liberada após excluir o último contrato; alterações mantendo `shareable = true` continuam permitidas.
- O teste concorrente confirma que criar um contrato e desabilitar `shareable` não deixam um contrato associado a uma feature não compartilhável.
- Resultado: 78 testes unitários/arquitetura, 16 testes da API com auditoria e 1 teste de migração passaram.

## Reutilização de referências internas

- Comandos e consultas usam `SharedReferenceAdapter` para resolver referências e montar as saídas da feature.
- A resolução conjunta faz uma única busca do workspace e uma busca da aplicação no workspace, mantendo as validações de atividade e visibilidade existentes.
- Ao editar um contrato já carregado, a validação de nome duplicado usa os IDs do proprietário persistidos no contrato.
- As consultas de contrato e participação do proprietário compartilham a busca do contrato pelo escopo; ativação e exclusão também reutilizam a busca existente nos comandos.
- A construção de mapeamentos resolve o ID do ambiente de origem uma vez por origem, reutilizando-o para todos os destinos da entrada.

## Detalhe de participação e disponibilidade da feature — etapa intermediária, 13/10/2026

A visão do proprietário devolve sourceEnvironments no endpoint:

```http
GET /api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/shared-contracts/{contractIdentifier}/participations/{participationIdentifier}
```

O fluxo é SharedOwnerController.findParticipation → SharedOwnerFacade.participation
→ SharedParticipationQueryService.findOwnerParticipationDetails. A oferta de
ambientes considera contrato ativo, disponibilidade da feature/microservice e
aplicação/workspace participante ativos. A feature deve continuar compartilhável.
A consulta reutiliza SharedReferenceAdapter.isFeatureAvailable, sem duplicar a
regra ou modificar o contrato público da resposta.

Feature ou microservice inativo produz sourceEnvironments vazio, preservando a
participação, o lifecycle do contrato e os mappings históricos. Reativação restaura
a lista se as demais referências permanecerem elegíveis. Listagens e a visão do
participante continuam preservando histórico, sem incluir sourceEnvironments.

Verificação específica: 107 testes unitários/arquitetura e 52 testes da API Shared
aprovados, sem falhas, erros ou skips. O novo cenário usa inativação/reativação
reais da feature e do microservice, mantém o contrato ativo e a participação aprovada,
preserva mappings e confirma sourceEnvironments vazio/restaurado. A regressão
inclui contratos/participantes inativos, histórico, autorização, ambientes e
concorrência existentes. `git diff --check` aprovado.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=SharedApiIT verify
```

Log: `/private/tmp/shared-feature-availability-verify.log`.

## Erro de negócio nos detalhes com feature indisponível — 13/10/2026

A regra final de leitura individual substitui a etapa intermediária acima. Buscar
um contrato ou sua participação com feature indisponível retorna HTTP 409 pelo
messaging da plataforma: shared.contract.feature-unavailable (SHARED-0013), com
mensagem e solução localizadas. A condição abrange feature inativa, microservice
inativo e feature não compartilhável.

O guard fica em SharedContractAccessService.requireFeatureForDetails e é chamado
pelas consultas individuais após conferir recurso/escopo. O erro não devolve
histórico ou mappings no detalhe; os dados permanecem persistidos. Campos
availability não foram adicionados aos DTOs. Listagens administrativas e comandos
de inativação/exclusão mantêm sua finalidade de gestão, permitindo limpar os dados
mesmo com feature inativa. Reativação permite voltar a consultar o detalhe.

Verificação específica: 107 testes unitários/arquitetura e 52 testes de Shared
aprovados, sem falhas, erros ou skips. Cobrem erro 409 com código/mensagem do
messaging e ausência de histórico/mappings/sourceEnvironments no corpo de erro,
inativação da feature e do microservice, reativação, escopo incorreto (404),
autorização (403), ausência de eventos de auditoria nas leituras e limpeza por
inativação/exclusão do contrato com a feature inativa. `git diff --check` aprovado.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=SharedApiIT verify
```

Log: `/private/tmp/shared-feature-business-error-verify.log`.
