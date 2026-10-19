# Parameter — parâmetros dinâmicos

Este documento descreve o contrato implementado atualmente pela feature `parameter` do
Workspace Service. A classe de fachada ainda se chama `DynamicParameterFacade`, e a
API usa `/dynamic-parameters`; esses nomes explícitos distinguem parâmetros de runtime
de propriedades de configuração do próprio serviço.

## Organização atual

- Feature: `feature.parameter`.
- Agregado: `ParameterKey`, em `feature.parameter.domain`.
- Serviços: `ParameterKeyCommandService` (CRUD e lifecycle),
  `ParameterKeyQueryService` (consultas) e `ParameterKeyPublisherService` (publicação
  manual, reconciliação agendada e remoção no Redis).
- Entrada HTTP: `entrypoint.parameter.ParameterKeyController`.
- Propriedades próprias da feature: `feature.parameter.config.ParametersConfig`.
- Configuração comum da aplicação: `config.WorkspaceProperties`, prefixo
  `workspace.parameter`.
- Redis: capability genérica `platform-redis` (`RedisStore`); a feature define o
  namespace e os valores de domínio.

O domínio e as camadas que têm um único subdomínio não criam um pacote redundante
`parameterkey`. As operações permanecem na camada `usecase.operations` porque CRUD,
consultas e publicação têm responsabilidades distintas. Não há `Finder`,
`LifecycleOperations`, `PublicationOperations`, serviço de runtime separado nem logger
específico da feature.

## Contrato HTTP e lifecycle

| Operação | Contrato |
| --- | --- |
| CRUD e listagem | `/api/v1/dynamic-parameters`; leitura paginada e mutações OWNER |
| Catálogos | `/api/v1/parameter-key-type` e `/api/v1/parameter-key-status-type` |
| Publicação | `POST /api/v1/dynamic-parameters/publish`, body `{"identifiers":[...]}` |
| Resposta da publicação | `202 Accepted`, sem corpo |

Uma chave é criada ativa em `DRAFT`, sem snapshot publicado. Editar tipo ou valor
mantém o snapshot atual e retorna o status para `DRAFT`; editar somente descrição
preserva o status. Publicar copia `value` e `type` para o snapshot publicado e altera o
estado para `PUBLISHED`. Inativar define `active=false`, limpa o snapshot e agenda a
remoção do Redis após o commit. Reativar mantém `DRAFT` e não publica automaticamente.
Exclusão física é limitada a chaves inativas.

A publicação valida o formato e a autorização antes de aceitar o pedido. Um executor
Spring processa os identifiers fora da thread HTTP. Cada chave usa sua própria
transação e auditoria; uma falha individual não impede as demais. O estado persistido
é confirmado antes do envio ao Redis. O `202` confirma aceitação em memória, não a
entrega durável: ainda não há tabela de publicação, consulta de resultados ou retry
persistido.

## Redis e reconciliação

O serviço publica usando `RedisStore.setAll`, com lotes de até 500 chaves por padrão.
O scheduler de `ParameterKeyPublisherService` consulta páginas de chaves ativas com
snapshot publicado e reenvia seus valores a cada hora. O namespace configurado no
`application.yml` é `dynamic-parameter`; a chave lógica é
`<microservice.code>.<key>`, o envelope é `{type,value}` e não há TTL definido pela
feature. A capability Redis permanece genérica e não contém conceitos de Parameter.

Propriedades disponíveis (definidas em `WorkspaceProperties`):

```yaml
workspace:
  parameter:
    runtime:
      enabled: true
      batch-size: 500
    reconciliation:
      enabled: true
      interval: PT1H
```

`runtime.enabled` controla escrita e remoção no Redis; `runtime.batch-size` controla
os lotes manuais e o tamanho das páginas do scheduler. `reconciliation.enabled`
controla o agendamento; `reconciliation.interval` define o atraso inicial e o
intervalo fixo entre execuções. O máximo de lote da library Redis deve ser maior ou
igual ao tamanho configurado nesta aplicação.

Não há SCAN, remoção de órfãos, tabela de publicação, coordenação distribuída ou
transação distribuída entre banco e Redis. Se o Redis falhar depois de o banco
confirmar a publicação, o snapshot permanece salvo e o scheduler pode reenviá-lo.
Remoções falhas não são reconstruídas pelo scheduler, que só consulta snapshots
ativos; repetir a operação tenta remover novamente.

## Persistência e validação

As migrations V75–V76 alinham nomes físicos e bindings de schema sem editar migrations
anteriores. O histórico e os testes de upgrade com dados estão registrados em
[EXECUCAO.md](../features-golden/EXECUCAO.md). O request é validado pelo schema
publicado antes da desserialização, e os validators dos casos de uso aplicam as regras
de negócio e de lifecycle.

Os erros de publicação e reconciliação são registrados com `LOG.error` pelo serviço.
Quando disponíveis, os logs incluem identifier, chave completa e valor publicado,
além da exceção capturada. Não existe `DynamicParameterErrorLogger` nem sanitizador
específico neste fluxo.

## Validação registrada

O último `clean verify` documentado em [EXECUCAO.md](../features-golden/EXECUCAO.md)
passou com 538 testes (137 unitários/arquiteturais e 401 de integração), sem falhas,
erros ou skips. As integrações usam MySQL e Redis reais via Testcontainers. Essa
contagem é o resultado daquele checkpoint, não uma garantia para builds posteriores.
