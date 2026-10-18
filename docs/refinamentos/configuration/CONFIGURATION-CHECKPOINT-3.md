# Configuration V1 — checkpoint 3: publicação e Redis

> Registro histórico anterior à renomeação. O contrato atual é [DynamicParameter / ParameterKey](../dynamic-parameter/DYNAMIC-PARAMETER.md).

Referência: `guideline-java/docs/refinamento/14-configuration/configuration-refinement-v1.md`, [plano](CONFIGURATION-CHECKPOINT-1.md) e [checkpoint 2](CONFIGURATION-CHECKPOINT-2.md).

Status: **checkpoint 3 concluído, verificação focada e regressão completa aprovadas**. O usuário autorizou a próxima fase após validar o checkpoint 2. A dependência `spring-boot-starter-data-redis`, previamente apresentada no plano do checkpoint 3, foi incluída sob a versão gerenciada pelo baseline. Nenhuma dependência de lock foi adicionada.

## Contratos de publicação

| Rota | Entrada | Resultado |
| --- | --- | --- |
| PATCH `/api/v1/configurations/{identifier}/publish` | UUID no path; sem body obrigatório | `databaseCommitted`, metadados de `configuration` e `synchronization` |
| POST `/api/v1/configurations/publish-batch` | `{"identifiers":["uuid", "..."]}` | `databaseCommitted` para o lote e metadados/sincronização por item, na ordem solicitada |

OWNER em ambas as operações, usando AuthorizationAction.UPDATE, como a publicação existente de Schema. As respostas não carregam value/publishedValue. SYNCED significa que o runtime foi alinhado ao estado atual commitado; PENDING indica runtime desabilitado, contenção ou perda da lease; FAILED sinaliza erro técnico de sincronização. O banco pode estar confirmado com Redis pendente/falho. Não há atomicidade distribuída.

Somente ativa/DRAFT pode publicar. Uma nova publicação de PUBLISHED é rejeitada; a recuperação Redis ocorre pela reconciliação, sem gerar nova versão/evento de publicação. Create/Update/Activate continuam sem publicação automática. Edição mantém o snapshot anterior até nova publicação explícita.

## Transação e validação

- ConfigurationPublicationService coordena uma operação explícita fora de transações de chamadores. ConfigurationPublicationOperations abre a transação de escrita, com locks em ordem determinística de UUID.
- Carregar e validar todos os registros antes da primeira mutação: existência, ativa/DRAFT, tipo disponível e JSON pelo contrato vigente `PLATFORM/configuration`. O envelope de validação contém version/type/value e usa o ramo de edição do mesmo schema. Não existe schema extra de expressão.
- saveAllAndFlush persiste o conjunto; os metadados vêm das entidades retornadas após o flush. A auditoria da biblioteca já aceita Collection e gera um snapshot/evento por Configuration, na mesma transação.
- Falhas na validação, persistência, auditoria ou commit cancelam tudo. Synchronization callbacks não executam em rollback.
- V74 publica apenas o contrato da operação em lote, `configuration-publication`, com UUIDs, lista não vazia, sem itens repetidos ou campos extras. Duplicidade de identidade após canonicalização de UUID também é verificada no use case.
- A sincronização é registrada em TransactionSynchronization.afterCommit. Receipts transitórios permitem produzir a resposta após a confirmação do commit e os resultados de runtime; não há campo persistido extra, cache local, histórico de Configuration, fila ou outbox de publicação. A auditoria existente continua usando sua própria infraestrutura.
- Inactivate, inclusive retry idempotente, e Delete registram remoção/sincronização após commit. Respostas CRUD mantêm o contrato de metadados/204 do banco; falhas runtime são registradas e recuperadas pelo scheduler.

## Padrão de consultas

Após a revisão solicitada pelo usuário, ConfigurationRepository e as referências de Microservice usam **interfaces de projeção em repository/projection**, com aliases JPQL. Os QueryServices fazem o mapeamento para seus contratos. Repositories não constroem DTOs do use case.

Detalhe/listagem administrativos preservam filtros, paginação, ordenação estável e obtenção de referências em lote. A reconciliação usa Slice/Pageable com cursor por último ID para evitar salto por mudança de offsets durante a varredura. As referências do pai passam pelo Port/Adapter e pela query pública do módulo Platform; não há acesso ao repository/entity de outro módulo.

## Sincronização concorrente

Um serviço único atende publicação, inativação/exclusão e reconciliação. Ele suspende qualquer contexto transacional remanescente, adquire a lease Redis, relê o estado em uma transação READ ONLY REQUIRES_NEW e encerra essa leitura antes da escrita Redis. O adapter recusa operações com transação de banco ativa.

A releitura usa o estado **atual** por fullKey. Somente active=true e published_value SQL NOT NULL produz envelope. DRAFT com publicação anterior continua usando **published_type_code e published_value**, nunca o tipo/valor editável. Literal JSON null continua presente e é publicado como `{"type":"CONFIGURATION","value":null}`.

- Chave física: `config:<microservice.code>.<key>`.
- Envelope: `{"type":"CONFIGURATION|EXPRESSION","value":...}`.
- Coordenação: `configuration:runtime:lease`, fora do prefixo de dados.
- Token de proprietário UUID, aquisição SET NX com expiração; Lua confirma o token em cada SET/DEL e na liberação.
- Lease perdida/expirada impede o comando atrasado de sobrescrever/remover estado de outra instância. Liberação por dono antigo não libera a lease atual.
- Valores de configuração **sem TTL**. Só a lease expira.
- Runtime exceptions não propagam payload/cause/messages aos logs; logs informam chave, identificador quando disponível e classe do erro, com MDC da plataforma quando há contexto de request. O pipeline mantém DefaultLogSanitizer; não há filtro/sanitizador próprio da feature.

