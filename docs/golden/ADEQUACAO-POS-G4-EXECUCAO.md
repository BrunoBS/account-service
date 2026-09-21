# Execução — Adequação Arquitetural Pós-G4

## Estado

**CONCLUÍDA APÓS REVIEW CORRETIVO** na branch `refactor/architectural-alignment-post-g4`.

## Baseline

- `main`: `b49949638818f1229c78dfb93b0b6238e1a5e1fe`;
- Verify baseline: #70 / run `35636832562`;
- baseline: 26 testes;
- resultado baseline: `BUILD SUCCESS`.

## Registros prévios à alteração

- ADR inicial: commit `75d1aae2e58b9320298c820794cd74206b1a874b`;
- Plano inicial: commit `4ac97ef58f614339e727934d5b9442e5d26e2549`.

Os dois documentos foram criados antes da alteração da aplicação.

## Implementação

Commit estrutural:

```text
c1e4a56d97f94e89e38d7ddf1b0c9fab5f66b6ef
```

O resultado implementa:

- `br.com.itau.portalmanager.workspace`;
- macrozonas concretas Foundation/Core/Input;
- Core Workspace com Domain, Use Cases, Repository e Integration;
- Use Cases separados para create/find/update/inactivate/restore;
- Request/Response na Web;
- Input/Output nos Use Cases;
- Repository restrito à persistência Workspace;
- integração explícita para busca reversa de tags;
- `Account -> Workspace` em contratos ativos;
- `/api/v1/workspaces`;
- audit resource `WORKSPACE`;
- tagging owner `WORKSPACE`;
- artifact/application name `workspace-service`;
- migration V3;
- onze fitness functions/regras arquiteturais.

## Validação da implementação

GitHub Actions Verify #72:

- run `35664191912`;
- Java 25.0.4+1;
- Spring Boot 4.1.1;
- Maven repository isolado;
- Maven Enforcer verde;
- DependencyConvergence verde;
- 34 fontes principais;
- 10 fontes de teste;
- 14 testes unitários/arquiteturais;
- 21 testes de integração;
- total: 35 testes;
- 0 falhas;
- 0 erros;
- MySQL 8.0;
- Flyway validou/aplicou V1, V2 e V3;
- schema final v3;
- `BUILD SUCCESS`.

## Account remanescente

As ocorrências remanescentes foram classificadas e são conscientes:

1. V1/V2: histórico Flyway imutável;
2. V3: nomes legados usados como origem da renomeação;
3. DatabaseMigrationIT: prova de ausência das tabelas legadas após V3;
4. `accountId` do contexto de Authorization da Foundation: contrato técnico externo;
5. documentação histórica G0–G4;
6. hospedagem `BrunoBS/account-service`.

Não há classes, packages, endpoints, payloads, repositories ou entidades ativos usando Account como nome do agregado da Golden Reference.

## Resultado

A adequação arquitetural pós-G4 está tecnicamente concluída e pronta para review humano antes de qualquer avanço de fase.


## Review corretivo posterior

A primeira conclusão foi reaberta após review arquitetural completo.

### Gaps encontrados

- `MessagingConfiguration` havia sido classificada em `foundation.messaging` sem
  decisão arquitetural suficiente;
- a substituição do ArchUnit antigo havia perdido proteção de independência de Domain;
- a regra cross-module não distinguia adequadamente módulos aninhados;
- Input não possuía proteção contra bypass dos Use Cases;
- a V3 estava comprovada apenas em banco vazio.

### Correções

- ADR-006 definiu o package root exato como composition root;
- `WorkspaceMessagingConfiguration` passou a residir no root;
- Domain voltou a ser protegido contra UseCase/Repository/Integration/Input/Spring Web;
- Input passou a não acessar internals de Core/Feature;
- cross-module passou a aceitar somente contratos `UseCase/Input/Output`;
- `DatabaseUpgradeMigrationIT` passou a provar V2 populada -> V3.

O Verify #81 / run `35668223125` falhou durante o review porque a primeira versão da
regra de Input também bloqueava `foundation.catalog.domain`. A regra foi corrigida para
proteger internals de Core/Feature sem proibir a direção macro permitida.

### Validação final do review

Verify #82 / run `35668527818`:

- 34 fontes principais;
- 10 fontes de teste;
- 11 fitness functions;
- 3 testes unitários;
- 21 testes de integração;
- 35 testes totais;
- 0 falhas;
- 0 erros;
- `DatabaseUpgradeMigrationIT` executou V1/V2, inseriu dados legados e aplicou V3;
- **BUILD SUCCESS**.

## Pendências conscientes

- decisão do `groupId` Maven oficial antes de `GOLDEN-REFERENCE-V1`;
- SQL físico de busca reversa em `tags` permanece dívida explícita da integração;
- nome físico do repositório permanece histórico.

A PR #5 continua como draft/review; merge e checkpoint não fazem parte desta execução.
