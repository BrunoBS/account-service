# ADR — Adequação Arquitetural Pós-G4

- **Status:** Aceito após review corretivo
- **Data:** 2026-09-21
- **Baseline funcional:** G4 concluída
- **Baseline técnico analisado:** `BrunoBS/account-service` / `main` / `b49949638818f1229c78dfb93b0b6238e1a5e1fe`
- **Branch de execução:** `refactor/architectural-alignment-post-g4`

## Contexto

A G4 foi concluída no head funcional `a4c238eea24022d984aed49ba8a18eeb9d0a4853`. Depois disso, a aplicação recebeu
somente a migração de namespace da Foundation, encerrada no commit `b49949638818f1229c78dfb93b0b6238e1a5e1fe`, sem
iniciar G5.

O baseline atual compila 24 fontes principais e 9 fontes de teste e está organizado sob:

```text
br.com.portalmanager.account
├── api
├── application
├── configuration
├── domain
└── persistence
```

Essa organização preserva separações úteis, mas não representa o padrão arquitetural aprovado para a nova Golden
Reference, que adota as macrozonas `foundation`, `core`, `feature` e `entrypoint`.

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

1. O package root atual (`br.com.portalmanager.account`) não corresponde ao namespace arquitetural alvo
   `br.com.itau.portalmanager.workspace`.
2. Não existem as macrozonas `foundation/core/feature/entrypoint`.
3. `AccountService` concentra seis intenções de negócio no mesmo application service.
4. `Command/Result` são contratos de aplicação, mas o padrão alvo define `Input/Output` por Use Case.
5. `AccountRepository.findFiltered` consulta diretamente a entidade `Tag` da capability de tagging, misturando
   persistência própria com dado administrado por outra capability.
6. `AccountValidator` mistura validação de regras de entrada com consultas de unicidade ao repository.
7. `configuration` existe como zona paralela ao modelo alvo.
8. Os testes ArchUnit atuais protegem apenas três regras e não fecham a topologia arquitetural.
9. A nomenclatura `Account` está presente em packages, classes, contratos HTTP, payloads, persistência, tagging,
   auditoria, configuração, testes e documentação ativa.

## Decisão arquitetural

A aplicação será adequada para:

```text
br.com.itau.portalmanager.workspace
├── foundation
├── core
├── feature
└── entrypoint
```

Não serão criados pacotes vazios. Enquanto não existir uma Feature real, `feature` poderá não possuir classes concretas.

O package root exato também funciona como **composition root**. Conforme o ADR-006, ele pode conter a classe
`WorkspaceServiceApplication` e classes `@Configuration` de wiring técnico do consumidor, sem constituir uma quinta
macrozona. As macrozonas não dependem dessas classes de composição.

### Foundation

No baseline real desta adequação, Foundation local conterá somente responsabilidades concretas:

```text
foundation
└── catalog
    └── domain
        ├── WorkspaceType
        └── LifecycleType
```

O wiring de messaging não pertence à application foundation macrozone. `WorkspaceMessagingConfiguration` reside no
composition root, conforme `ADR-006-COMPOSITION-ROOT-E-SUPERFICIE-PUBLICA.md`.

`WorkspaceType` e `LifecycleType` permanecem enums fechados. Esta reorganização não implica adoção de `platform-catalog`
nem mudança funcional dos valores existentes.

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

### Entrypoint/Web

