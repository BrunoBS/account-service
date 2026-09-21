# Plano — Adequação Arquitetural Pós-G4

## 1. Objetivo

Adequar o baseline funcional concluído na G4 ao padrão de `GOLDEN_REFERENCE_PADRAO_ARQUITETURAL.md`, preservando comportamento de negócio e incorporando explicitamente a decisão de domínio `Account -> Workspace`.

A mudança nominal de contratos externos/persistidos é a única alteração observável deliberada; ela será tratada por migration e atualização coordenada de testes/documentação.

## 2. Baseline

- Repositório: `BrunoBS/account-service`
- Branch analisada: `main`
- Commit analisado: `b49949638818f1229c78dfb93b0b6238e1a5e1fe`
- Head funcional da G4: `a4c238eea24022d984aed49ba8a18eeb9d0a4853`
- Branch de execução: `refactor/architectural-alignment-post-g4`
- Evidência: GitHub Actions Verify #70 / run `35636832562`
- Resultado: `BUILD SUCCESS`
- Testes: 6 unitários/arquiteturais + 20 integração = 26
- Falhas/erros: 0/0

O Verify executou Java 25 e `mvn clean verify` com repository Maven local isolado e resolução remota da Foundation.

## 3. Inventário atual

### Estrutura principal

```text
br.com.portalmanager.account
├── AccountServiceApplication
├── api
│   ├── AccountController
│   ├── request
│   │   ├── ApproverRequest
│   │   ├── CreateAccountRequest
│   │   └── UpdateAccountRequest
│   └── response
│       ├── AccountResponse
│       └── ApproverResponse
├── application
│   ├── AccountMessageKeys
│   ├── AccountNormalizer
│   ├── AccountService
│   ├── AccountValidator
│   ├── model
│   │   ├── AccountResult
│   │   ├── ApproverCommand
│   │   ├── ApproverResult
│   │   ├── CreateAccountCommand
│   │   └── UpdateAccountCommand
│   └── tagging
│       ├── AccountSystemTagProvider
│       └── AccountTagOwnerType
├── configuration
│   └── MessagingConfiguration
├── domain
│   ├── Account
│   ├── AccountApprover
│   ├── AccountLifecycle
│   └── AccountType
└── persistence
    └── AccountRepository
```

### Persistência

- V1 cria `accounts` e `account_approvers`;
- V2 cria `tags`;
- `AccountRepository` acessa tabelas do agregado e também referencia a entidade `Tag` em JPQL;
- JPA usa `@Version` e `ddl-auto=validate`.

### Entrada Web

- base path: `/api/v1/accounts`;
- create/list/get/update/deactivate/restore;
- Request/Response já ficam na camada HTTP;
- conversão Request -> Command e Result -> Response acontece na Web;
- Controller depende de `AccountService`, não de Repository.

### Orquestração

`AccountService` contém create, findById, findAll, update, deactivate e restore.

### Capabilities

- Authorization e ResourceVisibility;
- TagManager;
- Audit;
- Messaging;
- Logging via starter.

## 4. Itens já aderentes

- entidade JPA no Domain;
- relação `Account -> AccountApprover` é interna ao mesmo agregado;
- Controller não acessa Repository diretamente;
- Domain não depende de Web;
- Request/Response não vazam para Domain;
- optimistic locking explícito com JPA `@Version`;
- migrations versionadas;
- `platform-crud` ausente;
- Foundation não será alterada nesta atividade.

## 5. Gap analysis