A fonte de verdade permanece MySQL. Falha Redis não desfaz commit, e um timeout pode ter resultado indeterminado no Redis. Idempotência, token de propriedade e reconciliação recuperam a divergência.

## Reconciliação e operação

ConfigurationReconciliationService percorre active/publishedValue presentes em páginas, incluindo DRAFT, e repõe conteúdo ausente/desatualizado. SCAN é limitado a `config:*`. Cada candidato é relido sob a coordenação antes de alterar runtime, protegendo mudanças concorrentes e recriação após exclusão. Chaves fora desse prefixo não são tocadas.

Runtime e scheduler são opt-in e desabilitados por default. O scheduler usa Spring @Scheduled, aproximadamente horário, com initial delay e fixed delay iguais ao intervalo configurado. Os valores publicados podem ficar indisponíveis até a sincronização/reconciliação concluir.

Configuração de exemplo, a aplicar no ambiente que executa o produtor:

```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
      database: 0
      connect-timeout: 2s
      timeout: 2s
platform:
  configuration:
    runtime:
      enabled: true
      lease-duration: PT30S
      scan-count: 100
    reconciliation:
      enabled: true
      interval: PT1H
      batch-size: 100
```

Não foi adicionado override de logging ou parsing JSON ao application.yml. São usados os defaults solicitados do Jackson e da biblioteca. Host/port/database/credenciais/TLS de Redis seguem Spring Boot.

**Precondições operacionais:** Redis standalone e database/namespace `config:*` exclusivo desta instalação. A coordenação Lua envolve a lease global e a chave de dados; a V1 não oferece suporte a Redis Cluster com chaves em slots diferentes. DEV/HOMO/PROD usam instalações e conexões próprias, sem dimensão lógica de ambiente no modelo.

Se runtime ficar desabilitado, a publicação confirma MySQL e retorna PENDING; não haverá recuperação Redis até runtime ser habilitado. Para recuperação periódica, habilitar também reconciliação. Intervalos/timeouts/lease são configuráveis; uma lease curta demais pode impedir processamento que exceda seu prazo.

## Evidências

- Compile com Redis gerenciado: BUILD SUCCESS. `/private/tmp/configuration-publication-compile.log`.
- Verify focado final: **175 testes aprovados** — 125 unitários/arquiteturais e 50 integrações (17 API CRUD/schema, 15 publicação, 7 Redis/concorrência, 5 reconciliação, 4 concorrência de Configuration e 2 migration/armazenamento). Sem falhas, erros ou skips. Concluído em 14/10/2026 às 22:46:47 (America/Sao_Paulo), em 2min16s. Log `/private/tmp/configuration-publication-focused.log`.
- Publicação comprova commit antes da escrita Redis, ausência de transação de banco nas operações Redis, JSON em todas as formas e literal null, ausência de TTL, OWNER/401/403, lista validada integralmente, versões/auditoria e rollback por falha de persistência depois do flush ou de auditoria depois do primeiro item.
- Simultâneas: um vencedor na publicação individual; lotes sobrepostos/reversos sem deadlock; publicação recarrega a versão atual mesmo após snapshot/cache JPA anterior.
- Redis real: tokens errados/expirados não escrevem/apagam/liberam; escritor antigo atrasado perde propriedade e não sobrescreve publicação mais recente; inativação antes da releitura impede publicação obsoleta.
- Falha real de conexão preserva publicação commitada; restauração com conexão funcional recupera a chave. Lote diferencia item FAILED/SYNCED sem rollback fictício.
- Reconciliação recupera Redis vazio/desatualizado em múltiplas páginas, restaura tipo/valor publicado anterior em DRAFT, limpa órfãs/inativas/nunca publicadas e preserva namespaces alheios. Inativação repetida e delete removem resíduos; reativação não publica. Inativar Microservice não cria cascata não prevista.
- Logs de falha com sentinela não carregam valor ou exception message/cause.
- Testes usam MySQL 8.4.11 e Redis em container dedicado, com imagem local fixada por digest `redis@sha256:8b81dd37ff027bec4e516d41acfbe9fe2460070dc6d4a4570a2ac5b9d59df065`.
- Regressão completa `mvn --offline --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**, **541 testes** (125 unitários/arquiteturais + 416 integrações), sem falhas, erros, skips ou flakes. Concluído em 14/10/2026 às 23:03:26 (America/Sao_Paulo), em 11min34s. Log `/private/tmp/configuration-full.log`; relatórios Surefire/Failsafe confirmam os totais. Inclui APIs, autorização, arquitetura, concorrência, auditoria, migrations/upgrade/checksums, JPA, Maven Enforcer e convergence de todos os módulos. `git diff --check` aprovado.

## Arquivos e próximo checkpoint

Produção: publication service/operations, validador de publicação, callbacks after-commit, runtime query/synchronization/reconciliation, Port/Adapter Redis e scripts Lua, properties/settings/scheduler, interfaces de projeção e mapeadores, DTOs/rotas de publicação, schema/V74, bundles e starter Redis. Testes: suporte Redis isolado, PublicationIT, RedisIT, ReconciliationIT, RuntimeSettingsTest, arquitetura e ajustes nas fixtures/testes do checkpoint 2.

Trabalho na branch `feature/segments-categories-v1`, preservando segmentos e alterações anteriores. Não houve alteração da platform-libraries, commit/push/merge.

O próximo checkpoint é **4 — biblioteca de consumo**: ConfigurationProvider/ConfigurationValue, Redis exclusivo, conversões/path/matches/defaults e autoconfiguração. A seção 8 do refinamento exige aprovação explícita antes de avançar. A implementação da biblioteca ainda não foi iniciada.
