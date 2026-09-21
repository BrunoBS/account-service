# ADR — Adequação Arquitetural Pós-G4

- **Status:** Aceito
- **Data:** 2026-09-21
- **Baseline funcional:** G4 concluída
- **Baseline técnico analisado:** `BrunoBS/account-service` / `main` / `b49949638818f1229c78dfb93b0b6238e1a5e1fe`
- **Branch de execução:** `refactor/architectural-alignment-post-g4`

## Contexto

A G4 foi concluída no head funcional `a4c238eea24022d984aed49ba8a18eeb9d0a4853`. Depois disso, a aplicação recebeu somente a migração de namespace da Foundation, encerrada no commit `b49949638818f1229c78dfb93b0b6238e1a5e1fe`, sem iniciar G5.

O baseline atual compila 24 fontes principais e 9 fontes de teste e está organizado sob:

```text
br.com.portalmanager.account
├── api
├── application
├── configuration
├── domain
└── persistence
```

Essa organização preserva separações úteis, mas não representa o padrão arquitetural aprovado para a nova Golden Reference, que adota as macrozonas `foundation`, `core`, `feature` e `input`.

A validação do baseline é o GitHub Actions Verify #70, run `35636832562`, que executou Java 25 e:

```bash
mvn --settings .github/maven-settings.xml \
    --batch-mode \
    --no-transfer-progress \
    -Dmaven.repo.local="${RUNNER_TEMP}/account-service-m2" \
    clean verify
```

Resultado: 6 testes unitários/arquiteturais + 20 testes de integração, 0 falhas, 0 erros e `BUILD SUCCESS`.

## Problemas encontrados

1. O package root atual (`br.com.portalmanager.account`) não corresponde ao namespace arquitetural alvo `br.com.itau.portalmanager.workspace`.
2. Não existem as macrozonas `foundation/core/feature/input`.
3. `AccountService` concentra seis intenções de negócio no mesmo application service.
4. `Command/Result` são contratos de aplicação, mas o padrão alvo define `Input/Output` por Use Case.
5. `AccountRepository.findFiltered` consulta diretamente a entidade `Tag` da capability de tagging, misturando persistência própria com dado administrado por outra capability.
6. `AccountValidator` mistura validação de regras de entrada com consultas de unicidade ao repository.
7. `configuration` existe como zona paralela ao modelo alvo.
8. Os testes ArchUnit atuais protegem apenas três regras e não fecham a topologia arquitetural.
9. A nomenclatura `Account` está presente em packages, classes, contratos HTTP, payloads, persistência, tagging, auditoria, configuração, testes e documentação ativa.

## Decisão arquitetural

A aplicação será adequada para:

```text
br.com.itau.portalmanager.workspace
├── foundation
├── core
├── feature
└── input
```

Não serão criados pacotes vazios. Enquanto não existir uma Feature real, `feature` poderá não possuir classes concretas.

A classe de bootstrap `WorkspaceServiceApplication` poderá permanecer diretamente no package root para preservar o component scan, sem constituir uma quinta macrozona.

### Foundation

No baseline real desta adequação, Foundation local conterá somente responsabilidades concretas:

```text
foundation
├── catalog
│   └── domain
│       ├── WorkspaceType
│       └── LifecycleType
└── messaging
    └── MessagingConfiguration
```

`WorkspaceType` e `LifecycleType` permanecem enums fechados. Esta reorganização não implica adoção de `platform-catalog` nem mudança funcional dos valores existentes.

### Core Workspace

```text
core.workspace
├── domain
│   ├── Workspace
│   ├── WorkspaceApprover
│   ├── WorkspaceSystemTagProvider
│   └── validation
├── usecase
│   ├── create
│   ├── update
│   ├── findbyid
│   ├── findall
│   ├── inactivate
│   ├── restore
│   └── support
├── repository
│   └── WorkspaceRepository
└── integration
    └── tagging
```

As intenções existentes serão expostas por Use Cases separados:

- `CreateWorkspaceUseCase`;
- `UpdateWorkspaceUseCase`;
- `FindWorkspaceByIdUseCase`;
- `FindAllWorkspacesUseCase`;
- `InactivateWorkspaceUseCase`;
- `RestoreWorkspaceUseCase`.

A entidade JPA continuará no `domain`, conforme o modelo pragmático aprovado.

### Input/Web

```text
input.web.workspace
├── request
├── response
└── WorkspaceController
```

Request/Response pertencem à Web. Input/Output pertencem ao Use Case.

O fluxo será:

```text
Request -> Input -> UseCase -> Output -> Response
```

O Controller não acessará Repository ou Integration.