| Origem atual | Destino | Ação | Justificativa | Risco/Dependências |
|---|---|---|---|---|
| `br.com.portalmanager.account` | `br.com.itau.portalmanager.workspace` | mover/renomear | namespace alvo aprovado | imports, component scan, testes |
| `api` | `input.web.workspace` | mover | Web é porta de entrada | contratos HTTP |
| `AccountController` | `WorkspaceController` | renomear/mover | domínio Workspace | endpoint, audit |
| `Create/UpdateAccountRequest` | `Create/UpdateWorkspaceRequest` | renomear | contrato Web explícito | JSON |
| `AccountResponse` | `WorkspaceResponse` | renomear | contrato Web explícito | JSON |
| `AccountService` | Use Cases por intenção | decompor | Use Case é API interna | transações, annotations |
| `Command/Result` | `Input/Output` | substituir | padrão alvo | mapeamentos |
| `AccountNormalizer` | `core.workspace.usecase.support` | manter/refatorar | suporte compartilhado de orquestração | tagging normalizer |
| `AccountValidator` | `domain.validation` + Use Cases | dividir | domínio sem repository; UC trata unicidade | mensagens |
| `Account` | `core.workspace.domain.Workspace` | renomear/mover | decisão de domínio | JPA/migration |
| `AccountApprover` | `WorkspaceApprover` | renomear/mover | agregado Workspace | FK |
| `AccountType` | `foundation.catalog.domain.WorkspaceType` | renomear/mover | tipo persistido/catalog | coluna/JSON |
| `AccountLifecycle` | `foundation.catalog.domain.LifecycleType` | renomear/mover | tipo persistido/catalog | coluna |
| `AccountRepository` | `core.workspace.repository.WorkspaceRepository` | renomear/mover | persistência própria | queries |
| JPQL contra `Tag` | `core.workspace.integration.tagging` | remover/refatorar | integração externa não pertence ao repository | contrato tabela tags |
| `AccountSystemTagProvider` | `core.workspace.domain.WorkspaceSystemTagProvider` | mover/renomear | regra de tags de sistema é do domínio | sem dependência externa |
| `AccountTagOwnerType.ACCOUNT` | owner `WORKSPACE` | substituir | domínio renomeado | dados existentes em tags |
| `configuration.MessagingConfiguration` | `foundation.messaging` | mover | elimina quinta zona | bean loading |
| `/api/v1/accounts` | `/api/v1/workspaces` | alterar | contrato alinhado ao domínio | breaking HTTP |
| JSON `accountType` | `workspaceType` | alterar | contrato alinhado ao domínio | breaking HTTP |
| audit `ACCOUNT` | `WORKSPACE` | alterar | taxonomia alinhada | eventos futuros |
| service name `account-service` | `workspace-service` | alterar | identidade ativa | observabilidade/audit/config |
| artifactId `account-service` | `workspace-service` | alterar | identidade ativa | coordenada Maven |
| `accounts/account_approvers` | `workspaces/workspace_approvers` | migration V3 | persistência alinhada | Flyway |
| `account_type/account_id` | `workspace_type/workspace_id` | migration V3 | persistência alinhada | JPA/FK |
| V1/V2 históricas | manter | manter | não quebrar Flyway | ocorrência Account consciente |
| testes atuais | packages/classes/contratos Workspace | refatorar | preservar cobertura | 26 testes |
| `GoldenArchitectureTest` | fitness functions completas | ampliar | governança executável | ArchUnit |

## 6. Violações encontradas

### Topologia

A raiz atual usa `api/application/configuration/domain/persistence`; isso não implementa as macrozonas aprovadas.

### Use Cases

O único `AccountService` é uma API interna monolítica e não demonstra o padrão de Use Case por intenção.

### Persistência cruzada

`AccountRepository.findFiltered` faz subquery JPQL em `Tag`, expondo uma dependência de persistência que não pertence ao módulo.

### Validação

`AccountValidator` consulta `AccountRepository`; regras puras e verificação de unicidade estão misturadas.

### Governança

O ArchUnit atual não protege topologia, macrozonas, Integration, Controllers por annotation ou acesso cruzado entre módulos.

## 7. Classificação Account -> Workspace

### Renomear como conceito de negócio

- packages/classes do serviço;
- Domain/Repository/Use Cases;
- Web Request/Response/Controller;
- métodos/variáveis de negócio;
- endpoint e payload;
- audit resource;
- tag owner;
- tabelas/colunas ativas;
- códigos de erro;
- service/application name;
- artifactId;
- testes ativos e README.

### Manter por ser termo técnico/externo

- `session.accountId` do contrato de Authorization da Foundation;
- eventuais termos `account` pertencentes a APIs externas que não representem o agregado Workspace.

### Manter por histórico

- V1/V2 já publicadas/aplicadas;
- ADRs/reviews G0-G4 que documentam o estado histórico;
- nome do repositório GitHub durante esta atividade.

## 8. Ondas de execução

### Onda A0 — Diagnóstico e decisão

Entregáveis:

- este plano;
- `ADR-ADEQUACAO-ARQUITETURAL-POS-G4.md`.

Critério de aceite:

- baseline e CI registrados;
- inventário real concluído;
- gaps e impacto Account -> Workspace classificados.

### Onda A1 — Foundation local e identidade base

Ações:

- criar package root `br.com.itau.portalmanager.workspace`;
- renomear bootstrap para `WorkspaceServiceApplication`;
- mover enums para `foundation.catalog.domain`;
- mover MessagingConfiguration para `foundation.messaging`;
- atualizar artifactId/app name/configuração ativa para Workspace.

