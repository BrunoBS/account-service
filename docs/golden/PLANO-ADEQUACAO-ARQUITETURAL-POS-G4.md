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
| `configuration.MessagingConfiguration` | composition root / `WorkspaceMessagingConfiguration` | mover/renomear | wiring técnico do consumidor; ADR-006 | bean loading |
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
- mover/renomear `MessagingConfiguration` para `WorkspaceMessagingConfiguration` no composition root;
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

**Status:** CONCLUÍDO

### Implementação

Commit estrutural:

```text
c1e4a56d97f94e89e38d7ddf1b0c9fab5f66b6ef
```

Foram executadas as ondas A1–A6:

- namespace ativo alterado para `br.com.itau.portalmanager.workspace`;
- identidade Maven/aplicação alterada para `workspace-service`;
- macrozonas concretas organizadas em `foundation`, `core` e `input`;
- `feature` não foi criada vazia;
- agregado `Account` renomeado para `Workspace`;
- `AccountService` removido e substituído por Use Cases por intenção;
- Request/Response movidos para `input.web.workspace`;
- Input/Output mantidos no espaço de Use Cases;
- persistência do Workspace isolada no `WorkspaceRepository`;
- busca reversa de tags retirada do Repository e explicitada em `integration.tagging`;
- audit resource alterado para `WORKSPACE`;
- owner type de tagging alterado para `WORKSPACE`;
- endpoint alterado para `/api/v1/workspaces`;
- JSON `accountType` alterado para `workspaceType`;
- migration V3 adicionada sem reescrever V1/V2;
- fitness functions ampliadas.

### Árvore final relevante

```text
br.com.itau.portalmanager.workspace
├── WorkspaceServiceApplication
├── WorkspaceMessagingConfiguration
├── foundation
│   └── catalog
│       └── domain
│           ├── LifecycleType
│           └── WorkspaceType
├── core
│   └── workspace
│       ├── domain
│       │   ├── validation
│       │   ├── Workspace
│       │   ├── WorkspaceApprover
│       │   ├── WorkspaceMessageKeys
│       │   └── WorkspaceSystemTags
│       ├── integration
│       │   └── tagging
│       │       └── WorkspaceTagSearchIntegration
│       ├── repository
│       │   └── WorkspaceRepository
│       └── usecase
│           ├── create
│           ├── findall
│           ├── findbyid
│           ├── inactivate
│           ├── model
│           ├── restore
│           ├── support
│           └── update
└── input
    └── web
        └── workspace
            ├── request
            ├── response
            └── WorkspaceController
```

### Persistência

Flyway final:

```text
V1 create account schema        (histórica, preservada)
V2 create tags                  (histórica, preservada)
V3 rename account domain to workspace
```

A V3:

- renomeia `accounts` para `workspaces`;
- renomeia `account_approvers` para `workspace_approvers`;
- renomeia `account_type` para `workspace_type`;
- renomeia `account_id` para `workspace_id`;
- renomeia índices/constraints aplicáveis;
- recria a FK com nomenclatura Workspace;
- converte `tags.owner_type = ACCOUNT` para `WORKSPACE`.

### Governança automatizada

`GoldenArchitectureTest` protege onze fitness functions/regras executáveis:

1. topologia permitida;
2. composition root restrito a bootstrap/configuração;
3. Foundation sem dependências para zonas superiores;
4. Core sem Feature/Input;
5. zonas internas sem Input;
6. Domain sem UseCase/Repository/Integration/Input/Spring Web;
7. Input sem bypass para Domain/Repository/Integration de Core/Feature;
8. RestController sem Repository;
9. RestController sem Integration;
10. colaboração cross-module somente por contratos de Use Case;
11. identificação correta de módulos de negócio aninhados.

### Validação

GitHub Actions Verify #72, run `35664191912`:

- 34 fontes principais;
- 9 fontes de teste;
- 8 testes arquiteturais;
- 3 testes unitários;
- 20 testes de integração;
- 31 testes no total;
- 0 falhas;
- 0 erros;
- Flyway V1 → V2 → V3 em MySQL 8.0;
- `BUILD SUCCESS`.

### Exceções conscientes / Account remanescente

Permanecem somente:

- V1/V2 históricas, para preservar o histórico Flyway;
- referências a nomes antigos dentro da V3, necessárias para executar a renomeação;
- `DatabaseMigrationIT`, que cita `accounts` e `account_approvers` para provar que não existem após V3;
- `accountId` do UserContext/Authorization da Foundation, pois é contrato técnico externo ao agregado Workspace;
- documentação histórica G0–G4;
- nome físico do repositório `BrunoBS/account-service`.

Não permanece arquitetura ativa híbrida Account/Workspace.

### Divergências mantidas

- `feature` não possui classes porque não existe Feature real no baseline pós-G4; criar pacote vazio violaria a diretriz do padrão.
- o repositório GitHub não foi renomeado, pois isso é mudança administrativa externa sem necessidade para a adequação arquitetural.
- a integração de busca de tags usa leitura SQL explícita da tabela administrada pela capability porque `TagManager` não expõe busca reversa por tag; a Foundation não foi modificada nesta atividade.


## 13. Review corretivo da fase

O review posterior à primeira execução reabriu a conclusão técnica e corrigiu quatro gaps
antes do checkpoint:

- wiring Spring do consumidor saiu de `foundation.messaging` e passou ao composition
  root, por decisão explícita no ADR-006;
- fitness functions recuperaram a independência de Domain e fortaleceram Input e
  colaboração cross-module;
- módulos aninhados passaram a ser identificados corretamente;
- migration V3 ganhou teste de upgrade com dados reais, além do teste de banco vazio.

### Evidência

Verify #82 / run `35668527818`:

- Java 25;
- 34 fontes principais;
- 10 fontes de teste;
- 11 testes arquiteturais;
- 3 testes unitários;
- 21 testes de integração;
- 35 testes totais;
- 0 falhas / 0 erros;
- MySQL 8;
- Flyway V2 com dados -> V3 validado;
- **BUILD SUCCESS**.

### Pendências para checkpoint

- decidir o `groupId` Maven oficial da aplicação/serviços Golden;
- manter registrada a dívida do SQL físico de busca reversa de tags;
- metadados administrativos do repositório podem ser ajustados sem alterar a arquitetura.

**Status final desta atividade:** implementação e review corretivo concluídos; PR permanece
em review antes do checkpoint.