## Repository x Integration

`WorkspaceRepository` representará somente a persistência das tabelas pertencentes ao módulo Workspace.

A busca por tag deixará de fazer JPQL contra a entidade `Tag` da platform library.

Como a API pública atual de `TagManager` não fornece busca reversa de owners por nome de tag, será criada uma integração explícita dentro de `core.workspace.integration.tagging` para consultar o contrato persistido da capability de tagging. O Use Case de listagem orquestrará:

```text
FindAllWorkspacesUseCase
├── WorkspaceRepository
├── TagManager
└── WorkspaceTagSearchIntegration
```

Isso evita acesso direto do repository de Workspace ao Domain/Repository interno de outra capability e mantém `platform-libraries` fora do escopo de alteração.

## Mudança conceitual: Account -> Workspace

A renomeação é uma decisão de domínio, não uma substituição textual.

### 1. Conceito de negócio que será renomeado

Serão convertidos para `Workspace`:

- package root da aplicação;
- classe de bootstrap;
- entidade e agregado;
- approver pertencente ao agregado;
- Repository;
- Use Cases;
- Input/Output;
- Controller;
- Request/Response;
- nomes de métodos e variáveis de negócio;
- `AccountType` -> `WorkspaceType`;
- endpoint `/api/v1/accounts` -> `/api/v1/workspaces`;
- campo HTTP `accountType` -> `workspaceType`;
- path variable `accountId` -> `workspaceId`;
- audit resource `ACCOUNT` -> `WORKSPACE`;
- tag owner type `ACCOUNT` -> `WORKSPACE`;
- service/application name `account-service` -> `workspace-service`;
- artifactId Maven `account-service` -> `workspace-service`;
- códigos de erro `ACCOUNT-xxxx` -> `WORKSPACE-xxxx`;
- tabelas/colunas físicas do agregado;
- testes ativos;
- README e documentação produzida a partir desta decisão.

### 2. Ocorrências que legitimamente permanecem Account

Não serão renomeadas quando `Account` pertencer a outro contrato técnico ou histórico.

Exemplo confirmado: `AuthorizationMock.session.accountId(...)` representa o campo `accountId` do contrato da Foundation de autorização, não o agregado de negócio da Golden. Ele permanece inalterado.

Documentação histórica G0-G4 pode continuar descrevendo Account para preservar evidência e rastreabilidade do estado em que foi produzida.

O nome do repositório GitHub `BrunoBS/account-service` permanece como identidade de hospedagem nesta atividade. Renomear o repositório é uma mudança administrativa externa e não é necessária para adequar código, contratos da aplicação e arquitetura.

### 3. Contratos persistidos/externos que exigem mudança explícita

A renomeação afeta contratos observáveis:

- HTTP path e JSON;
- Maven artifactId;
- `spring.application.name`;
- configuração de audit service-name;
- variáveis de ambiente de datasource;
- códigos de erro;
- taxonomia de audit resource;
- `tags.owner_type`;
- nomes de tabelas, colunas, índices e constraints.

Essas alterações são deliberadas nesta Golden Reference. Não será mantido alias `/accounts`, pois isso perpetuaria uma API híbrida Account/Workspace sem requisito de compatibilidade aprovado.

Eventos de auditoria históricos não serão reescritos; novos eventos usarão `WORKSPACE`.

## Estratégia de migration

As migrations V1 e V2 já fazem parte do histórico Flyway e **não serão editadas nem renomeadas**, evitando alteração de checksum/descrição de migrations aplicadas.

Será adicionada uma nova migration V3 para:

- `accounts` -> `workspaces`;
- `account_approvers` -> `workspace_approvers`;
- `account_type` -> `workspace_type`;
- `account_id` -> `workspace_id`;
- atualizar nomes de índices/constraints relevantes;
- converter `tags.owner_type = 'ACCOUNT'` para `'WORKSPACE'`.

A permanência da palavra Account em V1/V2 é uma exceção histórica consciente, não arquitetura híbrida ativa.

## Comunicação entre módulos

Permanece a regra:

> colaboração entre módulos internos ocorre por Use Case público; Repository/Domain interno de outro módulo não é API de colaboração.

Nesta fase existe somente `core.workspace`, mas os testes arquiteturais serão escritos de forma a proteger essa regra para módulos futuros quando tecnicamente verificável.

## Governança automatizada

Os testes arquiteturais serão ampliados para proteger, no mínimo:

1. somente `foundation`, `core`, `feature` e `input` como zonas de primeiro nível abaixo do root;
2. Foundation não depende de Core/Feature/Input;
3. Core não depende de Feature/Input;
4. módulos internos não dependem de Input;
5. classes `@RestController` não dependem de Repository;
6. classes `@RestController` não dependem de Integration;
7. Domain não depende de Integration;
8. acesso cruzado a Repository/Domain interno de outro módulo é rejeitado quando identificável;
9. uma zona paralela não pode ser criada para contornar as regras.

## Consequências

### Positivas

- arquitetura passa a refletir explicitamente o padrão da Golden Reference;
- Use Cases tornam-se API interna e unidade de orquestração;
- protocolo HTTP fica desacoplado do Core;
- Repository deixa de conhecer persistência de outra capability;
- a mudança Account -> Workspace é completa e auditável;
- regras arquiteturais objetivas passam a falhar o build quando violadas;
- o código fica utilizável como referência para scaffolding futuro.

### Trade-offs

- aumento intencional do número de classes por separar intenções de negócio;
- quebra nominal deliberada do contrato HTTP/JSON e de identificadores de serviço;
- necessidade de migration de banco;
- integração de busca por tags passa a explicitar dependência do contrato persistido da capability até existir API pública reversa na Foundation;
- migrations e documentação histórica ainda contêm `Account` por rastreabilidade.

## Alternativas consideradas

### Apenas mover packages

Rejeitada. Manteria `AccountService`, contratos genéricos e a mistura de tagging no Repository.

### Substituição textual Account -> Workspace

Rejeitada. Quebraria contratos técnicos legítimos, migrations históricas e poderia corromper semântica.

### Manter /accounts como alias de compatibilidade

Rejeitada neste baseline por inexistir requisito de compatibilidade externa aprovado e porque manteria nomenclatura híbrida na Golden Reference.

### Alterar platform-libraries para suportar busca reversa de tags

Rejeitada nesta atividade. A Foundation está fora do escopo e não deve ser reaberta sem evidência/decisão própria.

### Manter JPQL contra Tag dentro de WorkspaceRepository

Rejeitada por misturar persistência do módulo com detalhes internos de outra capability.

## Exceções conscientes

- V1/V2 e documentos históricos G0-G4 permanecem com nomenclatura Account.
- `AuthorizationMock.session.accountId` e equivalentes da Foundation permanecem Account quando o termo pertence ao contrato de identidade/autorização externo ao domínio Workspace.
- o nome do repositório GitHub permanece `account-service` nesta atividade.

## Resultado aplicado

A execução confirmou a arquitetura proposta sem necessidade de reabrir a Foundation.

Estrutura de produção resultante:

```text
br.com.itau.portalmanager.workspace
├── WorkspaceServiceApplication
├── foundation
│   ├── catalog.domain
│   └── messaging
├── core
│   └── workspace
│       ├── domain
│       ├── repository
│       ├── integration.tagging
│       └── usecase
└── input
    └── web.workspace
```

Não existe Feature concreta no baseline pós-G4; por isso nenhum pacote vazio foi criado.

O antigo `AccountService` foi substituído por seis Use Cases explícitos. O `WorkspaceRepository` passou a acessar somente a persistência do próprio módulo. A busca reversa de tags foi isolada em `WorkspaceTagSearchIntegration`, preservando `platform-libraries` sem alteração.

A migration `V3__rename_account_domain_to_workspace.sql` foi aplicada sobre V1/V2 em MySQL 8, renomeando schema ativo e convertendo `tags.owner_type` para `WORKSPACE`.

Os contratos ativos passaram a utilizar `Workspace` em Java, HTTP, payload, audit, tagging, mensagens e identidade da aplicação. Permaneceram apenas as exceções históricas/técnicas registradas nesta decisão.

## Evidências

- baseline atual: `b49949638818f1229c78dfb93b0b6238e1a5e1fe`;
- Verify #70 / run `35636832562`: `BUILD SUCCESS`;
- Java 25.0.4+1;
- Maven Enforcer: Java, Maven, property, duplicate dependency versions e dependency convergence aprovados;
- baseline: 6 testes unitários/arquiteturais + 20 testes de integração, 0 falhas / 0 erros;
- implementação: Verify #72 / run `35664191912`: `BUILD SUCCESS`;
- 34 fontes principais e 9 fontes de teste compiladas com Java 25;
- 11 testes unitários/arquiteturais, incluindo 8 fitness functions;
- 20 testes de integração equivalentes ao baseline funcional;
- 31 testes totais, 0 falhas / 0 erros;
- Flyway aplicou V1, V2 e V3 e encerrou em schema version `v3`;
- plano de execução: `docs/golden/PLANO-ADEQUACAO-ARQUITETURAL-POS-G4.md`.