```text
entrypoint.web.workspace
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

Como a API pública atual de `TagManager` não fornece busca reversa de owners por nome de tag, será criada uma integração
explícita dentro de `core.workspace.integration.tagging` para consultar o contrato persistido da capability de tagging.
O Use Case de listagem orquestrará:

```text
FindAllWorkspacesUseCase
├── WorkspaceRepository
├── TagManager
└── WorkspaceTagSearchIntegration
```

Isso evita acesso direto do repository de Workspace ao Domain/Repository interno de outra capability e mantém
`platform-libraries` fora do escopo de alteração.

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

Exemplo confirmado: `AuthorizationMock.session.accountId(...)` representa o campo `accountId` do contrato da Foundation
de autorização, não o agregado de negócio da Golden. Ele permanece inalterado.

Documentação histórica G0-G4 pode continuar descrevendo Account para preservar evidência e rastreabilidade do estado em
que foi produzida.

O nome do repositório GitHub `BrunoBS/account-service` permanece como identidade de hospedagem nesta atividade. Renomear
o repositório é uma mudança administrativa externa e não é necessária para adequar código, contratos da aplicação e
arquitetura.

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

Essas alterações são deliberadas nesta Golden Reference. Não será mantido alias `/accounts`, pois isso perpetuaria uma
API híbrida Account/Workspace sem requisito de compatibilidade aprovado.

Eventos de auditoria históricos não serão reescritos; novos eventos usarão `WORKSPACE`.

## Estratégia de migration

As migrations V1 e V2 já fazem parte do histórico Flyway e **não serão editadas nem renomeadas**, evitando alteração de
checksum/descrição de migrations aplicadas.

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

> colaboração entre módulos internos ocorre por Use Case público; Repository/Domain interno de outro módulo não é API de
> colaboração.

Nesta fase existe somente `core.workspace`, mas os testes arquiteturais serão escritos de forma a proteger essa regra
para módulos futuros quando tecnicamente verificável.

## Governança automatizada

Os testes arquiteturais serão ampliados para proteger, no mínimo:

1. somente `foundation`, `core`, `feature` e `entrypoint` como zonas de primeiro nível abaixo do root;
2. Foundation não depende de Core/Feature/Entrypoint;
3. Core não depende de Feature/Entrypoint;
4. módulos internos não dependem de Entrypoint;
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
- integração de busca por tags passa a explicitar dependência do contrato persistido da capability até existir API
  pública reversa na Foundation;
- migrations e documentação histórica ainda contêm `Account` por rastreabilidade.

## Alternativas consideradas

### Apenas mover packages

Rejeitada. Manteria `AccountService`, contratos genéricos e a mistura de tagging no Repository.

### Substituição textual Account -> Workspace

Rejeitada. Quebraria contratos técnicos legítimos, migrations históricas e poderia corromper semântica.

### Manter /accounts como alias de compatibilidade

Rejeitada neste baseline por inexistir requisito de compatibilidade externa aprovado e porque manteria nomenclatura
híbrida na Golden Reference.

### Alterar platform-libraries para suportar busca reversa de tags

Rejeitada nesta atividade. A Foundation está fora do escopo e não deve ser reaberta sem evidência/decisão própria.

### Manter JPQL contra Tag dentro de WorkspaceRepository

Rejeitada por misturar persistência do módulo com detalhes internos de outra capability.

## Exceções conscientes

- V1/V2 e documentos históricos G0-G4 permanecem com nomenclatura Account.
- `AuthorizationMock.session.accountId` e equivalentes da Foundation permanecem Account quando o termo pertence ao
  contrato de identidade/autorização externo ao domínio Workspace.
- o nome do repositório GitHub permanece `account-service` nesta atividade.

## Resultado aplicado

A execução confirmou a arquitetura proposta sem necessidade de reabrir a Foundation.

Estrutura de produção resultante:

```text
br.com.itau.portalmanager.workspace
├── WorkspaceServiceApplication
├── WorkspaceMessagingConfiguration
├── foundation
│   └── catalog.domain
├── core
│   └── workspace
│       ├── domain
│       ├── repository
│       ├── integration.tagging
│       └── usecase
└── entrypoint
    └── web.workspace