Critério:

- compila;
- startup test verde;
- nenhuma quinta zona criada.

Validação:

```bash
mvn clean verify
```

### Onda A2 — Core Workspace: Domain + Repository

Ações:

- `Account` -> `Workspace`;
- `AccountApprover` -> `WorkspaceApprover`;
- `AccountRepository` -> `WorkspaceRepository`;
- remover query direta contra `Tag`;
- introduzir integration de busca reversa de tags;
- criar migration V3 de schema e owner type.

Critério:

- persistência do Workspace usa somente tabelas próprias;
- migration em banco vazio termina em V3;
- testes JPA/locking/unicidade verdes.

### Onda A3 — Use Cases e contratos internos

Ações:

- decompor AccountService em seis Use Cases;
- criar Inputs/Output;
- separar validação pura de unicidade persistida;
- manter normalização compartilhada sem abstração genérica;
- preservar ResourceVisibility nos Use Cases de leitura;
- preservar transações e comportamento de tagging.

Critério:

- não existe `AccountService`;
- Controller futuro poderá depender somente dos Use Cases;
- comportamento create/read/list/update/inactivate/restore preservado.

### Onda A4 — Input/Web e contratos Workspace

Ações:

- mover `api` para `input.web.workspace`;
- criar `WorkspaceController`;
- Request -> Input;
- Output -> Response;
- alterar `/accounts` -> `/workspaces`;
- alterar `accountType` -> `workspaceType`;
- audit resource `WORKSPACE`;
- códigos de erro `WORKSPACE-xxxx`.

Critério:

- Web não depende de Repository/Integration;
- testes HTTP cobrem somente contrato Workspace;
- nenhuma rota ativa `/accounts`.

### Onda A5 — Fitness functions

Ações:

- ampliar `GoldenArchitectureTest`;
- validar topologia fechada;
- macro-direções;
- Controller por `@RestController`;
- Domain x Integration;
- cross-module internals.

Critério:

- teste falharia para uma quinta zona;
- teste falharia para Controller -> Repository/Integration;
- teste falharia para Core -> Input/Feature.

### Onda A6 — Limpeza documental e busca residual

Ações:

- README e documentação ativa para Workspace;
- busca global por `Account/account/ACCOUNT/accounts/account_`;
- classificar cada ocorrência remanescente;
- manter somente exceções registradas no ADR.

Critério:

- nenhuma ocorrência Account ativa acidental;
- ocorrências históricas/técnicas justificadas.

### Onda A7 — Validação final

Executar:

```bash
mvn clean verify
```

Confirmar:

- 26 testes anteriores equivalentes continuam protegendo comportamento;
- novos testes arquiteturais verdes;
- Flyway V1 -> V2 -> V3 verde;
- endpoint Workspace verde;
- audit/tagging/authorization verdes;
- árvore final aderente;
- CI verde.

Atualizar ADR e este plano com resultado real.

## 9. Testes arquiteturais

Fitness functions mínimas:

1. topologia de primeiro nível fechada;
2. Foundation sem dependência de Core/Feature/Input;
3. Core sem dependência de Feature/Input;
4. Foundation/Core/Feature sem dependência de Input;
5. `@RestController` sem Repository;
6. `@RestController` sem Integration;
7. Domain sem Integration;
8. módulos Core não acessam Repository/Domain interno de outro módulo;
9. classes fora das macrozonas não podem criar atalhos.

A classe de bootstrap no package root é exceção estrutural explícita e não constitui macrozona.

## 10. Validação

A validação de cada onda será feita pelo mesmo fluxo oficial do repository:

```bash
mvn --settings .github/maven-settings.xml \
    --batch-mode \
    --no-transfer-progress \
    clean verify
```

No CI, o Maven repository permanece isolado.

## 11. Critérios de conclusão

- comportamento de negócio da G4 preservado;
- nomenclatura ativa Workspace consolidada;
- mudanças de HTTP/banco/integrations registradas;
- estrutura aderente ou exceções documentadas;
- testes existentes equivalentes verdes;
- testes arquiteturais verdes;
- build final verde;
- Foundation não alterada;
- ausência de arquitetura híbrida Account/Workspace acidental.

## 12. Resultado da execução

A preencher após implementação com:

- commits executados;
- árvore final;
- migrations aplicadas;
- lista de arquivos movidos/alterados;
- testes e CI;
- exceções conscientes;
- ocorrências Account remanescentes e classificação.