```

Não existe Feature concreta no baseline pós-G4; por isso nenhum pacote vazio foi criado.

O antigo `AccountService` foi substituído por seis Use Cases explícitos. O `WorkspaceRepository` passou a acessar
somente a persistência do próprio módulo. A busca reversa de tags foi isolada em `WorkspaceTagSearchIntegration`,
preservando `platform-libraries` sem alteração.

A migration `V3__rename_account_domain_to_workspace.sql` foi aplicada sobre V1/V2 em MySQL 8, renomeando schema ativo e
convertendo `tags.owner_type` para `WORKSPACE`.

Os contratos ativos passaram a utilizar `Workspace` em Java, HTTP, payload, audit, tagging, mensagens e identidade da
aplicação. Permaneceram apenas as exceções históricas/técnicas registradas nesta decisão.

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

## Review corretivo pós-implementação

Após a primeira conclusão técnica, foi executado um review completo da fase. O review
identificou gaps que não alteravam o comportamento funcional, mas impediam considerar o
estado anterior como referência arquitetural definitiva.

### Findings e correções

1. **Wiring técnico classificado por exclusão em Foundation**  
   `MessagingConfiguration` havia sido movida para `foundation.messaging` apenas para
   evitar uma quinta zona. A decisão foi corrigida pelo ADR-006: wiring Spring específico
   do consumidor reside no composition root. A classe ativa é
   `WorkspaceMessagingConfiguration`.

2. **Fitness function de Domain enfraquecida**  
   A regra antiga que impedia Domain de depender de orquestração/persistência/Web havia
   sido perdida. O ArchUnit agora impede `domain -> usecase/repository/integration/entrypoint`
   e `domain -> org.springframework.web`.

3. **Boundary Entrypoint insuficiente**  
   Entrypoint agora é impedido de acessar diretamente Domain/Repository/Integration de módulos
   Core/Feature. Foundation permanece consumível conforme a direção macro permitida.

4. **Cross-module incompleto**  
   A identificação de módulos passou a considerar o caminho até
   `domain/usecase/repository/integration`, distinguindo módulos aninhados como
   `core.configuration.workspace` e `core.configuration.application`.
   Colaboração cross-module é permitida somente por contratos de Use Case
   (`*UseCase`, `*Input`, `*Output`).

5. **Migration validada somente em banco vazio**  
   Foi criado `DatabaseUpgradeMigrationIT`, que sobe o schema até V2, insere Account,
   Approver e Tag reais, executa V3 e comprova preservação dos dados, conversão
   `ACCOUNT -> WORKSPACE` e cascade da FK renomeada.

### Evidência final do review

- commit da decisão: `95982e2d1ade0822d5acc4eefd4d4253fc7e80bc`;
- commit estrutural/testes: `a33b47ff349b4aa2cafa21a78491cc8f45fe2d7d`;
- ajuste da fitness function de Entrypoint: `fcb364c6c16b4e098ae32c85422778d7b612e0af`;
- Verify #81 / run `35668223125`: falhou corretamente ao revelar uma regra de Entrypoint
  excessivamente ampla, que confundia `foundation.catalog.domain` com internals de
  módulo;
- Verify #82 / run `35668527818`: **BUILD SUCCESS**;
- 34 fontes principais e 10 fontes de teste;
- 11 fitness functions + 3 testes unitários = 14 testes Surefire;
- 21 testes de integração Failsafe;
- 35 testes totais, 0 falhas, 0 erros;
- `DatabaseUpgradeMigrationIT`: V2 com dados -> V3 validado em MySQL 8.

### Pendências não resolvidas por falta de decisão vigente

- o `groupId` Maven da aplicação permanece `br.com.portalmanager`; o projeto não
  contém decisão suficiente para alterá-lo automaticamente para acompanhar o namespace
  Java. O tema deve ser decidido antes do checkpoint `GOLDEN-REFERENCE-V1`;
- a busca reversa de tags continua conhecendo o contrato físico da tabela `tags`; é
  dívida aceita e coberta por integração, não autorização para reabrir
  `platform-libraries`;
- o nome físico do repositório continua histórico, conforme decisão anterior.
